package com.nord.nord.dto;

import com.nord.nord.model.Status;

import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(Long id, LocalDateTime dataPedido, Status status, String formaPagamento,
                             Integer quantidadeParcelas, Double valorParcela, Double valorTotal,
                             String enderecoEntrega, String clienteNome, String clienteEmail,
                             List<ItemPedidoResponse> itens) {
}
