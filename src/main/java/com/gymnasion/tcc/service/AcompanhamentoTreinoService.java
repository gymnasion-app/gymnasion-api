package com.gymnasion.tcc.service;

import com.gymnasion.tcc.domain.*;
import com.gymnasion.tcc.domain.enums.ResponsavelPreenchimento;
import com.gymnasion.tcc.domain.enums.StatusSessaoTreino;
import com.gymnasion.tcc.dto.*;
import com.gymnasion.tcc.exceptions.NotFoundException;
import com.gymnasion.tcc.repository.*;
import com.gymnasion.tcc.domain.enums.TipoNotificacao;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@AllArgsConstructor
public class AcompanhamentoTreinoService {

    private final PersonalTrainerRepository personalTrainerRepository;
    private final AlunoRepository alunoRepository;
    private final RotinaTreinoRepository rotinaTreinoRepository;
    private final SessaoTreinoRepository sessaoTreinoRepository;
    private final NotificacaoRepository notificacaoRepository;

    public static final TipoNotificacao TIPO_NOTIFICACAO_PENDENCIAS = TipoNotificacao.METRICAS_PENDENTES;

    @Transactional
    public AcompanhamentoAlunoResponseDTO obterAcompanhamentoAluno(Usuario usuario, UUID alunoId) {
        PersonalTrainer personal = obterPersonalLogado(usuario);
        Aluno aluno = alunoRepository.findByIdAndPersonalId(alunoId, personal.getId())
                .orElseThrow(() -> new NotFoundException("Aluno não encontrado ou não pertence a este Personal Trainer."));

        // Busca todas as rotinas ativas do aluno, ou a mais recente caso nenhuma esteja ativa
        List<RotinaTreino> todasRotinas = rotinaTreinoRepository.findByPersonalTrainerIdAndAlunosId(personal.getId(), aluno.getId());
        List<RotinaTreino> rotinasAtivasList = todasRotinas.stream()
                .filter(r -> Boolean.TRUE.equals(r.getAtivo()))
                .toList();

        RotinaTreino rotinaPrimaria = !rotinasAtivasList.isEmpty()
                ? rotinasAtivasList.getFirst()
                : (!todasRotinas.isEmpty() ? todasRotinas.get(todasRotinas.size() - 1) : null);

        List<AcompanhamentoAlunoResponseDTO.RotinaAtivaAcompanhamentoDTO> rotinasAtivasDTO = new ArrayList<>();
        RotinaTrackingSnapshot snapshotPrimario = new RotinaTrackingSnapshot(List.of(), List.of(), 0, 0);
        int totalMetricasPendentesNotificacao = 0;

        if (!rotinasAtivasList.isEmpty()) {
            for (int i = 0; i < rotinasAtivasList.size(); i++) {
                RotinaTreino rotinaAtivaItem = rotinasAtivasList.get(i);
                RotinaTrackingSnapshot snapshot = montarSnapshotRotina(rotinaAtivaItem);
                if (i == 0) {
                    snapshotPrimario = snapshot;
                }
                totalMetricasPendentesNotificacao += snapshot.todasMetricasPendentes().size();
                rotinasAtivasDTO.add(new AcompanhamentoAlunoResponseDTO.RotinaAtivaAcompanhamentoDTO(
                        rotinaAtivaItem.getId(),
                        rotinaAtivaItem.getTitulo(),
                        rotinaAtivaItem.getModalidade() != null ? rotinaAtivaItem.getModalidade().getId() : null,
                        rotinaAtivaItem.getModalidade() != null ? rotinaAtivaItem.getModalidade().getNome() : null,
                        rotinaAtivaItem.getFrequenciaSemanal(),
                        snapshot.todasMetricasPendentes().size(),
                        snapshot.totalPlanejadas(),
                        snapshot.totalConcluidas(),
                        snapshot.todasMetricasPendentes(),
                        snapshot.sessoesDTO()
                ));
            }
        } else if (rotinaPrimaria != null) {
            snapshotPrimario = montarSnapshotRotina(rotinaPrimaria);
            totalMetricasPendentesNotificacao = snapshotPrimario.todasMetricasPendentes().size();
        }

        // Sincroniza notificação usando o total consolidado de métricas pendentes das rotinas ativas
        sincronizarNotificacaoAluno(personal, aluno, totalMetricasPendentesNotificacao);

        String nomeAluno = aluno.getUsuario() != null ? aluno.getUsuario().getNome() : null;
        String emailAluno = aluno.getUsuario() != null ? aluno.getUsuario().getEmail() : null;

        return new AcompanhamentoAlunoResponseDTO(
                aluno.getId(),
                nomeAluno,
                emailAluno,
                rotinaPrimaria != null ? rotinaPrimaria.getId() : null,
                rotinaPrimaria != null ? rotinaPrimaria.getTitulo() : null,
                rotinaPrimaria != null && rotinaPrimaria.getModalidade() != null ? rotinaPrimaria.getModalidade().getId() : null,
                rotinaPrimaria != null && rotinaPrimaria.getModalidade() != null ? rotinaPrimaria.getModalidade().getNome() : null,
                snapshotPrimario.todasMetricasPendentes().size(),
                snapshotPrimario.totalPlanejadas(),
                snapshotPrimario.totalConcluidas(),
                snapshotPrimario.todasMetricasPendentes(),
                snapshotPrimario.sessoesDTO(),
                rotinasAtivasDTO
        );
    }

