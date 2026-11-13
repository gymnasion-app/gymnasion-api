package com.gymnasion.tcc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;

public record CorrigirDesempenhoRequestDTO(
        @Schema(description = "Nova data real de execução (a data planejada é preservada). Omitir para não alterar", example = "2026-10-23")
        LocalDate dataReal,

        @Schema(description = "Novas observações do desempenho. Omitir (null) para não alterar; enviar string vazia (\"\") para limpar", example = "Aluno relatou dor no ombro na última série.")
        String observacoes,

        @Valid
        @Schema(description = "Métricas a corrigir (valores já registrados) ou a incluir (métricas pendentes do personal). Omitir as que não serão alteradas")
        List<RegistrarResultadoMetricaDTO> metricas,

        @Min(value = 1, message = "A duração deve ser no mínimo 1 minuto.")
        @Max(value = 1440, message = "A duração não pode ultrapassar 1440 minutos.")
        @Schema(description = "Nova duração real da sessão em minutos. Omitir para não alterar", example = "75")
        Integer duracao
) {
    public CorrigirDesempenhoRequestDTO(
            LocalDate dataReal,
            String observacoes,
            List<RegistrarResultadoMetricaDTO> metricas
    ) {
        this(dataReal, observacoes, metricas, null);
    }
}
