package com.gymnasion.tcc.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AtualizarModalidadesAlunoRequestDTO(
        @NotEmpty(message = "Selecione pelo menos uma modalidade esportiva.")
        @Size(min = 1, max = 3, message = "O aluno pode ter no máximo 3 modalidades vinculadas.")
        List<Long> modalidadesIds
) {}
