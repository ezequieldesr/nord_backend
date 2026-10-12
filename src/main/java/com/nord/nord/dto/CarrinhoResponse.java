package com.nord.nord.dto;

import java.util.List;

public record CarrinhoResponse(Long id, List<ItemCarrinhoResponse> itens, Double total) {
}
