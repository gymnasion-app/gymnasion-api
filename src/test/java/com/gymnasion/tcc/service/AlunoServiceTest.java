package com.gymnasion.tcc.service;

import com.gymnasion.tcc.domain.Aluno;
import com.gymnasion.tcc.domain.Modalidade;
import com.gymnasion.tcc.domain.Notificacao;
import com.gymnasion.tcc.domain.PersonalTrainer;
import com.gymnasion.tcc.domain.Usuario;
import com.gymnasion.tcc.domain.enums.StatusConvite;
import com.gymnasion.tcc.dto.AlunoResponseDTO;
import com.gymnasion.tcc.dto.AtualizarModalidadesAlunoRequestDTO;
import com.gymnasion.tcc.dto.MensagemResponseDTO;
import com.gymnasion.tcc.event.SolicitacaoAlunoAvaliadaEvent;
import com.gymnasion.tcc.event.SolicitacaoAlunoEmailListener;
import com.gymnasion.tcc.exceptions.BusinessException;
import com.gymnasion.tcc.repository.AlunoRepository;
import com.gymnasion.tcc.repository.ModalidadeRepository;
import com.gymnasion.tcc.repository.NotificacaoRepository;
import com.gymnasion.tcc.repository.PersonalTrainerRepository;
import com.gymnasion.tcc.repository.RotinaTreinoRepository;
import com.gymnasion.tcc.repository.TermoAceiteRepository;
import com.gymnasion.tcc.repository.TurmaRepository;
import com.gymnasion.tcc.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlunoServiceTest {

    @Mock
    private AlunoRepository alunoRepository;
    @Mock
    private PersonalTrainerRepository personalTrainerRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private TermoAceiteRepository termoAceiteRepository;
    @Mock
    private NotificacaoService notificacaoService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ModalidadeRepository modalidadeRepository;
    @Mock
    private RotinaTreinoRepository rotinaTreinoRepository;
    @Mock
    private TurmaRepository turmaRepository;

    @InjectMocks
    private AlunoService alunoService;

    private Usuario usuarioPersonal;
    private PersonalTrainer personal;
    private Aluno aluno;

    @BeforeEach
    void setUp() {
        usuarioPersonal = Usuario.builder().id(UUID.randomUUID()).nome("Carlos Personal").email("carlos@teste.com").build();
        personal = PersonalTrainer.builder().id(UUID.randomUUID()).usuario(usuarioPersonal).build();

        Usuario usuarioAluno = Usuario.builder().id(UUID.randomUUID()).nome("João Aluno").email("joao@teste.com").build();
        aluno = Aluno.builder()
                .id(UUID.randomUUID())
                .usuario(usuarioAluno)
                .personal(personal)
                .status(StatusConvite.PENDENTE)
                .modalidades(new HashSet<>())
                .build();

        lenient().when(personalTrainerRepository.getByUsuario(usuarioPersonal)).thenReturn(Optional.of(personal));
        lenient().when(alunoRepository.findById(aluno.getId())).thenReturn(Optional.of(aluno));
        lenient().when(alunoRepository.findByIdAndPersonalId(aluno.getId(), personal.getId())).thenReturn(Optional.of(aluno));
    }

    @Test
    @DisplayName("US-03 C3: aprovar aluno ativa, cria notificação e dispara e-mail automático")
    void aprovarAluno_disparaEmailENotificacao() {
        when(alunoRepository.save(any(Aluno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlunoResponseDTO response = alunoService.aprovarAluno(aluno.getId(), usuarioPersonal);

        assertThat(response.status()).isEqualTo(StatusConvite.ATIVO);

        verify(notificacaoService).notificarSolicitacaoAprovada(aluno, "Carlos Personal");
        verify(notificacaoService).resolverSolicitacao(eq(personal), eq(aluno.getId()), anyBoolean());

        ArgumentCaptor<SolicitacaoAlunoAvaliadaEvent> eventCaptor = ArgumentCaptor.forClass(SolicitacaoAlunoAvaliadaEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().aprovado()).isTrue();
        assertThat(eventCaptor.getValue().emailAluno()).isEqualTo("joao@teste.com");
        assertThat(eventCaptor.getValue().nomePersonal()).isEqualTo("Carlos Personal");
    }

    @Test
    @DisplayName("US-03 C4: recusar aluno remove registro, dispara e-mail e retorna mensagem do DRS")
    void recusarAluno_disparaEmailERetornaMensagem() {
        MensagemResponseDTO response = alunoService.recusarAluno(aluno.getId(), usuarioPersonal);

        assertThat(response.mensagem()).isEqualTo("Aluno João Aluno foi recusado com sucesso!");
        verify(alunoRepository).delete(aluno);
        verify(termoAceiteRepository).deleteByUsuario(aluno.getUsuario());
        verify(usuarioRepository).delete(aluno.getUsuario());
        verify(notificacaoService).resolverSolicitacao(eq(personal), eq(aluno.getId()), anyBoolean());

        ArgumentCaptor<SolicitacaoAlunoAvaliadaEvent> eventCaptor = ArgumentCaptor.forClass(SolicitacaoAlunoAvaliadaEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().aprovado()).isFalse();
        assertThat(eventCaptor.getValue().emailAluno()).isEqualTo("joao@teste.com");
    }

    @Test
    @DisplayName("Não dispara e-mail quando o aluno não está pendente")
    void aprovarAluno_naoPendente_naoDisparaEmail() {
        aluno.setStatus(StatusConvite.ATIVO);

        assertThatThrownBy(() -> alunoService.aprovarAluno(aluno.getId(), usuarioPersonal))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(eventPublisher, notificacaoService);
    }

    @Test
    @DisplayName("Listener envia o e-mail correto conforme aprovação/recusa")
    void listener_encaminhaParaEmailService() {
        EmailService emailService = mock(EmailService.class);
        SolicitacaoAlunoEmailListener listener = new SolicitacaoAlunoEmailListener(emailService);

        listener.onSolicitacaoAvaliada(new SolicitacaoAlunoAvaliadaEvent("a@a.com", "A", "P", true));
        listener.onSolicitacaoAvaliada(new SolicitacaoAlunoAvaliadaEvent("b@b.com", "B", "P", false));

        verify(emailService).notificarAprovacaoAluno("a@a.com", "P");
        verify(emailService).notificarRecusaAluno("b@b.com", "P");
    }

    @Test
    @DisplayName("Paginação: getAlunosVinculados com Pageable retorna página correta")
    void getAlunosVinculados_comPaginacao() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 5);
        org.springframework.data.domain.Page<Aluno> pageMock = new org.springframework.data.domain.PageImpl<>(List.of(aluno), pageable, 1);
        when(alunoRepository.findByPersonalId(personal.getId(), pageable)).thenReturn(pageMock);

        org.springframework.data.domain.Page<AlunoResponseDTO> resultado = alunoService.getAlunosVinculados(usuarioPersonal, pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).id()).isEqualTo(aluno.getId());
    }

    @Test
    @DisplayName("Paginação: getAlunosPendentes com Pageable retorna página correta")
    void getAlunosPendentes_comPaginacao() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 5);
        org.springframework.data.domain.Page<Aluno> pageMock = new org.springframework.data.domain.PageImpl<>(List.of(aluno), pageable, 1);
        when(alunoRepository.findByPersonalIdAndStatus(personal.getId(), StatusConvite.PENDENTE, pageable)).thenReturn(pageMock);

        org.springframework.data.domain.Page<AlunoResponseDTO> resultado = alunoService.getAlunosPendentes(usuarioPersonal, pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Paginação: getAlunosInativos com Pageable retorna página correta")
    void getAlunosInativos_comPaginacao() {
        aluno.setStatus(StatusConvite.INATIVO);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 5);
        org.springframework.data.domain.Page<Aluno> pageMock = new org.springframework.data.domain.PageImpl<>(List.of(aluno), pageable, 1);
        when(alunoRepository.findByPersonalIdAndStatus(personal.getId(), StatusConvite.INATIVO, pageable)).thenReturn(pageMock);

        org.springframework.data.domain.Page<AlunoResponseDTO> resultado = alunoService.getAlunosInativos(usuarioPersonal, pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).status()).isEqualTo(StatusConvite.INATIVO);
    }

    @Test
    @DisplayName("obterMeuPerfil: Retorna os dados do próprio aluno autenticado com sucesso")
    void obterMeuPerfil_comSucesso() {
        Usuario usuarioAluno = aluno.getUsuario();
        when(alunoRepository.findByUsuario(usuarioAluno)).thenReturn(Optional.of(aluno));

        AlunoResponseDTO resultado = alunoService.obterMeuPerfil(usuarioAluno);

        assertThat(resultado).isNotNull();
        assertThat(resultado.id()).isEqualTo(aluno.getId());
        assertThat(resultado.nomePersonal()).isEqualTo(usuarioPersonal.getNome());
        assertThat(resultado.usuario().email()).isEqualTo(usuarioAluno.getEmail());
    }

    @Test
    @DisplayName("atualizarModalidades: adiciona segunda e terceira modalidade com sucesso")
    void atualizarModalidades_adicionaSegundaETerceiraModalidade_sucesso() {
        Modalidade natacao = Modalidade.builder().id(1L).nome("Natação").ativo(true).build();
        Modalidade musculacao = Modalidade.builder().id(2L).nome("Musculação").ativo(true).build();
        Modalidade corrida = Modalidade.builder().id(3L).nome("Corrida").ativo(true).build();
        aluno.setModalidades(new HashSet<>(java.util.Set.of(natacao)));

        when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(natacao));
        when(modalidadeRepository.findById(2L)).thenReturn(Optional.of(musculacao));
        when(modalidadeRepository.findById(3L)).thenReturn(Optional.of(corrida));
        when(alunoRepository.save(any(Aluno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlunoResponseDTO resultado = alunoService.atualizarModalidades(
                aluno.getId(),
                new AtualizarModalidadesAlunoRequestDTO(List.of(1L, 2L, 3L)),
                usuarioPersonal
        );

        assertThat(resultado).isNotNull();
        assertThat(aluno.getModalidades())
                .extracting(Modalidade::getId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("atualizarModalidades: rejeita mais de 3 modalidades")
    void atualizarModalidades_maisDeTresModalidades_lancaExcecao() {
        assertThatThrownBy(() -> alunoService.atualizarModalidades(
                aluno.getId(),
                new AtualizarModalidadesAlunoRequestDTO(List.of(1L, 2L, 3L, 4L)),
                usuarioPersonal
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O aluno pode ter no máximo 3 modalidades vinculadas.");
    }

    @Test
    @DisplayName("atualizarModalidades: bloqueia remoção de modalidade com rotina ativa")
    void atualizarModalidades_removerModalidadeComRotinaAtiva_lancaBusinessException() {
        Modalidade natacao = Modalidade.builder().id(1L).nome("Natação").ativo(true).build();
        Modalidade musculacao = Modalidade.builder().id(2L).nome("Musculação").ativo(true).build();
        aluno.setModalidades(new HashSet<>(java.util.Set.of(natacao, musculacao)));

        when(modalidadeRepository.findById(2L)).thenReturn(Optional.of(musculacao));
        when(rotinaTreinoRepository.existsByAlunoIdAndModalidadeIdAndAtivoTrue(aluno.getId(), 1L)).thenReturn(true);

        assertThatThrownBy(() -> alunoService.atualizarModalidades(
                aluno.getId(),
                new AtualizarModalidadesAlunoRequestDTO(List.of(2L)),
                usuarioPersonal
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível remover a modalidade 'Natação' pois o aluno possui uma rotina ativa vinculada a ela.");
    }

    @Test
    @DisplayName("atualizarModalidades: bloqueia remoção de modalidade com turma ativa")
    void atualizarModalidades_removerModalidadeComTurmaAtiva_lancaBusinessException() {
        Modalidade natacao = Modalidade.builder().id(1L).nome("Natação").ativo(true).build();
        Modalidade musculacao = Modalidade.builder().id(2L).nome("Musculação").ativo(true).build();
        aluno.setModalidades(new HashSet<>(java.util.Set.of(natacao, musculacao)));

        when(modalidadeRepository.findById(2L)).thenReturn(Optional.of(musculacao));
        when(rotinaTreinoRepository.existsByAlunoIdAndModalidadeIdAndAtivoTrue(aluno.getId(), 1L)).thenReturn(false);
        when(turmaRepository.existsByPersonalIdAndModalidadeIdAndAlunosId(
                personal.getId(), 1L, aluno.getId())).thenReturn(true);

        assertThatThrownBy(() -> alunoService.atualizarModalidades(
                aluno.getId(),
                new AtualizarModalidadesAlunoRequestDTO(List.of(2L)),
                usuarioPersonal
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Não é possível remover a modalidade 'Natação' pois o aluno está vinculado a uma turma desta modalidade.");
    }

    @Test
    @DisplayName("atualizarModalidades: permite remover modalidade quando não há rotina ativa nem turma")
    void atualizarModalidades_removerModalidadeApenasComRotinaEncerrada_sucesso() {
        Modalidade natacao = Modalidade.builder().id(1L).nome("Natação").ativo(true).build();
        Modalidade musculacao = Modalidade.builder().id(2L).nome("Musculação").ativo(true).build();
        aluno.setModalidades(new HashSet<>(java.util.Set.of(natacao, musculacao)));

        when(modalidadeRepository.findById(2L)).thenReturn(Optional.of(musculacao));
        when(rotinaTreinoRepository.existsByAlunoIdAndModalidadeIdAndAtivoTrue(aluno.getId(), 1L)).thenReturn(false);
        when(turmaRepository.existsByPersonalIdAndModalidadeIdAndAlunosId(personal.getId(), 1L, aluno.getId())).thenReturn(false);
        when(alunoRepository.save(any(Aluno.class))).thenAnswer(inv -> inv.getArgument(0));

        AlunoResponseDTO resultado = alunoService.atualizarModalidades(
                aluno.getId(),
                new AtualizarModalidadesAlunoRequestDTO(List.of(2L)),
                usuarioPersonal
        );

        assertThat(resultado).isNotNull();
        assertThat(aluno.getModalidades())
                .extracting(Modalidade::getId)
                .containsExactly(2L);
    }
}

