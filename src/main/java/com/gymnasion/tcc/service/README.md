# Business Services (`service`)

## Overview
Core business logic, domain validation, multi-tenant data isolation, notification orchestration, OpenPDF report generation, in-memory login rate limiting, SMTP email dispatch, and Strava API v3 OAuth/activity synchronization services.

## Interface
- `[AcompanhamentoTreinoService.java:AcompanhamentoTreinoService]` (`obterAcompanhamentoAluno`, `obterResumoPendencias`, `listarNotificacoes`, `marcarComoLida`, `sincronizarNotificacaoParaAluno`)
- `[AgregadorEvolucaoDesempenho.java:AgregadorEvolucaoDesempenho]` (`agregarSeriesMetricas`, `agregarEvolucaoRpe`, `calcularResumoGeral`, `obterDataReferencia`, `obterDescricaoBorg`)
- `[AlunoService.java:AlunoService]` (`obterMeuPerfil`, `getAlunosVinculados`, `getAlunosInativos`, `getAlunosPendentes`, `desativarAluno`, `reativarAluno`, `aprovarAluno`, `recusarAluno`)
- `[AuthService.java:AuthService]` (`login`)
- `[CorrecaoDesempenhoService.java:CorrecaoDesempenhoService]` (`consultarRegistro`, `corrigirRegistro`, `consultarAuditoria`)
- `[DashboardEvolucaoService.java:DashboardEvolucaoService]` (`obterDashboardPeloPersonal`, `obterDashboardPeloAluno`)
- `[EmailService.java:EmailService]` (`carregarTemplates`, `enviarEmail`, `enviarEmailHtmlComLogo`, `enviarConviteAluno`, `notificarAprovacaoAluno`, `notificarRecusaAluno`, `notificarAlunosPendentes`)
- `[LembreteAlunosPendentesService.java:LembreteAlunosPendentesService]` (`processarLembretes`)
- `[LoginAttemptService.java:LoginAttemptService]` (`expurgarTentativasExpiradas`, `loginFailed`, `loginSucceeded`, `isBlocked`, `getTentativas`, `reset`, `resetAll`, `setClock`)
- `[MetricaService.java:MetricaService]` (`criarMetrica`, `listarMetricasPorModalidade`, `atualizarMetrica`, `excluirMetrica`)
- `[ModalidadeService.java:ModalidadeService]` (`listarTodas`, `buscarPorId`, `criar`, `desativar`, `ativar`, `atualizar`)
- `[NotificacaoService.java:NotificacaoService]` (`notificarNovaSolicitacao`, `resolverSolicitacao`, `notificarSolicitacaoAprovada`, `notificarTreinoConcluido`, `notificarConviteEsgotado`, `notificarStravaExpirado`, `resolverStravaExpirado`, `notificarRelatorioPronto`, `buscarLembreteAlunosPendentes`, `registrarLembreteAlunosPendentes`, `marcarTodasComoLidas`, `contarNaoLidas`, `listarPorUsuario`, `marcarComoLida`, `toDTO`, `formatarDuracao`, `criarNotificacao`)
- `[ObservacaoFeedbackService.java:ObservacaoFeedbackService]` (`registrarFeedbackPeloPersonal`, `registrarObservacaoPeloAluno`, `registrar`, `listarObservacoesEFeedbacksPeloPersonal`, `listarObservacoesEFeedbacksPeloAluno`, `listarPorAluno`)
- `[PersonalConviteService.java:PersonalConviteService]` (`gerarLinkConvite`, `registroAlunoConvite`)
- `[RelatorioPdfGenerator.java:RelatorioPdfGenerator]` (`gerar`)
- `[RelatorioPerformanceService.java:RelatorioPerformanceService]` (`gerarRelatorioPeloPersonal`, `gerarRelatorioPeloAluno`)
- `[RotinaTreinoService.java:RotinaTreinoService]` (`criarRotinaTreino`, `listarPorPersonal`, `listarPorAluno`, `listarMinhasRotinas`, `buscarPorId`, `atualizarRotina`, `excluirRotina`, `vincularAlunos`, `vincularTurma`, `desvincularAluno`, `encerrarRotina`, `reutilizarRotina`, `atribuirRotina`, `adicionarSessao`, `consultarPlanejamento`, `consultarSessao`, `editarSessaoFutura`, `duplicarSessao`, `duplicarSemana`, `obterFormularioExecucao`, `registrarExecucao`, `validarExecucao`, `consultarHistoricoRotina`, `consultarHistoricoAluno`, `toSessaoResponseDTO`)
- `[StravaService.java:StravaService]` (`obterAuthUrl`, `conectar`, `obterStatus`, `obterStatusPorPersonal`, `desconectar`, `listarAtividadesRecentes`, `listarAtividadesPorPersonal`)
- `[TurmaService.java:TurmaService]` (`criarTurma`, `listarTurmas`, `buscarPorId`, `atualizarTurma`, `adicionarAluno`, `removerAluno`)
- `[UsuarioService.java:UsuarioService]` (`criarPersonal`, `obterMeuPerfil`, `atualizarDadosCadastrais`, `obterPreferencias`, `atualizarPreferencias`, `alterarSenha`, `validarPoliticaSenhaForte`)
- `[ValidadorValorMetrica.java:ValidadorValorMetrica]` (`validar`)

