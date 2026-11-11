package com.gymnasion.tcc.dto;

import java.util.List;
import java.util.UUID;

public record AcompanhamentoAlunoResponseDTO(
        UUID alunoId,
        String nomeAluno,
        String emailAluno,
        UUID rotinaAtivaId,
        String tituloRotinaAtiva,
        Long modalidadeId,
        String nomeModalidade,
        Integer quantidadeMetricasPendentes,
        Integer totalSessoesPlanejadas,
        Integer totalSessoesConcluidas,
        List<MetricaPendenteDTO> todasMetricasPendentes,
        List<SessaoAcompanhamentoDTO> sessoes,
        List<RotinaAtivaAcompanhamentoDTO> rotinasAtivas
) {
    public AcompanhamentoAlunoResponseDTO(
            UUID alunoId,
            String nomeAluno,
            String emailAluno,
            UUID rotinaAtivaId,
            String tituloRotinaAtiva,
            Long modalidadeId,
            String nomeModalidade,
            Integer quantidadeMetricasPendentes,
            Integer totalSessoesPlanejadas,
            Integer totalSessoesConcluidas,
            List<MetricaPendenteDTO> todasMetricasPendentes,
            List<SessaoAcompanhamentoDTO> sessoes
    ) {
        this(
                alunoId,
                nomeAluno,
                emailAluno,
                rotinaAtivaId,
                tituloRotinaAtiva,
                modalidadeId,
                nomeModalidade,
                quantidadeMetricasPendentes,
                totalSessoesPlanejadas,
                totalSessoesConcluidas,
                todasMetricasPendentes,
                sessoes,
                List.of()
        );
    }

    public record RotinaAtivaAcompanhamentoDTO(
            UUID rotinaId,
            String rotinaTitulo,
            Long modalidadeId,
            String modalidadeNome,
            Integer frequenciaSemanal,
            int quantidadeMetricasPendentes,
            int totalSessoesPlanejadas,
            int totalSessoesConcluidas,
            List<MetricaPendenteDTO> metricasPendentes,
            List<SessaoAcompanhamentoDTO> sessoes
    ) {}
}
