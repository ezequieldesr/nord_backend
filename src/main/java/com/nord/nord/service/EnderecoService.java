package com.nord.nord.service;

import com.nord.nord.dto.EnderecoRequest;
import com.nord.nord.dto.EnderecoResponse;
import com.nord.nord.exception.RecursoNaoEncontradoException;
import com.nord.nord.model.Endereco;
import com.nord.nord.repository.EnderecoRepository;
import com.nord.nord.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Todos os métodos recebem o usuarioId vindo do TOKEN (nunca do corpo da requisição).
 * Buscar sempre por (id do endereço + id do usuário) impede mexer no endereço de outra pessoa.
 */
@Service
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final UsuarioRepository usuarioRepository;

    public EnderecoService(EnderecoRepository enderecoRepository, UsuarioRepository usuarioRepository) {
        this.enderecoRepository = enderecoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<EnderecoResponse> listarPorUsuario(Long usuarioId) {
        return enderecoRepository.findByUsuario_IdOrderByIdDesc(usuarioId).stream()
                .map(this::paraResponse).toList();
    }

    @Transactional
    public EnderecoResponse criar(Long usuarioId, EnderecoRequest req) {
        Endereco endereco = new Endereco();
        endereco.setUsuario(usuarioRepository.getReferenceById(usuarioId));
        aplicar(endereco, req);
        return paraResponse(enderecoRepository.save(endereco));
    }

    @Transactional
    public EnderecoResponse atualizar(Long usuarioId, Long id, EnderecoRequest req) {
        Endereco endereco = buscarDoUsuario(usuarioId, id);
        aplicar(endereco, req);
        return paraResponse(endereco);
    }

    @Transactional
    public void deletar(Long usuarioId, Long id) {
        enderecoRepository.delete(buscarDoUsuario(usuarioId, id));
    }

    private Endereco buscarDoUsuario(Long usuarioId, Long id) {
        return enderecoRepository.findByIdAndUsuario_Id(id, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço não encontrado."));
    }

    private void aplicar(Endereco e, EnderecoRequest r) {
        e.setCep(r.cep().trim());
        e.setEstado(r.estado().trim().toUpperCase());
        e.setCidade(r.cidade().trim());
        e.setBairro(r.bairro().trim());
        e.setRua(r.rua().trim());
        e.setNumero(r.numero());
        e.setComplemento(r.complemento() == null || r.complemento().isBlank() ? null : r.complemento().trim());
    }

    public EnderecoResponse paraResponse(Endereco e) {
        return new EnderecoResponse(e.getId(), e.getCep(), e.getEstado(), e.getCidade(), e.getBairro(),
                e.getRua(), e.getNumero(), e.getComplemento());
    }
}
