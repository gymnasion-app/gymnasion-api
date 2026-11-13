package com.gymnasion.tcc.service;

import com.gymnasion.tcc.domain.Aluno;
import com.gymnasion.tcc.domain.Metrica;
import com.gymnasion.tcc.domain.Modalidade;
import com.gymnasion.tcc.domain.PersonalTrainer;
import com.gymnasion.tcc.domain.Usuario;
import com.gymnasion.tcc.domain.enums.CampoStrava;
import com.gymnasion.tcc.domain.enums.StatusSessaoTreino;
import com.gymnasion.tcc.domain.enums.TipoMetrica;
import com.gymnasion.tcc.dto.AtualizarMetricaRequestDTO;
import com.gymnasion.tcc.dto.CriarMetricaRequestDTO;
import com.gymnasion.tcc.dto.MensagemResponseDTO;
import com.gymnasion.tcc.dto.MetricaResponseDTO;
import com.gymnasion.tcc.exceptions.BusinessException;
import com.gymnasion.tcc.exceptions.DuplicateResourceException;
import com.gymnasion.tcc.exceptions.NotFoundException;
import com.gymnasion.tcc.repository.AuditoriaDesempenhoRepository;
import com.gymnasion.tcc.repository.MetricaRepository;
import com.gymnasion.tcc.repository.ModalidadeRepository;
import com.gymnasion.tcc.repository.PersonalTrainerRepository;
import com.gymnasion.tcc.repository.RegistroMetricaRepository;
import com.gymnasion.tcc.repository.SessaoTreinoRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@AllArgsConstructor
public class MetricaService {

    private final MetricaRepository metricaRepository;
    private final ModalidadeRepository modalidadeRepository;
    private final PersonalTrainerRepository personalTrainerRepository;
    private final RegistroMetricaRepository registroMetricaRepository;
    private final SessaoTreinoRepository sessaoTreinoRepository;
    private final AuditoriaDesempenhoRepository auditoriaDesempenhoRepository;
    private final AcompanhamentoTreinoService acompanhamentoTreinoService;

    @Transactional
    public MetricaResponseDTO criarMetrica(Usuario usuario, CriarMetricaRequestDTO dto) {
        PersonalTrainer personal = obterPersonalLogado(usuario);

        boolean existeDuplicado = metricaRepository.existsByPersonalTrainerIdAndModalidadeIdAndTituloIgnoreCaseAndAtivoTrue(
                personal.getId(), dto.modalidadeId(), dto.titulo().trim()
        );

        if (existeDuplicado) {
            throw new DuplicateResourceException("Já existe uma métrica com esse nome nessa modalidade.");
        }

        Modalidade modalidade = modalidadeRepository.findById(dto.modalidadeId())
                .orElseThrow(() -> new NotFoundException("Modalidade não encontrada."));

        if (Boolean.FALSE.equals(modalidade.getAtivo())) {
            throw new BusinessException("Não é possível cadastrar métricas em uma modalidade desativada.");
        }

        if (dto.campoStrava() != null && !dto.campoStrava().isCompativelCom(dto.tipo())) {
            throw new BusinessException("O campo do Strava selecionado não é compatível com o tipo de métrica.");
        }

        String unidadeNormalizada = normalizarUnidade(dto.tipo(), dto.unidade(), dto.campoStrava(), modalidade.getNome());
        CampoStrava campoStravaPersistido = dto.campoStrava() == CampoStrava.NENHUM ? null : dto.campoStrava();
        if (campoStravaPersistido != null && !campoStravaPersistido.isCompativelCom(dto.tipo(), unidadeNormalizada)) {
            throw new BusinessException("Campo Strava incompatível com o tipo/unidade da métrica.");
        }

        Metrica metrica = Metrica.builder()
                .titulo(dto.titulo().trim())
                .tipo(dto.tipo())
                .unidade(unidadeNormalizada)
                .obrigatoria(dto.obrigatoria())
                .quemPreenche(dto.quemPreenche())
                .campoStrava(campoStravaPersistido)
                .modalidade(modalidade)
                .personalTrainer(personal)
                .ativo(true)
                .build();

        metrica = metricaRepository.save(metrica);

        return toResponseDTO(metrica, 0L, false);
    }

