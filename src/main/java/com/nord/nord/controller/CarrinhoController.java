package com.nord.nord.controller;

import com.nord.nord.dto.AdicionarItemRequest;
import com.nord.nord.dto.AlterarQuantidadeRequest;
import com.nord.nord.dto.CarrinhoResponse;
import com.nord.nord.model.Usuario;
import com.nord.nord.service.CarrinhoService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carrinho")
public class CarrinhoController {

    private final CarrinhoService carrinhoService;

    public CarrinhoController(CarrinhoService carrinhoService) {
        this.carrinhoService = carrinhoService;
    }

    @GetMapping
    public CarrinhoResponse ver(@AuthenticationPrincipal Usuario usuario) {
        return carrinhoService.ver(usuario.getId());
    }

    @PostMapping("/itens")
    public CarrinhoResponse adicionarItem(@AuthenticationPrincipal Usuario usuario,
                                          @Valid @RequestBody AdicionarItemRequest req) {
        return carrinhoService.adicionarItem(usuario.getId(), req);
    }

    @PutMapping("/itens/{itemId}")
    public CarrinhoResponse alterarQuantidade(@AuthenticationPrincipal Usuario usuario,
                                              @PathVariable Long itemId,
                                              @Valid @RequestBody AlterarQuantidadeRequest req) {
        return carrinhoService.alterarQuantidade(usuario.getId(), itemId, req.quantidade());
    }

    @DeleteMapping("/itens/{itemId}")
    public CarrinhoResponse removerItem(@AuthenticationPrincipal Usuario usuario, @PathVariable Long itemId) {
        return carrinhoService.removerItem(usuario.getId(), itemId);
    }
}
