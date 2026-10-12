package com.nord.nord.controller;

import com.nord.nord.dto.CriarPedidoRequest;
import com.nord.nord.dto.PedidoResponse;
import com.nord.nord.model.Usuario;
import com.nord.nord.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse criar(@AuthenticationPrincipal Usuario usuario,
                                @Valid @RequestBody CriarPedidoRequest req) {
        return pedidoService.criar(usuario.getId(), req);
    }

    @GetMapping("/meus")
    public List<PedidoResponse> meusPedidos(@AuthenticationPrincipal Usuario usuario) {
        return pedidoService.listarDoUsuario(usuario.getId());
    }
}
