package com.nord.nord.repository;

import com.nord.nord.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    List<Produto> findAllByOrderByIdAsc();

    List<Produto> findByCategoria_IdOrderByIdAsc(Long categoriaId);
}
