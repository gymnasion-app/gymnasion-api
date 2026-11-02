package com.gymnasion.tcc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gymnasion.tcc.domain.enums.CampoAuditado;
import com.gymnasion.tcc.domain.enums.TipoOperacaoAuditoria;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditoriaDesempenhoResponseDTO(
        UUID id,
        TipoOperacaoAuditoria tipoOperacao,
        CampoAuditado campo,
        UUID metricaId,
        String tituloMetrica,
        String valorAnterior,
        String valorNovo,
        UUID usuarioId,
        String nomeUsuario,
        OffsetDateTime dataHora,
        String responsavelRole
) {
    public AuditoriaDesempenhoResponseDTO(
            UUID id,
            TipoOperacaoAuditoria tipoOperacao,
            CampoAuditado campo,
            UUID metricaId,
            String tituloMetrica,
            String valorAnterior,
            String valorNovo,
            UUID usuarioId,
            String nomeUsuario,
            OffsetDateTime dataHora
    ) {
        this(id, tipoOperacao, campo, metricaId, tituloMetrica, valorAnterior, valorNovo, usuarioId, nomeUsuario, dataHora, null);
    }

    @JsonProperty("metricaTitulo")
    public String metricaTitulo() {
        return tituloMetrica;
    }

    @JsonProperty("tipoAlteracao")
    public String tipoAlteracao() {
        return tipoOperacao != null ? tipoOperacao.name() : null;
    }

    @JsonProperty("responsavelNome")
    public String responsavelNome() {
        return nomeUsuario;
    }

    @JsonProperty("dataHoraAlteracao")
    public OffsetDateTime dataHoraAlteracao() {
        return dataHora;
    }
}

