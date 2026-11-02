package com.gymnasion.tcc.dto;

public record ModalidadeResponseDTO(
        Long id,
        String nome,
        String descricao,
        Boolean ativo
) {
    public ModalidadeResponseDTO(Long id, String nome, String descricao) {
        this(id, nome, descricao, true);
    }
}
