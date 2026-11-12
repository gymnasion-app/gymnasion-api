package com.gymnasion.tcc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record AtualizarSessaoTreinoRequestDTO(
        @NotNull(message = "A semana é obrigatória.")
        @Min(value = 1, message = "O número da semana deve estar entre 1 e 104.")
        @Max(value = 104, message = "O número da semana deve estar entre 1 e 104.")
        @Schema(description = "Número da semana no planejamento", example = "2")
        Integer semana,

        @NotNull(message = "A data planejada é obrigatória.")
        @Schema(description = "Nova data planejada da sessão com status PLANEJADA", example = "2026-10-25")
        LocalDate dataPlanejada,

        @Min(value = 1, message = "A duração deve ser de no mínimo 1 minuto.")
        @Schema(description = "Duração estimada em minutos", example = "50")
        Integer duracao,

        @Schema(description = "Orientações e atividades da sessão de treino", example = "Séries de resistência e técnicas de respiração.")
        String atividades,

        @Schema(description = "IDs das métricas vinculadas à sessão (null preserva os vínculos atuais; [] remove todos)")
        Set<UUID> metricasIds
) {
    public AtualizarSessaoTreinoRequestDTO(Integer semana, LocalDate dataPlanejada, Integer duracao, String atividades) {
        this(semana, dataPlanejada, duracao, atividades, null);
    }
}
