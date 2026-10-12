package com.nord.nord.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AlterarQuantidadeRequest(@NotNull @Min(1) @Max(20) Integer quantidade) {
}
