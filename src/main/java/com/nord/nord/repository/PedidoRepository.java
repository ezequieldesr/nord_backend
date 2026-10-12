package com.nord.nord.repository;

import com.nord.nord.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByUsuario_IdOrderByDataPedidoDesc(Long usuarioId);

    boolean existsByUsuario_Id(Long usuarioId);

    List<Pedido> findAllByOrderByDataPedidoDesc();
}
