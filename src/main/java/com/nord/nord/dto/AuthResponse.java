package com.nord.nord.dto;

import com.nord.nord.model.Role;

public record AuthResponse(String token, Long id, String nome, String email, Role role) {
}
