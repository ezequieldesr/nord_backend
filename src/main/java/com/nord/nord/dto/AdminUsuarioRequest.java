package com.nord.nord.dto;
import com.nord.nord.model.Role;
import jakarta.validation.constraints.*;
public record AdminUsuarioRequest(@NotBlank @Size(max=150) String nome,
    @NotBlank @Email @Size(max=254) String email, @NotNull Role role, @NotNull Boolean ativo) {}
