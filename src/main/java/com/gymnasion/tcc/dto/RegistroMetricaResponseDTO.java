package com.gymnasion.tcc.dto;

import com.gymnasion.tcc.domain.enums.TipoMetrica;
import java.util.UUID;

public record RegistroMetricaResponseDTO(
        UUID id,
        UUID metricaId,
        String tituloMetrica,
        TipoMetrica tipoMetrica,
        String unidade,
        String valor
) {
    public RegistroMetricaResponseDTO(UUID id, UUID metricaId, String tituloMetrica, TipoMetrica tipoMetrica, String valor) {
        this(id, metricaId, tituloMetrica, tipoMetrica, null, valor);
    }
}
