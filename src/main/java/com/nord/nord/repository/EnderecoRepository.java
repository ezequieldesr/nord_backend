package com.nord.nord.repository;

import com.nord.nord.model.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnderecoRepository extends JpaRepository<Endereco, Long> {
    List<Endereco> findByUsuario_IdOrderByIdDesc(Long usuarioId);

    /** Só encontra se o endereço for DESTE usuário: é a trava contra mexer no endereço dos outros. */
    Optional<Endereco> findByIdAndUsuario_Id(Long id, Long usuarioId);
}