    @Transactional(readOnly = true)
    public Page<MetricaResponseDTO> listarMetricasPorModalidade(Usuario usuario, Long modalidadeId, Pageable pageable) {
        PersonalTrainer personal = obterPersonalLogado(usuario);

        Page<Metrica> page = metricaRepository.findByPersonalTrainerIdAndModalidadeId(personal.getId(), modalidadeId, pageable);
        Set<UUID> idsComHistorico = buscarIdsComHistorico(page.getContent());
        return page.map(metrica -> {
            Long qtdAlunos = registroMetricaRepository.countAlunosComRegistrosByMetricaId(metrica.getId());
            return toResponseDTO(metrica, qtdAlunos, idsComHistorico.contains(metrica.getId()));
        });
    }

    @Transactional(readOnly = true)
    public List<MetricaResponseDTO> listarMetricasPorModalidade(Usuario usuario, Long modalidadeId) {
        PersonalTrainer personal = obterPersonalLogado(usuario);

        List<Metrica> metricas = metricaRepository.findByPersonalTrainerIdAndModalidadeId(personal.getId(), modalidadeId);
        Set<UUID> idsComHistorico = buscarIdsComHistorico(metricas);
        return metricas.stream()
                .map(metrica -> {
                    Long qtdAlunos = registroMetricaRepository.countAlunosComRegistrosByMetricaId(metrica.getId());
                    return toResponseDTO(metrica, qtdAlunos, idsComHistorico.contains(metrica.getId()));
                })
                .toList();
    }

