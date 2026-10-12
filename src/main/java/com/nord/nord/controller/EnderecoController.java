package com.nord.nord.controller;

import com.nord.nord.dto.EnderecoRequest;
import com.nord.nord.dto.EnderecoResponse;
import com.nord.nord.model.Usuario;
import com.nord.nord.service.EnderecoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/enderecos")
public class EnderecoController {

    private final EnderecoService enderecoService;

    public EnderecoController(EnderecoService enderecoService) {
        this.enderecoService = enderecoService;
    }

    @GetMapping
    public List<EnderecoResponse> listar(@AuthenticationPrincipal Usuario usuario) {
        return enderecoService.listarPorUsuario(usuario.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnderecoResponse criar(@AuthenticationPrincipal Usuario usuario,
                                  @Valid @RequestBody EnderecoRequest req) {
        return enderecoService.criar(usuario.getId(), req);
    }

    @PutMapping("/{id}")
    public EnderecoResponse atualizar(@AuthenticationPrincipal Usuario usuario,
                                      @PathVariable Long id,
                                      @Valid @RequestBody EnderecoRequest req) {
        return enderecoService.atualizar(usuario.getId(), id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@AuthenticationPrincipal Usuario usuario, @PathVariable Long id) {
        enderecoService.deletar(usuario.getId(), id);
    }
}
