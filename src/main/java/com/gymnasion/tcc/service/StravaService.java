package com.gymnasion.tcc.service;

import com.gymnasion.tcc.domain.Aluno;
import com.gymnasion.tcc.domain.IntegracaoStrava;
import com.gymnasion.tcc.domain.Usuario;
import com.gymnasion.tcc.domain.enums.Role;
import com.gymnasion.tcc.domain.enums.StatusConvite;
import com.gymnasion.tcc.dto.StravaAtividadeDTO;
import com.gymnasion.tcc.dto.StravaAuthUrlResponseDTO;
import com.gymnasion.tcc.dto.StravaConectarRequestDTO;
import com.gymnasion.tcc.dto.StravaStatusResponseDTO;
import com.gymnasion.tcc.exceptions.BusinessException;
import com.gymnasion.tcc.exceptions.DuplicateResourceException;
import com.gymnasion.tcc.exceptions.NotFoundException;
import com.gymnasion.tcc.exceptions.StravaRateLimitException;
import com.gymnasion.tcc.infra.config.StravaProperties;
import com.gymnasion.tcc.repository.AlunoRepository;
import com.gymnasion.tcc.repository.IntegracaoStravaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Integração com o Strava API v3 (US-14).
 *
 * A leitura/atualização de banco ocorre em TransactionTemplate curto fora do I/O HTTP para evitar
 * retenção de conexões JDBC, respeitando spring.jpa.open-in-view=false.
 */
@Service
@Slf4j
public class StravaService {

    private static final String STRAVA_OAUTH_AUTHORIZE_URL = "https://www.strava.com/oauth/authorize";
    private static final String STRAVA_OAUTH_TOKEN_URL = "https://www.strava.com/oauth/token";
    private static final String STRAVA_OAUTH_DEAUTHORIZE_URL = "https://www.strava.com/oauth/deauthorize";
    private static final String STRAVA_API_ACTIVITIES_URL = "https://www.strava.com/api/v3/athlete/activities";
    private static final String STRAVA_API_ACTIVITY_DETAIL_URL = "https://www.strava.com/api/v3/activities";
    private static final String SCOPES_SOLICITADOS = "read,activity:read_all";
    private static final Set<String> SCOPES_ATIVIDADES = Set.of("activity:read", "activity:read_all");
    private static final Set<String> ESPORTES_CORRIDA_CAMINHADA = Set.of("run", "trailrun", "virtualrun", "walk", "hike");
    private static final Duration VALIDADE_STATE = Duration.ofMinutes(10);
    private static final Duration ANTECEDENCIA_RENOVACAO = Duration.ofMinutes(5);
    private static final int LIMITE_PADRAO = 10;
    private static final int LIMITE_MAXIMO = 30;
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final ZoneId ZONA_SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private static final String MSG_ERRO_CONEXAO = "Erro ao conectar conta Strava. Entre em contato com o administrador.";
    private static final String MSG_SEM_PERMISSAO_ATIVIDADES = "O acesso às atividades do Strava não foi autorizado. Reconecte sua conta e permita o acesso às atividades.";
    private static final String MSG_TOKEN_EXPIRADO = "O token de acesso do Strava expirou. Por favor, reconecte sua conta.";
    private static final String MSG_FALHA_COMUNICACAO = "Falha ao carregar atividades do Strava. Tente novamente mais tarde.";
    private static final String MSG_RATE_LIMIT = "Limite temporário de requisições do Strava atingido. Preencha os dados manualmente se necessário.";
    private static final String MSG_LIMITE_POR_ALUNO = "Muitas consultas ao Strava em pouco tempo. Aguarde alguns minutos ou preencha os dados manualmente.";
    private static final String MSG_ID_ATIVIDADE_INVALIDO = "Identificador de atividade do Strava inválido.";
    private static final String MSG_ATIVIDADE_NAO_ENCONTRADA = "Atividade do Strava não encontrada.";

    // M3: protege a cota do app no Strava (compartilhada entre todos os usuários) com cache curto e limite por aluno.
    // Estado em memória, por instância: suficiente enquanto a API roda em um único container.
    private static final java.util.regex.Pattern ID_ATIVIDADE_STRAVA = java.util.regex.Pattern.compile("^\\d{1,20}$");
    private static final Duration TTL_CACHE_DETALHE = Duration.ofMinutes(10);
    private static final Duration TTL_CACHE_LISTAGEM = Duration.ofSeconds(60);
    private static final Duration JANELA_LIMITE_POR_ALUNO = Duration.ofMinutes(15);
    private static final int LIMITE_CHAMADAS_POR_ALUNO = 20;
    private static final int TAMANHO_PARA_LIMPEZA_CACHE = 500;

    private final IntegracaoStravaRepository integracaoStravaRepository;
    private final AlunoRepository alunoRepository;
    private final NotificacaoService notificacaoService;
    private final StravaProperties stravaProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final TransactionTemplate txTemplate;
    private final TransactionTemplate readOnlyTxTemplate;
    private final TransactionTemplate requiresNewTxTemplate;
    private final ConcurrentHashMap<UUID, Object> refreshLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, EntradaCache<StravaAtividadeDTO>> cacheDetalhe = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, EntradaCache<List<StravaAtividadeDTO>>> cacheListagem = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Deque<Instant>> chamadasPorAluno = new ConcurrentHashMap<>();

    @Autowired
    public StravaService(
            IntegracaoStravaRepository integracaoStravaRepository,
            AlunoRepository alunoRepository,
            NotificacaoService notificacaoService,
            StravaProperties stravaProperties,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager) {
        this(integracaoStravaRepository, alunoRepository, notificacaoService, stravaProperties, objectMapper,
                RestClient.builder().requestFactory(criarRequestFactory()), Clock.system(ZONA_SAO_PAULO), transactionManager);
    }

    StravaService(
            IntegracaoStravaRepository integracaoStravaRepository,
            AlunoRepository alunoRepository,
            NotificacaoService notificacaoService,
            StravaProperties stravaProperties,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            Clock clock) {
        this(integracaoStravaRepository, alunoRepository, notificacaoService, stravaProperties,
                objectMapper, restClientBuilder, clock, null);
    }

