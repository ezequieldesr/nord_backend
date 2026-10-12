package com.nord.nord.repository;

import com.nord.nord.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u order by u.id")
    java.util.List<Usuario> lockAll();

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
