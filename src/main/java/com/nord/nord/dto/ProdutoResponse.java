package com.nord.nord.dto;

public record ProdutoResponse(Long id, String nome, String descricao, String imagem, Double preco,
                              Integer quantidade, String tamanho, Long categoriaId, String categoriaNome) {
}
