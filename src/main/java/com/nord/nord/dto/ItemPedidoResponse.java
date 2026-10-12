package com.nord.nord.dto;

public record ItemPedidoResponse(String nomeProduto, String imagem, String tamanho,
                                 Integer quantidade, Double precoUnitario) {
}
