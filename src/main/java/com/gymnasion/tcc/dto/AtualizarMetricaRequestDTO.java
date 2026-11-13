package com.gymnasion.tcc.dto;

import com.gymnasion.tcc.domain.enums.CampoStrava;
import com.gymnasion.tcc.domain.enums.ResponsavelPreenchimento;
import com.gymnasion.tcc.domain.enums.TipoMetrica;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarMetricaRequestDTO(
        @NotBlank(message = "O título é obrigatório.")
        @Size(max = 100, message = "O título deve ter no máximo 100 caracteres.")
        String titulo,

        TipoMetrica tipo,

        Boolean obrigatoria,

        ResponsavelPreenchimento quemPreenche,

        CampoStrava campoStrava,

        @Size(max = 20, message = "A unidade deve ter no máximo 20 caracteres.")
        String unidade
) {
    public AtualizarMetricaRequestDTO(String titulo, TipoMetrica tipo, Boolean obrigatoria, ResponsavelPreenchimento quemPreenche) {
        this(titulo, tipo, obrigatoria, quemPreenche, null, null);
    }

    public AtualizarMetricaRequestDTO(String titulo, TipoMetrica tipo, Boolean obrigatoria, ResponsavelPreenchimento quemPreenche, CampoStrava campoStrava) {
        this(titulo, tipo, obrigatoria, quemPreenche, campoStrava, null);
    }
}
