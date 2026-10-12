package com.nord.nord.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EnderecoRequest(
        @NotBlank @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido (use 00000-000)") String cep,
        @NotBlank @Size(min = 2, max = 2, message = "use a sigla do estado, ex.: SP") String estado,
        @NotBlank @Size(max = 80) String cidade,
        @NotBlank @Size(max = 80) String bairro,
        @NotBlank @Size(max = 120) String rua,
        @NotNull @Positive Integer numero,
        @Size(max = 100) String complemento) {
}