    private record RotinaTrackingSnapshot(
            List<MetricaPendenteDTO> todasMetricasPendentes,
            List<SessaoAcompanhamentoDTO> sessoesDTO,
            int totalPlanejadas,
            int totalConcluidas
    ) {}

    private RotinaTrackingSnapshot montarSnapshotRotina(RotinaTreino rotina) {
        List<SessaoTreino> sessoes = sessaoTreinoRepository.findByRotinaTreinoIdOrderBySemanaAscDataPlanejadaAsc(rotina.getId());
        List<MetricaPendenteDTO> todasMetricasPendentes = new ArrayList<>();
        List<SessaoAcompanhamentoDTO> sessoesDTO = new ArrayList<>();

        int totalPlanejadas = 0;
        int totalConcluidas = 0;

        for (SessaoTreino sessao : sessoes) {
            if (sessao.getStatus() == StatusSessaoTreino.CONCLUIDA) {
                totalConcluidas++;
            } else {
                totalPlanejadas++;
            }

            List<MetricaPendenteDTO> pendenciasSessao = extrairPendenciasSessao(sessao);
            todasMetricasPendentes.addAll(pendenciasSessao);

            DesempenhoResponseDTO desempenhoDTO = toDesempenhoResponseDTO(sessao.getDesempenho());

            List<RotinaTreinoResponseDTO.MetricaResumoDTO> metricasSessaoDTO = extrairMetricasSessaoDTO(sessao);
            List<UUID> metricasIds = metricasSessaoDTO.stream()
                    .map(RotinaTreinoResponseDTO.MetricaResumoDTO::id)
                    .toList();

            sessoesDTO.add(new SessaoAcompanhamentoDTO(
                    sessao.getId(),
                    sessao.getSemana(),
                    sessao.getDataPlanejada(),
                    sessao.getDataReal(),
                    sessao.getDuracao(),
                    sessao.getAtividades(),
                    sessao.getStatus(),
                    sessao.getConcluidaAluno(),
                    sessao.getValidadaPersonal(),
                    !pendenciasSessao.isEmpty(),
                    pendenciasSessao.size(),
                    pendenciasSessao,
                    desempenhoDTO,
                    metricasIds,
                    metricasSessaoDTO
            ));
        }

        return new RotinaTrackingSnapshot(todasMetricasPendentes, sessoesDTO, totalPlanejadas, totalConcluidas);
    }

