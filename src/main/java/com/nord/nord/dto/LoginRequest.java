package com.nord.nord.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import java.nio.charset.StandardCharsets;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 72) String senha) {
    @AssertTrue(message = "Senha deve ter no máximo 72 bytes em UTF-8.")
    public boolean isSenhaDentroDoLimite() {
        return senha == null || senha.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
