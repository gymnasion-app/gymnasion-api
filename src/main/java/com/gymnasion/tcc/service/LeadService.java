package com.gymnasion.tcc.service;

import com.gymnasion.tcc.domain.Lead;
import com.gymnasion.tcc.dto.LeadRequestDTO;
import com.gymnasion.tcc.repository.LeadRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@AllArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;

    /**
     * Registra o interesse vindo da landing page. Se o e-mail já existir, atualiza os dados
     * em vez de duplicar (e sem revelar ao visitante que o e-mail já estava cadastrado).
     */
    @Transactional
    public void registrar(LeadRequestDTO dto) {
        String email = dto.email().toLowerCase().trim();
        String modalidade = dto.modalidade() == null || dto.modalidade().isBlank() ? null : dto.modalidade().trim();
        boolean aceitaNovidades = Boolean.TRUE.equals(dto.aceitaNovidades());

        Lead lead = leadRepository.findByEmail(email).orElseGet(() -> Lead.builder().email(email).build());
        lead.setNome(dto.nome().trim());
        lead.setModalidade(modalidade);
        lead.setAceitaNovidades(aceitaNovidades);
        lead.setDataAtualizacao(OffsetDateTime.now());
        leadRepository.save(lead);
    }
}
