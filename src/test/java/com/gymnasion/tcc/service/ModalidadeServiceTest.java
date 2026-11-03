package com.gymnasion.tcc.service;

import com.gymnasion.tcc.domain.Modalidade;
import com.gymnasion.tcc.dto.ModalidadeRequestDTO;
import com.gymnasion.tcc.dto.ModalidadeResponseDTO;
import com.gymnasion.tcc.exceptions.DuplicateResourceException;
import com.gymnasion.tcc.exceptions.NotFoundException;
import com.gymnasion.tcc.repository.ModalidadeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModalidadeServiceTest {

    @Mock
    private ModalidadeRepository modalidadeRepository;

    @InjectMocks
    private ModalidadeService modalidadeService;

    @Test
    @DisplayName("Listar apenas ativas com paginação")
    void listarApenasAtivasComPaginacao() {
        Pageable pageable = PageRequest.of(0, 10);
        Modalidade m = Modalidade.builder().id(1L).nome("Corrida").ativo(true).build();
        when(modalidadeRepository.findByAtivoTrue(pageable)).thenReturn(new PageImpl<>(List.of(m)));

        Page<ModalidadeResponseDTO> result = modalidadeService.listarTodas(pageable, true);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).nome()).isEqualTo("Corrida");
        assertThat(result.getContent().get(0).ativo()).isTrue();
        verify(modalidadeRepository).findByAtivoTrue(pageable);
    }

    @Test
    @DisplayName("Listar todas incluindo inativas quando apenasAtivas=false")
    void listarTodasIncluindoInativas() {
        Pageable pageable = PageRequest.of(0, 10);
        Modalidade m1 = Modalidade.builder().id(1L).nome("Corrida").ativo(true).build();
        Modalidade m2 = Modalidade.builder().id(2L).nome("Crossfit").ativo(false).build();
        when(modalidadeRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(m1, m2)));

        Page<ModalidadeResponseDTO> result = modalidadeService.listarTodas(pageable, false);

        assertThat(result.getContent()).hasSize(2);
        verify(modalidadeRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Buscar por ID com sucesso")
    void buscarPorIdComSucesso() {
        Modalidade m = Modalidade.builder().id(1L).nome("Natação").descricao("Treinos aquáticos").ativo(true).build();
        when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(m));

        ModalidadeResponseDTO result = modalidadeService.buscarPorId(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.nome()).isEqualTo("Natação");
        assertThat(result.ativo()).isTrue();
    }

    @Test
    @DisplayName("Buscar por ID inexistente lança NotFoundException")
    void buscarPorIdInexistenteLancaNotFound() {
        when(modalidadeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> modalidadeService.buscarPorId(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Modalidade não encontrada.");
    }

    @Test
    @DisplayName("Criar modalidade com sucesso")
    void criarModalidadeComSucesso() {
        ModalidadeRequestDTO dto = new ModalidadeRequestDTO("Ciclismo", "Treinos de bike");
        when(modalidadeRepository.existsByNomeIgnoreCase("Ciclismo")).thenReturn(false);
        when(modalidadeRepository.save(any(Modalidade.class))).thenAnswer(inv -> {
            Modalidade toSave = inv.getArgument(0);
            return Modalidade.builder()
                    .id(10L)
                    .nome(toSave.getNome())
                    .descricao(toSave.getDescricao())
                    .ativo(toSave.getAtivo())
                    .build();
        });

        ModalidadeResponseDTO result = modalidadeService.criar(dto);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.nome()).isEqualTo("Ciclismo");
        assertThat(result.ativo()).isTrue();
        verify(modalidadeRepository).save(any(Modalidade.class));
    }

    @Test
    @DisplayName("Criar modalidade duplicada lança DuplicateResourceException")
    void criarModalidadeDuplicadaLancaExcecao() {
        ModalidadeRequestDTO dto = new ModalidadeRequestDTO("Musculação", "Treinos de força");
        when(modalidadeRepository.existsByNomeIgnoreCase("Musculação")).thenReturn(true);

        assertThatThrownBy(() -> modalidadeService.criar(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Já existe uma modalidade cadastrada com esse nome.");

        verify(modalidadeRepository, never()).save(any(Modalidade.class));
    }

    @Test
    @DisplayName("Desativar modalidade existente altera ativo para false")
    void desativarModalidadeComSucesso() {
        Modalidade m = Modalidade.builder().id(1L).nome("Corrida").ativo(true).build();
        when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(m));
        when(modalidadeRepository.save(any(Modalidade.class))).thenAnswer(inv -> inv.getArgument(0));

        ModalidadeResponseDTO result = modalidadeService.desativar(1L);

        assertThat(result.ativo()).isFalse();
        verify(modalidadeRepository).save(m);
    }

    @Test
    @DisplayName("Ativar modalidade existente altera ativo para true")
    void ativarModalidadeComSucesso() {
        Modalidade m = Modalidade.builder().id(1L).nome("Corrida").ativo(false).build();
        when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(m));
        when(modalidadeRepository.save(any(Modalidade.class))).thenAnswer(inv -> inv.getArgument(0));

        ModalidadeResponseDTO result = modalidadeService.ativar(1L);

        assertThat(result.ativo()).isTrue();
        verify(modalidadeRepository).save(m);
    }

    @Test
    @DisplayName("Atualizar modalidade com sucesso")
    void atualizarModalidadeComSucesso() {
        Modalidade m = Modalidade.builder().id(1L).nome("Corrida").descricao("Antiga").ativo(true).build();
        ModalidadeRequestDTO dto = new ModalidadeRequestDTO("Corrida de Rua", "Nova descrição");
        when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(m));
        when(modalidadeRepository.existsByNomeIgnoreCaseAndIdNot("Corrida de Rua", 1L)).thenReturn(false);
        when(modalidadeRepository.save(any(Modalidade.class))).thenAnswer(inv -> inv.getArgument(0));

        ModalidadeResponseDTO result = modalidadeService.atualizar(1L, dto);

        assertThat(result.nome()).isEqualTo("Corrida de Rua");
        assertThat(result.descricao()).isEqualTo("Nova descrição");
    }

    @Test
    @DisplayName("Atualizar modalidade com nome duplicado lança DuplicateResourceException")
    void atualizarModalidadeNomeDuplicadoLancaExcecao() {
        Modalidade m = Modalidade.builder().id(1L).nome("Corrida").ativo(true).build();
        ModalidadeRequestDTO dto = new ModalidadeRequestDTO("Natação", "Descrição");
        when(modalidadeRepository.findById(1L)).thenReturn(Optional.of(m));
        when(modalidadeRepository.existsByNomeIgnoreCaseAndIdNot("Natação", 1L)).thenReturn(true);

        assertThatThrownBy(() -> modalidadeService.atualizar(1L, dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Já existe uma modalidade cadastrada com esse nome.");

        verify(modalidadeRepository, never()).save(any(Modalidade.class));
    }
}