    StravaService(
            IntegracaoStravaRepository integracaoStravaRepository,
            AlunoRepository alunoRepository,
            NotificacaoService notificacaoService,
            StravaProperties stravaProperties,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.integracaoStravaRepository = integracaoStravaRepository;
        this.alunoRepository = alunoRepository;
        this.notificacaoService = notificacaoService;
        this.stravaProperties = stravaProperties;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.restClient = restClientBuilder.build();
        this.clock = clock;

        if (transactionManager != null) {
            this.txTemplate = new TransactionTemplate(transactionManager);
            this.readOnlyTxTemplate = new TransactionTemplate(transactionManager);
            this.readOnlyTxTemplate.setReadOnly(true);
            this.requiresNewTxTemplate = new TransactionTemplate(transactionManager);
            this.requiresNewTxTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        } else {
            this.txTemplate = null;
            this.readOnlyTxTemplate = null;
            this.requiresNewTxTemplate = null;
        }

        if (!stravaProperties.isMockEnabled() && !stravaProperties.isConfigured()) {
            log.warn("Integração com Strava indisponível: defina STRAVA_CLIENT_ID e STRAVA_CLIENT_SECRET "
                    + "ou STRAVA_MOCK_ENABLED=true para simular em desenvolvimento.");
        }
    }

