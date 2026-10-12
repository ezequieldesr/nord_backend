package com.nord.nord.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CriarPedidoRequest(
        @NotNull Long enderecoId,
        @NotBlank @Pattern(regexp = "PIX|CARTAO|BOLETO", message = "use PIX, CARTAO ou BOLETO") String formaPagamento,
        @NotNull @Min(1) @Max(12) Integer quantidadeParcelas) {
}