    @Transactional
    public ResumoPendenciasResponseDTO obterResumoPendencias(Usuario usuario) {
        PersonalTrainer personal = obterPersonalLogado(usuario);
        List<Aluno> alunos = alunoRepository.findByPersonalId(personal.getId());

        List<ResumoPendenciasResponseDTO.AlunoPendenciaItemDTO> itens = new ArrayList<>();
        int totalGeralPendencias = 0;

        for (Aluno aluno : alunos) {
            List<RotinaTreino> todasRotinas = rotinaTreinoRepository.findByPersonalTrainerIdAndAlunosId(personal.getId(), aluno.getId());

            if (todasRotinas.isEmpty()) {
                sincronizarNotificacaoAluno(personal, aluno, 0);
                continue;
            }

            RotinaTreino rotinaReferencia = todasRotinas.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getAtivo()))
                    .findFirst()
                    .orElse(todasRotinas.get(todasRotinas.size() - 1));

            int pendenciasAluno = 0;
            RotinaTreino rotinaComPendencia = rotinaReferencia;

            // US-25 C8: Considera sessões da rotina ativa ou sessões concluídas de rotinas encerradas
            for (RotinaTreino rotina : todasRotinas) {
                List<SessaoTreino> sessoes = sessaoTreinoRepository.findByRotinaTreinoIdOrderBySemanaAscDataPlanejadaAsc(rotina.getId());
                for (SessaoTreino sessao : sessoes) {
                    if (Boolean.TRUE.equals(rotina.getAtivo()) || sessao.getStatus() == StatusSessaoTreino.CONCLUIDA) {
                        int qtd = extrairPendenciasSessao(sessao).size();
                        if (qtd > 0) {
                            pendenciasAluno += qtd;
                            rotinaComPendencia = rotina;
                        }
                    }
                }
            }

            // US-24 C4/C5/C6: Atualiza ou remove notificação do aluno
            sincronizarNotificacaoAluno(personal, aluno, pendenciasAluno);