    @Transactional
    public MetricaResponseDTO atualizarMetrica(UUID metricaId, Usuario usuario, AtualizarMetricaRequestDTO dto) {
        PersonalTrainer personal = obterPersonalLogado(usuario);

        Metrica metrica = metricaRepository.findByIdAndPersonalTrainerId(metricaId, personal.getId())
                .orElseThrow(() -> new NotFoundException("Métrica não encontrada ou não pertence a este Personal Trainer."));

        boolean possuiHistorico = registroMetricaRepository.existsByMetricaId(metricaId)
                || sessaoTreinoRepository.existsByMetricaIdAndStatus(metricaId, StatusSessaoTreino.CONCLUIDA)
                || auditoriaDesempenhoRepository.existsByMetricaId(metricaId);

        TipoMetrica tipoAnterior = metrica.getTipo();
        TipoMetrica tipoFinal = dto.tipo() != null ? dto.tipo() : tipoAnterior;
        boolean tipoAlterado = !Objects.equals(tipoFinal, tipoAnterior);
        if (tipoAlterado) {
            if (possuiHistorico) {
                throw new BusinessException("Não é possível alterar o tipo de uma métrica com histórico de registros. Crie uma nova métrica.");
            }
            metrica.setTipo(tipoFinal);
        }

        String unidadeAtual = metrica.getUnidade();
        String unidadeFinal;
        if (dto.unidade() != null) {
            String trimmed = dto.unidade().trim();
            if (trimmed.isEmpty()) {
                if (possuiHistorico && unidadeAtual != null) {
                    throw new BusinessException("Métrica com histórico não permite alterar tipo ou unidade.");
                }
                if (tipoFinal == TipoMetrica.TEMPO || tipoFinal == TipoMetrica.FREQUENCIA_CARDIACA) {
                    throw new BusinessException("Este tipo de métrica exige uma unidade de medida.");
                }
                unidadeFinal = null;
            } else {
                if (possuiHistorico && !Objects.equals(trimmed, unidadeAtual)) {
                    throw new BusinessException("Métrica com histórico não permite alterar tipo ou unidade.");
                }
                unidadeFinal = normalizarUnidade(tipoFinal, trimmed, dto.campoStrava(), metrica.getModalidade().getNome());
            }
        } else if (tipoAlterado || !CampoStrava.isUnidadeValida(tipoFinal, unidadeAtual)) {
            String candidato = CampoStrava.isUnidadeValida(tipoFinal, unidadeAtual) ? unidadeAtual : null;
            unidadeFinal = normalizarUnidade(tipoFinal, candidato, dto.campoStrava(), metrica.getModalidade().getNome());
        } else {
            unidadeFinal = unidadeAtual;
        }
        metrica.setUnidade(unidadeFinal);

        boolean pendenciaAlterada = (dto.obrigatoria() != null && !dto.obrigatoria().equals(metrica.getObrigatoria()))
                || (dto.quemPreenche() != null && dto.quemPreenche() != metrica.getQuemPreenche());

        if (dto.obrigatoria() != null) {
            metrica.setObrigatoria(dto.obrigatoria());
        }

        if (dto.quemPreenche() != null) {
            metrica.setQuemPreenche(dto.quemPreenche());
        }

        CampoStrava campoStravaFinal;
        if (dto.campoStrava() == CampoStrava.NENHUM) {
            campoStravaFinal = null;
        } else if (dto.campoStrava() != null) {
            if (!dto.campoStrava().isCompativelCom(tipoFinal, unidadeFinal)) {
                throw new BusinessException("Campo Strava incompatível com o tipo/unidade da métrica.");
            }
            campoStravaFinal = dto.campoStrava();
        } else {
            campoStravaFinal = metrica.getCampoStrava() == CampoStrava.NENHUM ? null : metrica.getCampoStrava();
            if (campoStravaFinal != null && !campoStravaFinal.isCompativelCom(tipoFinal, unidadeFinal)) {
                throw new BusinessException("Campo Strava incompatível com o tipo/unidade da métrica.");
            }
        }
        metrica.setCampoStrava(campoStravaFinal);

        if (!metrica.getTitulo().equalsIgnoreCase(dto.titulo().trim())) {
            boolean tituloEmUso = metricaRepository.existsByPersonalTrainerIdAndModalidadeIdAndTituloIgnoreCaseAndIdNotAndAtivoTrue(
                    personal.getId(), metrica.getModalidade().getId(), dto.titulo().trim(), metricaId
            );

            if (tituloEmUso) {
                throw new DuplicateResourceException("Já existe uma métrica com esse nome nessa modalidade.");
            }
            metrica.setTitulo(dto.titulo().trim());
        }

        metrica = metricaRepository.save(metrica);

        if (pendenciaAlterada) {
            sincronizarNotificacoes(sessaoTreinoRepository.findAlunosByMetricaId(metricaId));
        }

        Long qtdAlunos = registroMetricaRepository.countAlunosComRegistrosByMetricaId(metricaId);
        return toResponseDTO(metrica, qtdAlunos, possuiHistorico);
    }

    @Transactional
    public MensagemResponseDTO excluirMetrica(UUID metricaId, Usuario usuario) {
        PersonalTrainer personal = obterPersonalLogado(usuario);

        Metrica metrica = metricaRepository.findByIdAndPersonalTrainerId(metricaId, personal.getId())
                .orElseThrow(() -> new NotFoundException("Métrica não encontrada ou não pertence a este Personal Trainer."));

        boolean possuiHistorico = registroMetricaRepository.existsByMetricaId(metricaId)
                || sessaoTreinoRepository.existsByMetricaIdAndStatus(metricaId, StatusSessaoTreino.CONCLUIDA)
                || auditoriaDesempenhoRepository.existsByMetricaId(metricaId);

        List<Aluno> alunosImpactados = sessaoTreinoRepository.findAlunosByMetricaIdAndStatus(metricaId, StatusSessaoTreino.PLANEJADA);
        sessaoTreinoRepository.removerMetricaDeSessoesPlanejadas(metricaId);

        MensagemResponseDTO response;
        if (possuiHistorico) {
            metrica.setAtivo(false);
            metricaRepository.save(metrica);
            response = new MensagemResponseDTO("Métrica descontinuada com sucesso. O histórico foi mantido.");
        } else {
            metricaRepository.delete(metrica);
            response = new MensagemResponseDTO("Métrica excluída com sucesso!");
        }

        sincronizarNotificacoes(alunosImpactados);
        return response;
    }

