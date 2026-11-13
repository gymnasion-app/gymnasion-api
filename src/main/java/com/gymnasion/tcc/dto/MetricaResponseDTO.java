package com.gymnasion.tcc.dto;

import com.gymnasion.tcc.domain.enums.CampoStrava;
import com.gymnasion.tcc.domain.enums.ResponsavelPreenchimento;
import com.gymnasion.tcc.domain.enums.TipoMetrica;
import java.util.UUID;

public record MetricaResponseDTO(
        UUID id,
        String titulo,
        TipoMetrica tipo,
        String unidade,
        Long modalidadeId,
        String nomeModalidade,
        Boolean obrigatoria,
        ResponsavelPreenchimento quemPreenche,
        CampoStrava campoStrava,
        Boolean ativo,
        Long quantidadeAlunosComRegistros,
        Boolean possuiHistorico
) {
    public MetricaResponseDTO(UUID id, String titulo, TipoMetrica tipo, Long modalidadeId, String nomeModalidade, Boolean obrigatoria, ResponsavelPreenchimento quemPreenche, Boolean ativo, Long quantidadeAlunosComRegistros) {
        this(id, titulo, tipo, null, modalidadeId, nomeModalidade, obrigatoria, quemPreenche, null, ativo, quantidadeAlunosComRegistros, false);
    }

    public MetricaResponseDTO(UUID id, String titulo, TipoMetrica tipo, Long modalidadeId, String nomeModalidade, Boolean obrigatoria, ResponsavelPreenchimento quemPreenche, CampoStrava campoStrava, Boolean ativo, Long quantidadeAlunosComRegistros) {
        this(id, titulo, tipo, null, modalidadeId, nomeModalidade, obrigatoria, quemPreenche, campoStrava, ativo, quantidadeAlunosComRegistros, false);
    }
}
