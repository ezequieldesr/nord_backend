package com.nord.nord.dto;

public record ItemCarrinhoResponse(Long id, Long produtoId, String nomeProduto, String imagemProduto,
                                   Double preco, String tamanho, Integer quantidade, Double subtotal) {
}
