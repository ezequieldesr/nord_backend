package com.nord.nord.dto;
import com.nord.nord.model.Usuario;
import com.nord.nord.model.Role;
public record AdminUsuarioResponse(Long id, String nome, String email, Role role, boolean ativo) {
    public static AdminUsuarioResponse from(Usuario u) { return new AdminUsuarioResponse(u.getId(),u.getNome(),u.getEmail(),u.getRole(),u.isAtivo()); }
}