    private static SimpleClientHttpRequestFactory criarRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(6));
        factory.setReadTimeout(Duration.ofSeconds(12));
        return factory;
    }

    /**
     * Retorna a URL de autorização OAuth 2.0 (US-14 C1).
     * Com mock habilitado, retorna a URL de callback já com um code simulado.
     */
    public StravaAuthUrlResponseDTO obterAuthUrl(Usuario usuario) {
        validarUsuarioAluno(usuario);

        if (stravaProperties.isMockEnabled()) {
            String mockUrl = UriComponentsBuilder.fromUriString(stravaProperties.getRedirectUri())
                    .queryParam("code", "mock_code_" + usuario.getId())
                    .queryParam("scope", SCOPES_SOLICITADOS)
                    .build().encode().toUriString();
            return new StravaAuthUrlResponseDTO(mockUrl, true);
        }

        exigirCredenciais();

        String redirectUriEncoded = URLEncoder.encode(stravaProperties.getRedirectUri(), StandardCharsets.UTF_8);
        String authUrl = STRAVA_OAUTH_AUTHORIZE_URL
                + "?client_id=" + stravaProperties.getClientId()
                + "&response_type=code"
                + "&redirect_uri=" + redirectUriEncoded
                + "&approval_prompt=force"
                + "&scope=" + SCOPES_SOLICITADOS
                + "&state=" + gerarState(usuario.getId());

        return new StravaAuthUrlResponseDTO(authUrl, false);
    }

    /**
     * Conecta a conta Strava com o authorization code retornado no callback (US-14 C1).
     */
    public StravaStatusResponseDTO conectar(Usuario usuario, StravaConectarRequestDTO dto) {
        inReadOnlyTx(() -> obterAlunoPorUsuario(usuario));

        String code = dto != null ? dto.code() : null;
        if (code == null || code.isBlank()) {
            throw new BusinessException("Informe o código de autorização retornado pelo Strava.");
        }

        if (stravaProperties.isMockEnabled()) {
            return inTx(() -> {
                Aluno aluno = obterAlunoPorUsuario(usuario);
                log.info("Conectando Strava em modo mock para o aluno id={}", aluno.getId());
                return salvarIntegracao(
                        aluno,
                        usuario,
                        new TokenStrava("mock_access_token_" + aluno.getId(), "mock_refresh_token_" + aluno.getId(),
                                OffsetDateTime.now(clock).plusDays(30)),
                        999000L,
                        usuario.getNome() != null ? usuario.getNome() : "Atleta Strava (Demo)",
                        SCOPES_SOLICITADOS,
                        true
                );
            });
        }

        exigirCredenciais();

        if (!stateValido(dto.state(), usuario.getId())) {
            log.warn("Callback do Strava com state ausente, inválido ou expirado para o usuario id={}", usuario.getId());
            throw new BusinessException(MSG_ERRO_CONEXAO);
        }

        if (!possuiPermissaoAtividades(dto.scope())) {
            throw new BusinessException(MSG_SEM_PERMISSAO_ATIVIDADES);
        }

        TokenStrava token;
        Long atletaId;
        String nomeAtleta;
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", stravaProperties.getClientId());
            formData.add("client_secret", stravaProperties.getClientSecret());
            formData.add("code", code);
            formData.add("grant_type", "authorization_code");

            String responseBody = restClient.post()
                    .uri(STRAVA_OAUTH_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseBody);
            token = lerToken(root);

            JsonNode athleteNode = root.path("athlete");
            atletaId = athleteNode.hasNonNull("id") ? athleteNode.path("id").asLong(0L) : null;
            String firstname = athleteNode.path("firstname").asString("");
            String lastname = athleteNode.path("lastname").asString("");
            nomeAtleta = (firstname + " " + lastname).trim();
            if (nomeAtleta.isBlank() && usuario.getNome() != null) {
                nomeAtleta = usuario.getNome();
            }
        } catch (RestClientResponseException e) {
            log.error("Erro na resposta do Strava ao trocar code por token: HTTP {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(MSG_ERRO_CONEXAO);
        } catch (Exception e) {
            log.error("Falha ao comunicar com o Strava", e);
            throw new BusinessException(MSG_ERRO_CONEXAO);
        }

        String nomeAtletaFinal = nomeAtleta;
        return inTx(() -> {
            Aluno aluno = obterAlunoPorUsuario(usuario);
            if (atletaId != null && integracaoStravaRepository.existsByAtletaIdAndAlunoIdNot(atletaId, aluno.getId())) {
                log.warn("Atleta Strava id={} já vinculado a outro aluno; conexão recusada para o aluno id={}", atletaId, aluno.getId());
                throw new DuplicateResourceException("Esta conta do Strava já está vinculada a outro aluno.");
            }

            log.info("Strava conectado com sucesso para o aluno id={}, atletaId={}", aluno.getId(), atletaId);
            return salvarIntegracao(aluno, usuario, token, atletaId, nomeAtletaFinal, dto.scope(), false);
        });
    }

    /**
     * Consulta o status da integração Strava para o aluno logado.
     */
    public StravaStatusResponseDTO obterStatus(Usuario usuario) {
        ContextoIntegracao ctx = carregarContextoParaAlunoUsuario(usuario);
        return obterStatusPorContexto(ctx);
    }

    /**
     * Consulta o status do Strava de um aluno a partir da visão do Personal Trainer.
     */
    public StravaStatusResponseDTO obterStatusPorPersonal(UUID alunoId, UUID personalUsuarioId) {
        ContextoIntegracao ctx = carregarContextoParaPersonal(alunoId, personalUsuarioId);
        return obterStatusPorContexto(ctx);
    }

    private StravaStatusResponseDTO obterStatusPorContexto(ContextoIntegracao ctx) {
        if (ctx.integracao() == null) {
            return new StravaStatusResponseDTO(false, false, null, null, false);
        }

        IntegracaoStrava integracao = ctx.integracao();
        boolean ehMock = isMockIntegracao(integracao);

        // US-14 C3: reconexão já pendente não gera nova chamada ao Strava nem nova notificação
        if (integracao.isReconexaoPendente()) {
            return new StravaStatusResponseDTO(true, true, integracao.getAtletaId(), integracao.getNomeAtleta(), ehMock);
        }

        if (!ehMock) {
            try {
                renovarTokenSeNecessario(integracao, ctx.alunoUsuario());
            } catch (BusinessException e) {
                log.warn("Não foi possível renovar o token Strava do aluno id={}: {}", ctx.alunoId(), e.getMessage());
            }
        }

        return new StravaStatusResponseDTO(
                true,
                integracao.isReconexaoPendente(),
                integracao.getAtletaId(),
                integracao.getNomeAtleta(),
                ehMock
        );
    }

    /**
     * Desconecta a conta Strava do aluno: revoga a autorização no Strava e remove a integração.
     */
    public void desconectar(Usuario usuario) {
        ContextoIntegracao ctx = inTx(() -> {
            Aluno aluno = obterAlunoPorUsuario(usuario);
            IntegracaoStrava integracao = integracaoStravaRepository.findByAluno(aluno).orElse(null);
            return new ContextoIntegracao(aluno.getId(), inicializado(aluno.getUsuario()), integracao);
        });

        if (ctx.integracao() == null) {
            return;
        }

        IntegracaoStrava integracao = ctx.integracao();
        if (!isMockIntegracao(integracao) && stravaProperties.isConfigured()) {
            revogarAutorizacao(integracao, usuario);
        }

        limparCacheDoAluno(ctx.alunoId());
        inTxVoid(() -> {
            integracaoStravaRepository.delete(integracao);
            notificacaoService.resolverStravaExpirado(usuario);
            log.info("Integração com Strava removida para o aluno id={}", ctx.alunoId());
        });
    }

    /**
     * Lista atividades recentes do Strava para o aluno logado (US-14 C2).
     */
    public List<StravaAtividadeDTO> listarAtividadesRecentes(Usuario usuario, Integer limite) {
        ContextoIntegracao ctx = carregarContextoParaAlunoUsuario(usuario);
        return listarAtividadesPorContexto(ctx, limite);
    }

    /**
     * Lista atividades recentes do aluno para o personal trainer.
     */
    public List<StravaAtividadeDTO> listarAtividadesPorPersonal(UUID alunoId, UUID personalUsuarioId, Integer limite) {
        ContextoIntegracao ctx = carregarContextoParaPersonal(alunoId, personalUsuarioId);
        return listarAtividadesPorContexto(ctx, limite);
    }

    /**
     * Obtém os detalhes completos de uma atividade do Strava para o aluno logado (inclui calorias).
     */
    public StravaAtividadeDTO obterDetalheAtividade(Usuario usuario, String atividadeId) {
        ContextoIntegracao ctx = carregarContextoParaAlunoUsuario(usuario);
        return obterDetalheAtividadePorContexto(ctx, atividadeId);
    }

    /**
     * Obtém os detalhes completos de uma atividade do Strava do aluno para o personal trainer.
     */
    public StravaAtividadeDTO obterDetalheAtividadePorPersonal(UUID alunoId, UUID personalUsuarioId, String atividadeId) {
        ContextoIntegracao ctx = carregarContextoParaPersonal(alunoId, personalUsuarioId);
        return obterDetalheAtividadePorContexto(ctx, atividadeId);
    }

    private List<StravaAtividadeDTO> listarAtividadesPorContexto(ContextoIntegracao ctx, Integer limite) {
        IntegracaoStrava integracao = exigirIntegracaoConectada(ctx);

        int max = (limite == null || limite <= 0) ? LIMITE_PADRAO : Math.min(limite, LIMITE_MAXIMO);

        if (isMockIntegracao(integracao)) {
            return gerarAtividadesMock(max);
        }

        if (integracao.isReconexaoPendente()) {
            throw new BusinessException(MSG_TOKEN_EXPIRADO);
        }

        String chaveCache = ctx.alunoId() + ":" + max;
        List<StravaAtividadeDTO> emCache = lerCache(cacheListagem, chaveCache);
        if (emCache != null) {
            return emCache;
        }
        registrarChamadaStrava(ctx.alunoId());

        renovarTokenSeNecessario(integracao, ctx.alunoUsuario());

        try {
            String json = restClient.get()
                    .uri(STRAVA_API_ACTIVITIES_URL + "?per_page=" + max)
                    .header("Authorization", "Bearer " + integracao.getAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            JsonNode arrayNode = objectMapper.readTree(json);
            List<StravaAtividadeDTO> lista = new ArrayList<>();

            if (arrayNode.isArray()) {
                for (JsonNode item : arrayNode) {
                    lista.add(converterJsonParaAtividadeDTO(item));
                }
            }

            List<StravaAtividadeDTO> resultado = List.copyOf(lista);
            gravarCache(cacheListagem, chaveCache, resultado, TTL_CACHE_LISTAGEM);
            return resultado;

        } catch (RestClientResponseException e) {
            tratarErroChamadaApiStrava(e, integracao, ctx.alunoUsuario(), "listar atividades");
            throw new BusinessException(MSG_FALHA_COMUNICACAO);
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar atividades do Strava", e);
            throw new BusinessException(MSG_FALHA_COMUNICACAO);
        }
    }

    private StravaAtividadeDTO obterDetalheAtividadePorContexto(ContextoIntegracao ctx, String atividadeId) {
        if (atividadeId == null || atividadeId.isBlank()) {
            throw new BusinessException("Informe o identificador da atividade do Strava.");
        }

        IntegracaoStrava integracao = exigirIntegracaoConectada(ctx);
        String idLimpo = atividadeId.trim();

        if (isMockIntegracao(integracao)) {
            return gerarAtividadesMock(LIMITE_MAXIMO).stream()
                    .filter(a -> idLimpo.equals(a.id()))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Atividade do Strava não encontrada."));
        }

        if (!ID_ATIVIDADE_STRAVA.matcher(idLimpo).matches()) {
            throw new BusinessException(MSG_ID_ATIVIDADE_INVALIDO);
        }

        if (integracao.isReconexaoPendente()) {
            throw new BusinessException(MSG_TOKEN_EXPIRADO);
        }

        String chaveCache = ctx.alunoId() + ":" + idLimpo;
        StravaAtividadeDTO emCache = lerCache(cacheDetalhe, chaveCache);
        if (emCache != null) {
            return emCache;
        }
        registrarChamadaStrava(ctx.alunoId());

        renovarTokenSeNecessario(integracao, ctx.alunoUsuario());

        try {
            String uri = STRAVA_API_ACTIVITY_DETAIL_URL + "/" + UriUtils.encodePathSegment(idLimpo, StandardCharsets.UTF_8);
            String json = restClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + integracao.getAccessToken())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            JsonNode item = objectMapper.readTree(json);
            // M3: a API do Strava também devolve atividades públicas de outros atletas; só aceita as do atleta conectado
            JsonNode atletaNode = item.path("athlete").path("id");
            if (integracao.getAtletaId() != null && !atletaNode.isMissingNode() && !atletaNode.isNull()
                    && atletaNode.asLong(-1L) != integracao.getAtletaId()) {
                log.warn("Atividade id={} pertence a outro atleta; acesso recusado para o aluno id={}", idLimpo, ctx.alunoId());
                throw new NotFoundException(MSG_ATIVIDADE_NAO_ENCONTRADA);
            }
            StravaAtividadeDTO detalhe = converterJsonParaAtividadeDTO(item);
            gravarCache(cacheDetalhe, chaveCache, detalhe, TTL_CACHE_DETALHE);
            return detalhe;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new NotFoundException(MSG_ATIVIDADE_NAO_ENCONTRADA);
            }
            tratarErroChamadaApiStrava(e, integracao, ctx.alunoUsuario(), "consultar detalhes da atividade id=" + idLimpo);
            throw new BusinessException(MSG_FALHA_COMUNICACAO);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao consultar detalhes da atividade id={} no Strava", idLimpo, e);
            throw new BusinessException(MSG_FALHA_COMUNICACAO);
        }
    }

    private void tratarErroChamadaApiStrava(
            RestClientResponseException e,
            IntegracaoStrava integracao,
            Usuario alunoUsuario,
            String contexto) {
        int status = e.getStatusCode().value();
        if (status == 429) {
            String rateUsage = e.getResponseHeaders() != null
                    ? e.getResponseHeaders().getFirst("X-RateLimit-Usage")
                    : null;
            log.warn("Rate limit do Strava (HTTP 429) ao {}: X-RateLimit-Usage={}", contexto, rateUsage);
            throw new StravaRateLimitException(MSG_RATE_LIMIT);
        }
        log.error("Erro ao {} no Strava: HTTP {} - {}", contexto, e.getStatusCode(), e.getResponseBodyAsString());
        if (status == 401) {
            marcarReconexaoPendente(integracao, alunoUsuario);
            if (e.getResponseBodyAsString().contains("activity:read_permission")) {
                throw new BusinessException(MSG_SEM_PERMISSAO_ATIVIDADES);
            }
            throw new BusinessException(MSG_TOKEN_EXPIRADO);
        }
        throw new BusinessException(MSG_FALHA_COMUNICACAO);
    }

    private IntegracaoStrava exigirIntegracaoConectada(ContextoIntegracao ctx) {
        if (ctx.integracao() == null) {
            throw new BusinessException("Conta Strava não conectada. Conecte sua conta do Strava nas configurações.");
        }
        return ctx.integracao();
    }

    private void renovarTokenSeNecessario(IntegracaoStrava integracao, Usuario usuario) {
        if (isMockIntegracao(integracao)) {
            return;
        }

        // Se expira em menos de 5 minutos, renova preventivamente
        if (integracao.getExpiraEm().isAfter(OffsetDateTime.now(clock).plus(ANTECEDENCIA_RENOVACAO))) {
            return;
        }

        UUID lockKey = integracao.getId() != null
                ? integracao.getId()
                : (usuario != null && usuario.getId() != null ? usuario.getId() : UUID.randomUUID());
        Object lock = refreshLocks.computeIfAbsent(lockKey, k -> new Object());
        synchronized (lock) {
            try {
                // Re-consulta o banco em tx curta caso outra thread já tenha renovado o token enquanto esta aguardava o lock
                Optional<IntegracaoStrava> doBanco = inReadOnlyTx(() ->
                        integracao.getId() != null ? integracaoStravaRepository.findById(integracao.getId()) : Optional.empty()
                );
                if (doBanco.isPresent()) {
                    IntegracaoStrava atual = doBanco.get();
                    if (atual.getExpiraEm().isAfter(OffsetDateTime.now(clock).plus(ANTECEDENCIA_RENOVACAO))) {
                        copiarEstadoToken(atual, integracao);
                        return;
                    }
                    integracao.setRefreshToken(atual.getRefreshToken());
                    integracao.setReconexaoPendente(atual.isReconexaoPendente());
                }

                exigirCredenciais();

                log.info("Renovando token do Strava para a integracao id={}", integracao.getId());
                TokenStrava token;
                try {
                    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                    formData.add("client_id", stravaProperties.getClientId());
                    formData.add("client_secret", stravaProperties.getClientSecret());
                    formData.add("grant_type", "refresh_token");
                    formData.add("refresh_token", integracao.getRefreshToken());

                    String responseBody = restClient.post()
                            .uri(STRAVA_OAUTH_TOKEN_URL)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .accept(MediaType.APPLICATION_JSON)
                            .body(formData)
                            .retrieve()
                            .body(String.class);

                    token = lerToken(objectMapper.readTree(responseBody));
                } catch (RestClientResponseException e) {
                    log.error("Strava recusou a renovação do token: HTTP {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    int status = e.getStatusCode().value();
                    if (status == 429) {
                        throw new StravaRateLimitException(MSG_RATE_LIMIT);
                    }
                    if (status == 400 || status == 401) {
                        Optional<IntegracaoStrava> recheck = inReadOnlyTx(() ->
                                integracao.getId() != null ? integracaoStravaRepository.findById(integracao.getId()) : Optional.empty()
                        );
                        if (recheck.isPresent() && recheck.get().getExpiraEm().isAfter(OffsetDateTime.now(clock).plus(ANTECEDENCIA_RENOVACAO))) {
                            copiarEstadoToken(recheck.get(), integracao);
                            return;
                        }
                        marcarReconexaoPendente(integracao, usuario);
                        throw new BusinessException(MSG_TOKEN_EXPIRADO);
                    }
                    throw new BusinessException(MSG_FALHA_COMUNICACAO);
                } catch (Exception e) {
                    log.error("Falha ao renovar token do Strava: {}", e.getMessage());
                    throw new BusinessException(MSG_FALHA_COMUNICACAO);
                }

                TokenStrava novoToken = token;
                inTxVoid(() -> {
                    IntegracaoStrava destino = integracao.getId() != null
                            ? integracaoStravaRepository.findById(integracao.getId()).orElse(integracao)
                            : integracao;
                    destino.setAccessToken(novoToken.accessToken());
                    destino.setRefreshToken(novoToken.refreshToken());
                    destino.setExpiraEm(novoToken.expiraEm());
                    integracaoStravaRepository.save(destino);
                    if (destino != integracao) {
                        copiarEstadoToken(destino, integracao);
                    }
                });
                log.info("Token Strava renovado com sucesso para a integracao id={}", integracao.getId());
            } finally {
                refreshLocks.remove(lockKey, lock);
            }
        }
    }

    private void marcarReconexaoPendente(IntegracaoStrava integracao, Usuario usuario) {
        if (integracao.isReconexaoPendente()) {
            return;
        }
        integracao.setReconexaoPendente(true);
        inRequiresNewTx(() -> {
            IntegracaoStrava destino = integracao.getId() != null
                    ? integracaoStravaRepository.findById(integracao.getId()).orElse(integracao)
                    : integracao;
            destino.setReconexaoPendente(true);
            integracaoStravaRepository.save(destino);
            Usuario usuarioNotificar = (destino.getAluno() != null && destino.getAluno().getUsuario() != null)
                    ? destino.getAluno().getUsuario()
                    : usuario;
            if (usuarioNotificar != null) {
                notificacaoService.notificarStravaExpirado(usuarioNotificar);
            }
        });
    }

    private void revogarAutorizacao(IntegracaoStrava integracao, Usuario usuario) {
        if (integracao.isReconexaoPendente()) {
            log.warn("Token Strava já inválido para a integracao id={}; revogação no Strava não é possível", integracao.getId());
            return;
        }

        try {
            renovarTokenSeNecessario(integracao, usuario);

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("access_token", integracao.getAccessToken());

            restClient.post()
                    .uri(STRAVA_OAUTH_DEAUTHORIZE_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Não foi possível revogar token no Strava (prosseguindo com remoção local): {}", e.getMessage());
        }
    }

    private TokenStrava lerToken(JsonNode root) {
        String accessToken = root.path("access_token").asString("");
        String refreshToken = root.path("refresh_token").asString("");
        long expiresAtEpoch = root.path("expires_at").asLong(0L);
        if (accessToken.isBlank() || refreshToken.isBlank() || expiresAtEpoch <= 0) {
            throw new IllegalStateException("Resposta de token do Strava sem access_token, refresh_token ou expires_at");
        }
        return new TokenStrava(accessToken, refreshToken,
                OffsetDateTime.ofInstant(Instant.ofEpochSecond(expiresAtEpoch), ZoneOffset.UTC));
    }

    private StravaAtividadeDTO converterJsonParaAtividadeDTO(JsonNode item) {
        String id = item.path("id").asString("");
        String nome = item.path("name").asString("Atividade Strava");
        String tipo = item.hasNonNull("sport_type")
                ? item.path("sport_type").asString("")
                : item.path("type").asString("");
        if (tipo.isBlank()) {
            tipo = null;
        }
        String esporte = traduzirEsporte(tipo);

        OffsetDateTime dataHora = lerDataHora(item);
        LocalDate data = lerDataLocal(item, dataHora);

        double distanciaMetros = item.path("distance").asDouble(0.0);
        double distanciaKm = BigDecimal.valueOf(distanciaMetros / 1000.0)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        String distanciaFormatada = String.format(PT_BR, "%.2f km", distanciaKm);

        int movingTime = item.path("moving_time").asInt(item.path("elapsed_time").asInt(0));
        String tempoFormatado = FormatadorTempo.formatarTempoSegundos(Math.max(0, movingTime));
        Integer duracaoMinutos = movingTime <= 0 ? null : Math.max(1, (int) Math.round(movingTime / 60.0));

        int elapsedTime = item.path("elapsed_time").asInt(movingTime);
        String tempoTotalFormatado = FormatadorTempo.formatarTempoSegundos(Math.max(0, elapsedTime));

        Integer hrMedia = item.hasNonNull("average_heartrate")
                ? (int) Math.round(item.path("average_heartrate").asDouble(0.0))
                : null;
        Integer hrMax = item.hasNonNull("max_heartrate")
                ? (int) Math.round(item.path("max_heartrate").asDouble(0.0))
                : null;

        Double averageSpeedMs = item.hasNonNull("average_speed") ? item.path("average_speed").asDouble() : null;
        String ritmoMedio = calcularRitmo(averageSpeedMs, distanciaMetros, movingTime, tipo);
        Double velocidadeMediaMs = calcularVelocidadeMs(averageSpeedMs, distanciaMetros, movingTime);

        Double cadenciaMedia = null;
        if (item.hasNonNull("average_cadence")) {
            double cadenciaBruta = item.path("average_cadence").asDouble();
            if (tipo != null && ESPORTES_CORRIDA_CAMINHADA.contains(tipo.toLowerCase(Locale.ROOT))) {
                cadenciaMedia = (double) Math.round(cadenciaBruta * 2.0);
            } else {
                cadenciaMedia = BigDecimal.valueOf(cadenciaBruta).setScale(1, RoundingMode.HALF_UP).doubleValue();
            }
        }
        Double potenciaMediaWatts = item.hasNonNull("average_watts")
                ? BigDecimal.valueOf(item.path("average_watts").asDouble()).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : null;
        Double potenciaMaximaWatts = item.hasNonNull("max_watts")
                ? BigDecimal.valueOf(item.path("max_watts").asDouble()).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : null;
        Double elevacaoGanhoMetros = item.hasNonNull("total_elevation_gain")
                ? BigDecimal.valueOf(item.path("total_elevation_gain").asDouble()).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : null;

        Double velMediaKmh = null;
        if (averageSpeedMs != null && averageSpeedMs > 0.0) {
            velMediaKmh = BigDecimal.valueOf(averageSpeedMs * 3.6).setScale(1, RoundingMode.HALF_UP).doubleValue();
        } else if (distanciaMetros > 10.0 && movingTime > 0) {
            velMediaKmh = BigDecimal.valueOf((distanciaMetros / 1000.0) / (movingTime / 3600.0))
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        } else if (averageSpeedMs != null) {
            velMediaKmh = 0.0;
        }
        Double velMaximaKmh = item.hasNonNull("max_speed")
                ? BigDecimal.valueOf(item.path("max_speed").asDouble() * 3.6).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : null;

        Double calorias = item.hasNonNull("calories") ? item.path("calories").asDouble(0.0) : null;

        return new StravaAtividadeDTO(
                id,
                nome,
                tipo,
                esporte,
                data,
                dataHora,
                distanciaMetros,
                distanciaKm,
                distanciaFormatada,
                movingTime,
                tempoFormatado,
                duracaoMinutos,
                hrMedia,
                hrMax,
                ritmoMedio,
                tempoTotalFormatado,
                cadenciaMedia,
                potenciaMediaWatts,
                potenciaMaximaWatts,
                elevacaoGanhoMetros,
                velMediaKmh,
                velMaximaKmh,
                calorias,
                velocidadeMediaMs
        );
    }

    private Double calcularVelocidadeMs(Double averageSpeed, double distanciaMetros, int movingTime) {
        double velocidade;
        if (averageSpeed != null && averageSpeed > 0.0) {
            velocidade = averageSpeed;
        } else if (distanciaMetros > 10.0 && movingTime > 0) {
            velocidade = distanciaMetros / movingTime;
        } else {
            return null;
        }
        return BigDecimal.valueOf(velocidade).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Data/hora de início no fuso do atleta: start_date vem em UTC e utc_offset traz o deslocamento local.
     */
    private OffsetDateTime lerDataHora(JsonNode item) {
        String inicioUtc = item.path("start_date").asString("");
        if (inicioUtc.isBlank()) {
            return null;
        }
        try {
            OffsetDateTime dataHora = OffsetDateTime.parse(inicioUtc);
            if (item.hasNonNull("utc_offset")) {
                int offsetSegundos = (int) item.path("utc_offset").asDouble(0.0);
                return dataHora.withOffsetSameInstant(ZoneOffset.ofTotalSeconds(offsetSegundos));
            }
            return dataHora;
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate lerDataLocal(JsonNode item, OffsetDateTime dataHora) {
        // start_date_local já está no horário local do atleta (o "Z" no fim não indica UTC)
        String inicioLocal = item.path("start_date_local").asString("");
        if (!inicioLocal.isBlank()) {
            try {
                return LocalDateTime.parse(inicioLocal, DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
            } catch (Exception ignored) {
            }
        }
        return dataHora != null ? dataHora.toLocalDate() : null;
    }

    private String calcularRitmo(Double averageSpeed, double distanciaMetros, int movingTime, String sportType) {
        String tipoNormalizado = sportType != null ? sportType.toLowerCase(Locale.ROOT) : "";
        if (tipoNormalizado.contains("ride")) {
            return null;
        }

        double velocidadeMs;
        if (averageSpeed != null && averageSpeed > 0.0) {
            velocidadeMs = averageSpeed;
        } else if (distanciaMetros > 10.0 && movingTime > 0) {
            velocidadeMs = distanciaMetros / movingTime;
        } else {
            return null;
        }

        double segundosPorUnidade;
        if (tipoNormalizado.contains("swim")) {
            segundosPorUnidade = 100.0 / velocidadeMs;
        } else if (tipoNormalizado.contains("rowing") || tipoNormalizado.contains("virtualrow")) {
            segundosPorUnidade = 500.0 / velocidadeMs;
        } else {
            segundosPorUnidade = 1000.0 / velocidadeMs;
        }

        int paceSegundos = (int) Math.round(segundosPorUnidade);
        if (paceSegundos <= 0) {
            return null;
        }
        long min = paceSegundos / 60;
        long sec = paceSegundos % 60;
        return String.format("%02d:%02d", min, sec);
    }

    private List<StravaAtividadeDTO> gerarAtividadesMock(int limite) {
        LocalDate hoje = LocalDate.now(clock);
        ZoneOffset offset = ZoneOffset.ofHours(-3);

        List<StravaAtividadeDTO> todas = List.of(
                new StravaAtividadeDTO(
                        "mock-act-1",
                        "Corrida Matinal no Parque",
                        "Run",
                        "Corrida",
                        hoje.minusDays(1),
                        hoje.minusDays(1).atTime(7, 30).atOffset(offset),
                        5230.5,
                        5.23,
                        "5,23 km",
                        1635,
                        "27:15",
                        27,
                        154,
                        172,
                        "05:13",
                        "28:10",
                        166.0,
                        null,
                        null,
                        45.0,
                        11.5,
                        15.8,
                        385.0,
                        3.199
                ),
                new StravaAtividadeDTO(
                        "mock-act-2",
                        "Treino de Tiros / Intervalado",
                        "Run",
                        "Corrida",
                        hoje.minusDays(3),
                        hoje.minusDays(3).atTime(18, 15).atOffset(offset),
                        3500.0,
                        3.50,
                        "3,50 km",
                        1020,
                        "17:00",
                        17,
                        168,
                        185,
                        "04:51",
                        "17:45",
                        174.0,
                        null,
                        null,
                        22.0,
                        12.4,
                        18.0,
                        260.0,
                        3.431
                ),
                new StravaAtividadeDTO(
                        "mock-act-3",
                        "Pedal de Fim de Semana",
                        "Ride",
                        "Ciclismo",
                        hoje.minusDays(5),
                        hoje.minusDays(5).atTime(8, 0).atOffset(offset),
                        21400.0,
                        21.40,
                        "21,40 km",
                        3120,
                        "52:00",
                        52,
                        142,
                        160,
                        null,
                        "55:30",
                        82.0,
                        185.0,
                        340.0,
                        210.0,
                        24.7,
                        42.5,
                        610.0,
                        6.859
                ),
                new StravaAtividadeDTO(
                        "mock-act-4",
                        "Longão de Domingo 15 km",
                        "Run",
                        "Corrida",
                        hoje.minusDays(7),
                        hoje.minusDays(7).atTime(6, 30).atOffset(offset),
                        15000.0,
                        15.0,
                        "15,00 km",
                        4800,
                        "01:20:00",
                        80,
                        158,
                        176,
                        "05:20",
                        "01:22:30",
                        168.0,
                        null,
                        null,
                        120.0,
                        11.3,
                        14.5,
                        920.0,
                        3.125
                ),
                new StravaAtividadeDTO(
                        "mock-act-5",
                        "Natação Contínua 1500m",
                        "Swim",
                        "Natação",
                        hoje.minusDays(8),
                        hoje.minusDays(8).atTime(7, 0).atOffset(offset),
                        1500.0,
                        1.5,
                        "1,50 km",
                        1800,
                        "30:00",
                        30,
                        148,
                        165,
                        "02:00",
                        "32:00",
                        30.0,
                        null,
                        null,
                        null,
                        3.0,
                        3.8,
                        310.0,
                        0.833
                ),
                new StravaAtividadeDTO(
                        "mock-act-6",
                        "Remo Ergômetro 5 km",
                        "Rowing",
                        "Remo",
                        hoje.minusDays(9),
                        hoje.minusDays(9).atTime(17, 30).atOffset(offset),
                        5000.0,
                        5.0,
                        "5,00 km",
                        1260,
                        "21:00",
                        21,
                        160,
                        178,
                        "02:06",
                        "21:30",
                        28.0,
                        220.0,
                        310.0,
                        null,
                        14.3,
                        17.0,
                        290.0,
                        3.968
                )
        );

        return todas.subList(0, Math.min(Math.max(limite, 0), todas.size()));
    }

    private String traduzirEsporte(String tipo) {
        if (tipo == null) return "Treino";
        return switch (tipo.toLowerCase()) {
            case "run", "trailrun", "virtualrun" -> "Corrida";
            case "ride", "virtualride", "ebikeride", "mountainbikeride", "emountainbikeride", "gravelride" -> "Ciclismo";
            case "swim" -> "Natação";
            case "tennis" -> "Tênis";
            case "walk", "hike" -> "Caminhada";
            case "weighttraining", "workout" -> "Musculação";
            case "rowing", "virtualrow" -> "Remo";
            default -> tipo;
        };
    }

    private boolean isMockIntegracao(IntegracaoStrava integracao) {
        return integracao.getAccessToken() != null && integracao.getAccessToken().startsWith("mock");
    }

    /**
     * Integrações criadas em modo mock não valem quando o mock está desligado: são removidas
     * para não exibir atividades simuladas como se fossem do aluno.
     */
    private Optional<IntegracaoStrava> buscarIntegracao(Aluno aluno) {
        Optional<IntegracaoStrava> opt = integracaoStravaRepository.findByAluno(aluno);
        if (opt.isPresent() && isMockIntegracao(opt.get()) && !stravaProperties.isMockEnabled()) {
            log.info("Removendo integração Strava simulada do aluno id={} (mock desabilitado)", aluno.getId());
            integracaoStravaRepository.delete(opt.get());
            return Optional.empty();
        }
        return opt;
    }

    private StravaStatusResponseDTO salvarIntegracao(
            Aluno aluno,
            Usuario usuario,
            TokenStrava token,
            Long atletaId,
            String nomeAtleta,
            String escopo,
            boolean ehMock) {

        IntegracaoStrava integracao = integracaoStravaRepository.findByAluno(aluno)
                .orElseGet(() -> IntegracaoStrava.builder().aluno(aluno).build());

        integracao.setAccessToken(token.accessToken());
        integracao.setRefreshToken(token.refreshToken());
        integracao.setExpiraEm(token.expiraEm());
        integracao.setAtletaId(atletaId);
        integracao.setNomeAtleta(nomeAtleta);
        integracao.setEscopo(escopo);
        integracao.setConectadoEm(OffsetDateTime.now(clock));
        integracao.setReconexaoPendente(false);

        integracaoStravaRepository.save(integracao);
        notificacaoService.resolverStravaExpirado(usuario);
        limparCacheDoAluno(aluno.getId());

        return new StravaStatusResponseDTO(true, false, atletaId, nomeAtleta, ehMock);
    }

    private boolean possuiPermissaoAtividades(String escopo) {
        if (escopo == null || escopo.isBlank()) {
            return false;
        }
        return Arrays.stream(escopo.split(","))
                .map(String::trim)
                .anyMatch(SCOPES_ATIVIDADES::contains);
    }

    /**
     * O state amarra o callback ao aluno que iniciou o fluxo (proteção contra CSRF no OAuth):
     * base64url("usuarioId:epochSegundos") + "." + HMAC-SHA256 assinado com o client secret.
     */
    private String gerarState(UUID usuarioId) {
        String payload = usuarioId + ":" + Instant.now(clock).getEpochSecond();
        String payloadCodificado = BASE64_URL.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return payloadCodificado + "." + assinar(payloadCodificado);
    }

    private boolean stateValido(String state, UUID usuarioId) {
        if (state == null || state.isBlank()) {
            return false;
        }
        String[] partes = state.split("\\.", -1);
        if (partes.length != 2) {
            return false;
        }

        byte[] assinaturaEsperada = assinar(partes[0]).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(assinaturaEsperada, partes[1].getBytes(StandardCharsets.UTF_8))) {
            return false;
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(partes[0]), StandardCharsets.UTF_8);
            String[] campos = payload.split(":", -1);
            if (campos.length != 2 || !campos[0].equals(usuarioId.toString())) {
                return false;
            }
            Instant emitidoEm = Instant.ofEpochSecond(Long.parseLong(campos[1]));
            Instant agora = Instant.now(clock);
            return !emitidoEm.isAfter(agora) && emitidoEm.plus(VALIDADE_STATE).isAfter(agora);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String assinar(String valor) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stravaProperties.getClientSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return BASE64_URL.encodeToString(mac.doFinal(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao assinar o state do OAuth do Strava", e);
        }
    }

    private void exigirCredenciais() {
        if (!stravaProperties.isConfigured()) {
            log.error("Strava sem STRAVA_CLIENT_ID/STRAVA_CLIENT_SECRET configurados e com mock desabilitado");
            throw new BusinessException(MSG_ERRO_CONEXAO);
        }
    }

    private Aluno obterAlunoDoPersonal(UUID alunoId, UUID personalUsuarioId) {
        Aluno aluno = alunoRepository.findById(alunoId)
                .orElseThrow(() -> new NotFoundException("Aluno não encontrado ou não pertence a este Personal Trainer."));

        if (!aluno.getPersonal().getUsuario().getId().equals(personalUsuarioId)) {
            throw new NotFoundException("Aluno não encontrado ou não pertence a este Personal Trainer.");
        }

        // RN-014: Aluno inativo não tem dados compartilhados
        if (aluno.getStatus() != StatusConvite.ATIVO) {
            throw new BusinessException("Erro. Este aluno está inativo.");
        }

        return aluno;
    }

    private Aluno obterAlunoPorUsuario(Usuario usuario) {
        validarUsuarioAluno(usuario);
        return alunoRepository.findByUsuario(usuario)
                .orElseThrow(() -> new NotFoundException("Perfil de aluno não encontrado para este usuário."));
    }

    private void validarUsuarioAluno(Usuario usuario) {
        if (usuario == null || usuario.getRole() != Role.ALUNO) {
            throw new BusinessException("Apenas alunos podem vincular sua conta do Strava.");
        }
    }

    private ContextoIntegracao carregarContextoParaAlunoUsuario(Usuario usuario) {
        return inTx(() -> {
            Aluno aluno = obterAlunoPorUsuario(usuario);
            IntegracaoStrava integracao = buscarIntegracao(aluno).orElse(null);
            return new ContextoIntegracao(aluno.getId(), inicializado(usuario), integracao);
        });
    }

    private ContextoIntegracao carregarContextoParaPersonal(UUID alunoId, UUID personalUsuarioId) {
        return inTx(() -> {
            Aluno aluno = obterAlunoDoPersonal(alunoId, personalUsuarioId);
            IntegracaoStrava integracao = buscarIntegracao(aluno).orElse(null);
            return new ContextoIntegracao(aluno.getId(), inicializado(aluno.getUsuario()), integracao);
        });
    }

    private Usuario inicializado(Usuario usuario) {
        if (usuario != null) {
            usuario.getId();
            usuario.getNome();
        }
        return usuario;
    }

    private void copiarEstadoToken(IntegracaoStrava origem, IntegracaoStrava destino) {
        destino.setAccessToken(origem.getAccessToken());
        destino.setRefreshToken(origem.getRefreshToken());
        destino.setExpiraEm(origem.getExpiraEm());
        destino.setReconexaoPendente(origem.isReconexaoPendente());
    }

    private <T> T inTx(java.util.function.Supplier<T> action) {
        if (txTemplate == null) {
            return action.get();
        }
        return txTemplate.execute(status -> action.get());
    }

    private void inTxVoid(Runnable action) {
        inTx(() -> {
            action.run();
            return null;
        });
    }

    private <T> T inReadOnlyTx(java.util.function.Supplier<T> action) {
        if (readOnlyTxTemplate == null) {
            return action.get();
        }
        return readOnlyTxTemplate.execute(status -> action.get());
    }

    private void inRequiresNewTx(Runnable action) {
        if (requiresNewTxTemplate == null) {
            action.run();
            return;
        }
        requiresNewTxTemplate.executeWithoutResult(status -> action.run());
    }

    /**
     * M3: limite de chamadas reais ao Strava por aluno em janela deslizante (aluno e personal compartilham a cota do aluno).
     */
    private void registrarChamadaStrava(UUID alunoId) {
        if (alunoId == null) {
            return;
        }
        Instant agora = Instant.now(clock);
        Deque<Instant> janela = chamadasPorAluno.computeIfAbsent(alunoId, k -> new ArrayDeque<>());
        synchronized (janela) {
            Instant inicioJanela = agora.minus(JANELA_LIMITE_POR_ALUNO);
            while (!janela.isEmpty() && !janela.peekFirst().isAfter(inicioJanela)) {
                janela.pollFirst();
            }
            if (janela.size() >= LIMITE_CHAMADAS_POR_ALUNO) {
                log.warn("Limite de consultas ao Strava por aluno atingido: aluno id={}", alunoId);
                throw new StravaRateLimitException(MSG_LIMITE_POR_ALUNO);
            }
            janela.addLast(agora);
        }
    }

    private <T> T lerCache(ConcurrentHashMap<String, EntradaCache<T>> cache, String chave) {
        EntradaCache<T> entrada = cache.get(chave);
        if (entrada == null) {
            return null;
        }
        if (!entrada.expiraEm().isAfter(Instant.now(clock))) {
            cache.remove(chave, entrada);
            return null;
        }
        return entrada.valor();
    }

    private <T> void gravarCache(ConcurrentHashMap<String, EntradaCache<T>> cache, String chave, T valor, Duration ttl) {
        Instant agora = Instant.now(clock);
        if (cache.size() >= TAMANHO_PARA_LIMPEZA_CACHE) {
            cache.values().removeIf(e -> !e.expiraEm().isAfter(agora));
        }
        cache.put(chave, new EntradaCache<>(valor, agora.plus(ttl)));
    }

    private void limparCacheDoAluno(UUID alunoId) {
        if (alunoId == null) {
            return;
        }
        String prefixo = alunoId + ":";
        cacheDetalhe.keySet().removeIf(k -> k.startsWith(prefixo));
        cacheListagem.keySet().removeIf(k -> k.startsWith(prefixo));
    }

    private record EntradaCache<T>(T valor, Instant expiraEm) {}

    private record ContextoIntegracao(UUID alunoId, Usuario alunoUsuario, IntegracaoStrava integracao) {}

    private record TokenStrava(String accessToken, String refreshToken, OffsetDateTime expiraEm) {}
}
