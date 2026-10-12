package com.nord.nord.dto;

import com.nord.nord.model.Status;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusRequest(@NotNull Status status) {
}
