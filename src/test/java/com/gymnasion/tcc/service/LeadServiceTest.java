package com.gymnasion.tcc.service;

import com.gymnasion.tcc.domain.Lead;
import com.gymnasion.tcc.dto.LeadRequestDTO;
import com.gymnasion.tcc.repository.LeadRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @InjectMocks
    private LeadService leadService;

    @Test
    @DisplayName("Registra novo lead normalizando e-mail e modalidade vazia")
    void registraNovoLead() {
        when(leadRepository.findByEmail("ana@exemplo.com")).thenReturn(Optional.empty());

        leadService.registrar(new LeadRequestDTO("  Ana Souza ", " Ana@Exemplo.com ", "", null, "captcha-token"));

        ArgumentCaptor<Lead> captor = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository).save(captor.capture());
        Lead salvo = captor.getValue();
        assertThat(salvo.getNome()).isEqualTo("Ana Souza");
        assertThat(salvo.getEmail()).isEqualTo("ana@exemplo.com");
        assertThat(salvo.getModalidade()).isNull();
        assertThat(salvo.getAceitaNovidades()).isFalse();
    }

    @Test
    @DisplayName("E-mail já registrado atualiza o lead existente em vez de duplicar")
    void atualizaLeadExistente() {
        UUID id = UUID.randomUUID();
        Lead existente = Lead.builder().id(id).nome("Ana").email("ana@exemplo.com").build();
        when(leadRepository.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(existente));

        leadService.registrar(new LeadRequestDTO("Ana Souza", "ana@exemplo.com", "natacao", true, "captcha-token"));

        ArgumentCaptor<Lead> captor = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(id);
        assertThat(captor.getValue().getNome()).isEqualTo("Ana Souza");
        assertThat(captor.getValue().getModalidade()).isEqualTo("natacao");
        assertThat(captor.getValue().getAceitaNovidades()).isTrue();
    }
}
