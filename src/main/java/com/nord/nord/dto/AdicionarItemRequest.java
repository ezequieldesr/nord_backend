package com.nord.nord.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdicionarItemRequest(
        @NotNull Long produtoId,
        @NotBlank @Size(max = 20) String tamanho,
        @NotNull @Min(1) @Max(20) Integer quantidade) {
}
