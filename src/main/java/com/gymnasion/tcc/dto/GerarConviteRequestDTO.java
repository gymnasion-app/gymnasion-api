package com.gymnasion.tcc.dto;

import jakarta.validation.constraints.Email;

import java.util.List;
import java.util.Objects;

public record GerarConviteRequestDTO(
        Long modalidadeId,
        List<Long> modalidadesIds,

        @Email(message = "O email do aluno deve ser válido.")
        String emailAluno
) {
    public GerarConviteRequestDTO(Long modalidadeId, String emailAluno) {
        this(modalidadeId, null, emailAluno);
    }

    public List<Long> resolverModalidadesIds() {
        if (modalidadesIds != null && !modalidadesIds.isEmpty()) {
            return modalidadesIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }
        if (modalidadeId != null) {
            return List.of(modalidadeId);
        }
        return List.of();
    }
}
