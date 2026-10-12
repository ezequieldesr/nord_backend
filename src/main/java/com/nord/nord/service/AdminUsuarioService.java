package com.nord.nord.service;
import com.nord.nord.dto.*;
import com.nord.nord.model.*;
import com.nord.nord.repository.*;
import com.nord.nord.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.util.Locale;
@Service
public class AdminUsuarioService {
    private final UsuarioRepository usuarios;
    private final PedidoRepository pedidos;
    private final CarrinhoRepository carrinhos;
    public AdminUsuarioService(UsuarioRepository usuarios, PedidoRepository pedidos, CarrinhoRepository carrinhos) {
        this.usuarios=usuarios; this.pedidos=pedidos; this.carrinhos=carrinhos;
    }
    @Transactional(readOnly=true)
    public Page<AdminUsuarioResponse> listar(int page) {
        return usuarios.findAll(PageRequest.of(Math.max(0,page),20,Sort.by("id"))).map(AdminUsuarioResponse::from);
    }
    private Usuario bloqueado(Long id, Long adminId) {
        var locked = usuarios.lockAll();
        if (locked.stream().noneMatch(a->a.getId().equals(adminId) && a.isAtivo() && a.getRole()==Role.ADMIN))
            throw new org.springframework.security.access.AccessDeniedException("Acesso administrativo revogado.");
        return locked.stream().filter(u->u.getId().equals(id)).findFirst()
            .orElseThrow(()->new RecursoNaoEncontradoException("Usuário não encontrado."));
    }
    @Transactional
    public AdminUsuarioResponse atualizar(Long id, AdminUsuarioRequest req, Long adminId) {
        Usuario u=bloqueado(id,adminId);
        if(id.equals(adminId) && (!req.ativo() || req.role()!=Role.ADMIN))
            throw new RegraDeNegocioException("Você não pode bloquear ou remover seu próprio acesso administrativo.");
        String email=req.email().trim().toLowerCase(Locale.ROOT);
        usuarios.findByEmail(email).filter(other->!other.getId().equals(id)).ifPresent(other->{throw new RegraDeNegocioException("E-mail já cadastrado.");});
        u.setNome(req.nome().trim()); u.setEmail(email); u.setRole(req.role()); u.setAtivo(req.ativo());
        u.setAuthVersion(u.getAuthVersion()+1);
        return AdminUsuarioResponse.from(usuarios.save(u));
    }
    @Transactional
    public void deletar(Long id, Long adminId) {
        if(id.equals(adminId)) throw new RegraDeNegocioException("Você não pode excluir sua própria conta administrativa.");
        Usuario u=bloqueado(id,adminId);
        if(pedidos.existsByUsuario_Id(id)) throw new RegraDeNegocioException("Este usuário possui pedidos. Desative a conta para preservar o histórico.");
        carrinhos.findByUsuario_Id(id).ifPresent(carrinhos::delete);
        usuarios.delete(u);
    }
}