            if (pendenciasAluno > 0) {
                totalGeralPendencias += pendenciasAluno;
                String nome = aluno.getUsuario() != null ? aluno.getUsuario().getNome() : "Aluno";
                itens.add(new ResumoPendenciasResponseDTO.AlunoPendenciaItemDTO(
                        aluno.getId(),
                        nome,
                        rotinaComPendencia.getId(),
                        rotinaComPendencia.getTitulo(),
                        pendenciasAluno
                ));
            }
        }

        return new ResumoPendenciasResponseDTO(
                itens.size(),
                totalGeralPendencias,
                itens
        );
    }

    @Transactional
    public Page<NotificacaoResponseDTO> listarNotificacoes(Usuario usuario, Pageable pageable) {
        PersonalTrainer personal = obterPersonalLogado(usuario);
        // Atualiza pendências para garantir consistência
        obterResumoPendencias(usuario);

        return notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(personal.getUsuario().getId(), pageable)
                .map(this::toNotificacaoResponseDTO);
    }

    @Transactional
    public List<NotificacaoResponseDTO> listarNotificacoes(Usuario usuario) {
        PersonalTrainer personal = obterPersonalLogado(usuario);
        // Atualiza pendências para garantir consistência
        obterResumoPendencias(usuario);

        return notificacaoRepository.findByUsuarioIdOrderByDataCriacaoDesc(personal.getUsuario().getId())
                .stream()
                .map(this::toNotificacaoResponseDTO)
                .toList();
    }

    @Transactional
    public NotificacaoResponseDTO marcarComoLida(Usuario usuario, UUID notificacaoId) {
        Notificacao notificacao = notificacaoRepository.findById(notificacaoId)
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada."));

        if (!notificacao.getUsuario().getId().equals(usuario.getId())) {
            throw new NotFoundException("Notificação não encontrada.");
        }

        // US-24 C7: Apenas visualizar a notificação não resolve a pendência
        notificacao.setLida(true);
        notificacao = notificacaoRepository.save(notificacao);
        return toNotificacaoResponseDTO(notificacao);
    }

    @Transactional
    public void sincronizarNotificacaoParaAluno(Aluno aluno) {
        if (aluno == null || aluno.getId() == null) {
            return;
        }

        Aluno alunoCompleto = alunoRepository.findById(aluno.getId()).orElse(aluno);
        PersonalTrainer personal = alunoCompleto.getPersonal();
        if (personal == null || personal.getUsuario() == null) {
            return;
        }

        List<RotinaTreino> todasRotinas = rotinaTreinoRepository.findByPersonalTrainerIdAndAlunosId(personal.getId(), alunoCompleto.getId());

        int pendencias = 0;
        for (RotinaTreino rotina : todasRotinas) {
            List<SessaoTreino> sessoes = sessaoTreinoRepository.findByRotinaTreinoIdOrderBySemanaAscDataPlanejadaAsc(rotina.getId());
            for (SessaoTreino sessao : sessoes) {
                if (Boolean.TRUE.equals(rotina.getAtivo()) || sessao.getStatus() == StatusSessaoTreino.CONCLUIDA) {
                    pendencias += extrairPendenciasSessao(sessao).size();
                }
            }
        }

        sincronizarNotificacaoAluno(personal, alunoCompleto, pendencias);
    }


    /**
     * US-24 C4, C5, C6: Criação, atualização ou remoção da notificação agrupada por aluno.
     * C6: Zerou pendências -> notificação removida.
     */
    private void sincronizarNotificacaoAluno(PersonalTrainer personal, Aluno aluno, int quantidadePendencias) {
        UUID usuarioPersonalId = personal.getUsuario().getId();
        Optional<Notificacao> notifOpt = notificacaoRepository.findByUsuarioIdAndTipoAndReferenciaId(
                usuarioPersonalId, TIPO_NOTIFICACAO_PENDENCIAS, aluno.getId()
        );

        String nomeAluno = aluno.getUsuario() != null ? aluno.getUsuario().getNome() : "Aluno";

        if (quantidadePendencias > 0) {
            String titulo = "Pendências de Métricas - " + nomeAluno;
            String mensagem = "O aluno " + nomeAluno + " possui " + quantidadePendencias + " métrica(s) pendente(s) de preenchimento.";

            if (notifOpt.isPresent()) {
                Notificacao notif = notifOpt.get();
                notif.setTitulo(titulo);
                notif.setMensagem(mensagem);
                notif.setDataCriacao(OffsetDateTime.now());
                notificacaoRepository.save(notif);
            } else {
                Notificacao nova = Notificacao.builder()
                        .usuario(personal.getUsuario())
                        .titulo(titulo)
                        .mensagem(mensagem)
                        .tipo(TIPO_NOTIFICACAO_PENDENCIAS)
                        .lida(false)
                        .referenciaId(aluno.getId())
                        .dataCriacao(OffsetDateTime.now())
                        .build();
                notificacaoRepository.save(nova);
            }
        } else {
            // US-24 C6: Zerou pendências -> notificação removida
            notifOpt.ifPresent(notificacaoRepository::delete);
        }
    }

    private List<RotinaTreinoResponseDTO.MetricaResumoDTO> extrairMetricasSessaoDTO(SessaoTreino sessao) {
        if (sessao == null || sessao.getMetricas() == null || sessao.getMetricas().isEmpty()) {
            return List.of();
        }
        boolean preservarDescontinuadas = sessao.getStatus() == StatusSessaoTreino.CONCLUIDA;
        return sessao.getMetricas().stream()
                .filter(m -> preservarDescontinuadas || Boolean.TRUE.equals(m.getAtivo()))
                .sorted(Comparator.comparing(Metrica::getTitulo, String.CASE_INSENSITIVE_ORDER))
                .map(m -> new RotinaTreinoResponseDTO.MetricaResumoDTO(
                        m.getId(),
                        m.getTitulo(),
                        m.getTipo(),
                        m.getObrigatoria(),
                        m.getQuemPreenche(),
                        m.getCampoStrava()
                ))
                .toList();
    }

    /**
     * Extrai as métricas pendentes do personal em uma sessão específica.
     * Métricas exclusivas do personal (PERSONAL) ou de ambos (AMBOS) que ainda não foram preenchidas no desempenho.
     */
    private List<MetricaPendenteDTO> extrairPendenciasSessao(SessaoTreino sessao) {
        if (sessao == null || sessao.getMetricas() == null || sessao.getMetricas().isEmpty()) {
            return List.of();
        }

        Set<Metrica> metricasSessao = sessao.getMetricas();
        Desempenho desempenho = sessao.getDesempenho();

        Set<UUID> metricasPreenchidas = new HashSet<>();
        if (desempenho != null && desempenho.getRegistrosMetricas() != null) {
            for (RegistroMetrica rm : desempenho.getRegistrosMetricas()) {
                if (rm.getMetrica() != null && rm.getValor() != null && !rm.getValor().isBlank()) {
                    metricasPreenchidas.add(rm.getMetrica().getId());
                }
            }
        }

        List<MetricaPendenteDTO> pendentes = new ArrayList<>();
        for (Metrica m : metricasSessao) {
            if (Boolean.FALSE.equals(m.getAtivo())) {
                continue;
            }
            // US-18 C5 / US-22 C5: Métricas exclusivas do personal
            boolean pendenciaPersonal = m.getQuemPreenche() == ResponsavelPreenchimento.PERSONAL
                    || (m.getQuemPreenche() == ResponsavelPreenchimento.AMBOS && Boolean.TRUE.equals(m.getObrigatoria()));

            if (pendenciaPersonal && !metricasPreenchidas.contains(m.getId())) {
                pendentes.add(new MetricaPendenteDTO(
                        sessao.getId(),
                        sessao.getSemana(),
                        sessao.getDataPlanejada(),
                        sessao.getDataReal(),
                        sessao.getStatus(),
                        sessao.getConcluidaAluno(),
                        m.getId(),
                        m.getTitulo(),
                        m.getTipo(),
                        m.getQuemPreenche(),
                        m.getObrigatoria()
                ));
            }
        }

        return pendentes;
    }

    private DesempenhoResponseDTO toDesempenhoResponseDTO(Desempenho d) {
        if (d == null) {
            return null;
        }

        List<RegistroMetricaResponseDTO> metricasDTO = d.getRegistrosMetricas() != null ? d.getRegistrosMetricas().stream()
                .map(rm -> new RegistroMetricaResponseDTO(
                        rm.getId(),
                        rm.getMetrica().getId(),
                        rm.getMetrica().getTitulo(),
                        rm.getMetrica().getTipo(),
                        rm.getValor()
                ))
                .toList() : List.of();

        return new DesempenhoResponseDTO(
                d.getId(),
                d.getDataRegistro(),
                d.getNivelCansaco(),
                d.getRegistradoPor(),
                d.getAluno() != null ? d.getAluno().getId() : null,
                d.getAluno() != null && d.getAluno().getUsuario() != null ? d.getAluno().getUsuario().getNome() : null,
                d.getObservacoes(),
                metricasDTO
        );
    }

    private NotificacaoResponseDTO toNotificacaoResponseDTO(Notificacao n) {
        return new NotificacaoResponseDTO(
                n.getId(),
                n.getTitulo(),
                n.getMensagem(),
                n.getTipo() != null ? n.getTipo().name() : null,
                n.getLida(),
                n.getReferenciaId(),
                n.getDataCriacao()
        );
    }

    private PersonalTrainer obterPersonalLogado(Usuario usuario) {
        return personalTrainerRepository.getByUsuario(usuario)
                .orElseThrow(() -> new NotFoundException("Personal Trainer não encontrado."));
    }
}
