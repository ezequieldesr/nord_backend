package com.nord.nord.dto;

import com.nord.nord.model.Role;

public record UsuarioResponse(Long id, String nome, String email, Role role) {
}
