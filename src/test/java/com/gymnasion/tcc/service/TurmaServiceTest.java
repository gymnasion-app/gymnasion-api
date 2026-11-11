package com.gymnasion.tcc.service;

import com.gymnasion.tcc.domain.*;
import com.gymnasion.tcc.domain.enums.Role;
import com.gymnasion.tcc.domain.enums.StatusConvite;
import com.gymnasion.tcc.dto.CriarTurmaRequestDTO;
import com.gymnasion.tcc.dto.MensagemResponseDTO;
import com.gymnasion.tcc.dto.TurmaResponseDTO;
import com.gymnasion.tcc.exceptions.BusinessException;
import com.gymnasion.tcc.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurmaServiceTest {

    @Mock
    private TurmaRepository turmaRepository;
    @Mock
    private PersonalTrainerRepository personalTrainerRepository;
    @Mock
    private ModalidadeRepository modalidadeRepository;
    @Mock
    private AlunoRepository alunoRepository;
    @Mock
    private RotinaTreinoRepository rotinaTreinoRepository;
    @Mock
    private AcompanhamentoTreinoService acompanhamentoTreinoService;

    @InjectMocks
    private TurmaService turmaService;

    private Usuario usuarioPersonal;
    private PersonalTrainer personal;
    private Modalidade natacao;
    private Modalidade musculacao;
    private Aluno alunoComNatacao;
    private Aluno alunoSemNatacao;

    @BeforeEach
    void setUp() {
        usuarioPersonal = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Treinador Carlos")
                .email("carlos@gymnasion.com")
                .role(Role.PERSONAL_TRAINER)
                .build();

        personal = PersonalTrainer.builder()
                .id(UUID.randomUUID())
                .usuario(usuarioPersonal)
                .build();

        natacao = Modalidade.builder().id(1L).nome("Natação").ativo(true).build();
        musculacao = Modalidade.builder().id(2L).nome("Musculação").ativo(true).build();

        alunoComNatacao = Aluno.builder()
                .id(UUID.randomUUID())
                .usuario(Usuario.builder().id(UUID.randomUUID()).nome("Ana Híbrida").build())
                .personal(personal)
                .status(StatusConvite.ATIVO)
                .modalidades(new HashSet<>(Set.of(natacao, musculacao)))
                .build();

        alunoSemNatacao = Aluno.builder()
                .id(UUID.randomUUID())
                .usuario(Usuario.builder().id(UUID.randomUUID()).nome("Bruno Musculação").build())
                .personal(personal)
                .status(StatusConvite.ATIVO)
                .modalidades(new HashSet<>(Set.of(musculacao)))
                .build();
    }

    @Test
    @DisplayName("criarTurma: permite aluno vinculado à modalidade da turma")
    void criarTurma_alunoPossuiModalidade_sucesso() {
        when(personalTrainerRepository.getByUsuario(usuarioPersonal)).thenReturn(Optional.of(personal));
        when(alunoRepository.findByPersonalId(personal.getId())).thenReturn(List.of(alunoComNatacao));
        when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(natacao));
        when(alunoRepository.findByIdAndPersonalId(alunoComNatacao.getId(), personal.getId()))
                .thenReturn(Optional.of(alunoComNatacao));
        when(turmaRepository.save(any(Turma.class))).thenAnswer(inv -> {
            Turma t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        CriarTurmaRequestDTO dto = new CriarTurmaRequestDTO(
                "Turma Natação Manhã", 1L, OffsetDateTime.now(), Set.of(alunoComNatacao.getId())
        );

        TurmaResponseDTO response = turmaService.criarTurma(usuarioPersonal, dto);

        assertThat(response.nome()).isEqualTo("Turma Natação Manhã");
        assertThat(response.quantidadeAlunos()).isEqualTo(1);
    }

    @Test
    @DisplayName("criarTurma: bloqueia aluno que não possui a modalidade vinculada ao perfil")
    void criarTurma_alunoSemModalidade_lancaBusinessException() {
        when(personalTrainerRepository.getByUsuario(usuarioPersonal)).thenReturn(Optional.of(personal));
        when(alunoRepository.findByPersonalId(personal.getId())).thenReturn(List.of(alunoSemNatacao));
        when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(natacao));
        when(alunoRepository.findByIdAndPersonalId(alunoSemNatacao.getId(), personal.getId()))
                .thenReturn(Optional.of(alunoSemNatacao));

        CriarTurmaRequestDTO dto = new CriarTurmaRequestDTO(
                "Turma Natação Manhã", 1L, OffsetDateTime.now(), Set.of(alunoSemNatacao.getId())
        );

        assertThatThrownBy(() -> turmaService.criarTurma(usuarioPersonal, dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O aluno Bruno Musculação não possui a modalidade 'Natação' vinculada ao seu perfil.");

        verify(turmaRepository, never()).save(any());
    }

    @Test
    @DisplayName("adicionarAluno: bloqueia entrada em turma se o aluno não possuir a modalidade no perfil")
    void adicionarAluno_alunoSemModalidade_lancaBusinessException() {
        Turma turmaNatacao = Turma.builder()
                .id(UUID.randomUUID())
                .nome("Turma Natação")
                .personal(personal)
                .modalidade(natacao)
                .alunos(new HashSet<>(Set.of(alunoComNatacao)))
                .build();

        when(personalTrainerRepository.getByUsuario(usuarioPersonal)).thenReturn(Optional.of(personal));
        when(turmaRepository.findByIdAndPersonalId(turmaNatacao.getId(), personal.getId()))
                .thenReturn(Optional.of(turmaNatacao));
        when(alunoRepository.findByIdAndPersonalId(alunoSemNatacao.getId(), personal.getId()))
                .thenReturn(Optional.of(alunoSemNatacao));

        assertThatThrownBy(() -> turmaService.adicionarAluno(usuarioPersonal, turmaNatacao.getId(), alunoSemNatacao.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O aluno Bruno Musculação não possui a modalidade 'Natação' vinculada ao seu perfil.");

        verify(turmaRepository, never()).save(any());
    }

    @Test
    @DisplayName("adicionarAluno: adiciona aluno com modalidade compatível com sucesso")
    void adicionarAluno_alunoComModalidade_sucesso() {
        Turma turmaMusculacao = Turma.builder()
                .id(UUID.randomUUID())
                .nome("Turma Força")
                .personal(personal)
                .modalidade(musculacao)
                .alunos(new HashSet<>(Set.of(alunoSemNatacao)))
                .build();

        when(personalTrainerRepository.getByUsuario(usuarioPersonal)).thenReturn(Optional.of(personal));
        when(turmaRepository.findByIdAndPersonalId(turmaMusculacao.getId(), personal.getId()))
                .thenReturn(Optional.of(turmaMusculacao));
        when(alunoRepository.findByIdAndPersonalId(alunoComNatacao.getId(), personal.getId()))
                .thenReturn(Optional.of(alunoComNatacao));
        when(turmaRepository.save(any(Turma.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rotinaTreinoRepository.existsByAlunoIdAndModalidadeIdAndAtivoTrue(alunoComNatacao.getId(), 2L))
                .thenReturn(false);
        when(rotinaTreinoRepository.findByPersonalTrainerIdAndModalidadeId(personal.getId(), 2L))
                .thenReturn(List.of());

        MensagemResponseDTO response = turmaService.adicionarAluno(usuarioPersonal, turmaMusculacao.getId(), alunoComNatacao.getId());

        assertThat(response.mensagem()).isEqualTo("Aluno adicionado com sucesso");
        assertThat(turmaMusculacao.getAlunos()).contains(alunoComNatacao);
    }
}