## Invariants
- `[LoginAttemptService.java:MAX_TENTATIVAS]` Blocks user authentication for 5 minutes (`TEMPO_BLOQUEIO`) after 5 consecutive failed login attempts (`RN-010`).
- `[AuthService.java:login]` Students with status `INATIVO`, `PENDENTE`, or `RECUSADO` are denied authentication and active operations (`RN-014`).
- `[UsuarioService.java:validarPoliticaSenhaForte]` Passwords must be at least 8 characters and include uppercase, lowercase, digit, and special character (`RN-002`).
- `[PersonalConviteService.java:gerarLinkConvite]` Invite links expire in 15 minutes, allow at most 30 registrations (`maximoUsuarios = 30`), and require terms acceptance (`TermoAceite` v1.0) on student registration.
- `[MetricaService.java:atualizarMetrica]` Metric data type cannot be modified once historical records or completed session links exist; deletion removes links from `PLANEJADA` sessions and performs a soft-delete (`ativo = false`) when history exists (`RN-017`).
- `[ValidadorValorMetrica.java:validar]` Enforces `RN-015` metric formats: `NUMERICO` (valid `BigDecimal`), `TEMPO` (`mm:ss` or `hh:mm:ss` via `FORMATO_TEMPO`), `FREQUENCIA_CARDIACA` (integer `30..250` bpm), and max length 255 characters.
- `[RotinaTreinoService.java:criarRotinaTreino]` A student can have at most 1 active routine per sport modality at a time (`US-11` / `US-25 C6`), every active routine must maintain at least 1 linked student (`RN-012`), and routines with executed sessions cannot be physically deleted (`US-12`).
- `[RotinaTreinoService.java:consultarPlanejamento]` Students viewing an active routine can only see the current and previous week's sessions and cannot execute future-week sessions (`US-21 C2`) or alter an already confirmed session execution (`US-22 C8`).
- `[CorrecaoDesempenhoService.java:corrigirRegistro]` Only the responsible Personal Trainer can correct a session's performance record or fill pending personal metrics; all changes are validated before saving and recorded in `AuditoriaDesempenho` as `INCLUSAO` or `CORRECAO` (`US-23`).
- `[AgregadorEvolucaoDesempenho.java:agregarEvolucaoRpe]` Evolution charts and RPE trends require at least 3 recorded sessions in the modality history (`US-15 C1` / `US-20 C3`).
- `[ObservacaoFeedbackService.java:ObservacaoFeedbackService]` Profile observations and feedbacks are append-only/immutable with no update or delete operations (`US-26 C5`).
- `[StravaService.java:gerarState]` OAuth state tokens are HMAC-SHA256 signed with `clientSecret` and valid for 10 minutes (`VALIDADE_STATE`), activity scopes (`activity:read` or `activity:read_all`) are mandatory, and a Strava athlete ID cannot be linked to more than one student.

## Side Effects
- `[AlunoService.java:aprovarAluno]` Mutates `Aluno`, `Usuario`, `TermoAceite`, and `Notificacao` records in PostgreSQL and publishes `SolicitacaoAlunoAvaliadaEvent`.
- `[PersonalConviteService.java:gerarLinkConvite]` Persists `PersonalConvites`, `Usuario`, `TermoAceite`, and `Aluno` entities and publishes `ConviteAlunoCriadoEvent`.
- `[LembreteAlunosPendentesService.java:processarLembretes]` Queries pending students, upserts in-app notifications, and publishes `LembreteAlunosPendentesEvent`.
- `[EmailService.java:EmailService]` Reads classpath email templates/logos and sends plain-text or HTML emails over SMTP via `JavaMailSender`.
- `[LoginAttemptService.java:expurgarTentativasExpiradas]` Mutates in-memory `ConcurrentHashMap` of login attempts and runs a `@Scheduled` cleanup every `600,000` ms.
- `[RotinaTreinoService.java:registrarExecucao]` Persists `RotinaTreino`, `SessaoTreino`, `Desempenho`, `RegistroMetrica`, and `AuditoriaDesempenho` entities and synchronizes pending metric and workout completion notifications.
- `[RelatorioPdfGenerator.java:gerar]` Loads embedded Plus Jakarta Sans TrueType fonts from classpath and renders binary PDF documents in memory using OpenPDF.
- `[StravaService.java:StravaService]` Performs external HTTPS calls to Strava OAuth and Activities API v3 endpoints (`authorize`, token exchange/refresh, `deauthorize`, `athlete/activities`) via `RestClient` and persists `IntegracaoStrava` tokens.

## Verification
- Run: `./mvnw test`