    private void sincronizarNotificacoes(List<Aluno> alunosImpactados) {
        if (acompanhamentoTreinoService != null && alunosImpactados != null) {
            for (Aluno aluno : alunosImpactados) {
                acompanhamentoTreinoService.sincronizarNotificacaoParaAluno(aluno);
            }
        }
    }

    private PersonalTrainer obterPersonalLogado(Usuario usuario) {
        return personalTrainerRepository.getByUsuario(usuario)
                .orElseThrow(() -> new NotFoundException("Personal Trainer não encontrado."));
    }

    private Set<UUID> buscarIdsComHistorico(List<Metrica> metricas) {
        if (metricas == null || metricas.isEmpty()) {
            return Set.of();
        }
        List<UUID> ids = metricas.stream()
                .map(Metrica::getId)
                .filter(Objects::nonNull)
                .toList();
        if (ids.isEmpty()) {
            return Set.of();
        }
        Set<UUID> comHistorico = new java.util.HashSet<>();
        Set<UUID> comRegistros = registroMetricaRepository.findMetricaIdsWithRegistros(ids);
        if (comRegistros != null) {
            comHistorico.addAll(comRegistros);
        }
        Set<UUID> concluidas = sessaoTreinoRepository.findMetricaIdsByStatus(ids, StatusSessaoTreino.CONCLUIDA);
        if (concluidas != null) {
            comHistorico.addAll(concluidas);
        }
        return comHistorico;
    }

    private String normalizarUnidade(TipoMetrica tipo, String rawUnidade, CampoStrava campoStrava, String nomeModalidade) {
        String trimmed = rawUnidade == null ? null : rawUnidade.trim();
        if (trimmed != null && trimmed.isEmpty()) {
            trimmed = null;
        }
        return switch (tipo) {
            case TEXTO -> {
                if (trimmed != null) {
                    throw new BusinessException("Tipo de métrica TEXTO não aceita unidade de medida.");
                }
                yield null;
            }
            case FREQUENCIA_CARDIACA -> {
                if (trimmed == null) {
                    yield CampoStrava.UNIDADE_BPM;
                }
                if (!CampoStrava.UNIDADE_BPM.equals(trimmed)) {
                    throw new BusinessException("Unidade inválida para métrica de frequência cardíaca. Utilize 'bpm'.");
                }
                yield CampoStrava.UNIDADE_BPM;
            }
            case TEMPO -> {
                if (trimmed == null) {
                    if (campoStrava == CampoStrava.RITMO_MEDIO) {
                        CampoStrava.TipoEUnidade sugestao = CampoStrava.sugerirTipoEUnidade(campoStrava, nomeModalidade);
                        yield sugestao != null ? sugestao.unidade() : "min/km";
                    }
                    yield "mm:ss";
                }
                if (!CampoStrava.isUnidadeValida(TipoMetrica.TEMPO, trimmed)) {
                    throw new BusinessException("Unidade inválida para métrica de tempo.");
                }
                yield trimmed;
            }
            case NUMERICO -> {
                if (trimmed == null) {
                    yield null;
                }
                if (!CampoStrava.isUnidadeValida(TipoMetrica.NUMERICO, trimmed)) {
                    throw new BusinessException("Unidade inválida para métrica numérica.");
                }
                yield trimmed;
            }
        };
    }

    private MetricaResponseDTO toResponseDTO(Metrica metrica, Long qtdAlunos, boolean possuiHistorico) {
        CampoStrava campoStrava = metrica.getCampoStrava() == CampoStrava.NENHUM ? null : metrica.getCampoStrava();
        return new MetricaResponseDTO(
                metrica.getId(),
                metrica.getTitulo(),
                metrica.getTipo(),
                metrica.getUnidade(),
                metrica.getModalidade().getId(),
                metrica.getModalidade().getNome(),
                metrica.getObrigatoria(),
                metrica.getQuemPreenche(),
                campoStrava,
                metrica.getAtivo(),
                qtdAlunos,
                possuiHistorico
        );
    }
}