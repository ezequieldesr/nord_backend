package com.nord.nord.controller;
import com.nord.nord.dto.*;
import com.nord.nord.model.Usuario;
import com.nord.nord.service.AdminUsuarioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
import org.springframework.http.HttpStatus;
@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioController {
    private final AdminUsuarioService service;
    public AdminUsuarioController(AdminUsuarioService service) {this.service=service;}
    public record UsuariosPage(List<AdminUsuarioResponse> content, long totalElements, int totalPages, int number) {}
    @GetMapping public UsuariosPage listar(@RequestParam(defaultValue="0") int page) {
        var result=service.listar(page);
        return new UsuariosPage(result.getContent(),result.getTotalElements(),result.getTotalPages(),result.getNumber());
    }
    @PutMapping("/{id}") public AdminUsuarioResponse atualizar(@PathVariable Long id,@Valid @RequestBody AdminUsuarioRequest req,@AuthenticationPrincipal Usuario admin) {return service.atualizar(id,req,admin.getId());}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable Long id,@AuthenticationPrincipal Usuario admin) {service.deletar(id,admin.getId());}
}
