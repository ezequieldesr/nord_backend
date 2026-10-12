package com.nord.nord.service;

import com.nord.nord.dto.CategoriaRequest;
import com.nord.nord.exception.RecursoNaoEncontradoException;
import com.nord.nord.exception.RegraDeNegocioException;
import com.nord.nord.model.Categoria;
import com.nord.nord.repository.CategoriaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listar() {
        return categoriaRepository.findAll(Sort.by("nome"));
    }

    @Transactional
    public Categoria criar(CategoriaRequest req) {
        String nome = req.nome().trim();
        if (categoriaRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraDeNegocioException("Já existe uma categoria com esse nome.");
        }
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria atualizar(Long id, CategoriaRequest req) {
        Categoria categoria = buscar(id);
        String nome = req.nome().trim();
        if (!categoria.getNome().equalsIgnoreCase(nome) && categoriaRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraDeNegocioException("Já existe uma categoria com esse nome.");
        }
        categoria.setNome(nome);
        return categoria;
    }

    @Transactional
    public void deletar(Long id) {
        categoriaRepository.delete(buscar(id));
        categoriaRepository.flush(); // se houver produtos nela, o erro aparece aqui (vira 409 no handler)
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
    }
}
