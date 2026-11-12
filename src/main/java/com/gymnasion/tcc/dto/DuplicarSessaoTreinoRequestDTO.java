package com.gymnasion.tcc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DuplicarSessaoTreinoRequestDTO(
        @NotNull(message = "A semana de destino é obrigatória.")
        @Min(value = 1, message = "O número da semana deve estar entre 1 e 104.")
        @Max(value = 104, message = "O número da semana deve estar entre 1 e 104.")
        @Schema(description = "Número da semana de destino (1..104)", example = "2")
        Integer semana,

        @NotNull(message = "A data planejada é obrigatória.")
        @Schema(description = "Data planejada da nova sessão duplicada", example = "2026-10-27")
        LocalDate dataPlanejada
) {}
