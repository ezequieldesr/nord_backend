package com.nord.nord.service;

import com.nord.nord.dto.ProdutoRequest;
import com.nord.nord.dto.ProdutoResponse;
import com.nord.nord.exception.RecursoNaoEncontradoException;
import com.nord.nord.model.Categoria;
import com.nord.nord.model.Produto;
import com.nord.nord.repository.CategoriaRepository;
import com.nord.nord.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProdutoService(ProdutoRepository produtoRepository, CategoriaRepository categoriaRepository) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponse> listar(Long categoriaId) {
        List<Produto> produtos = categoriaId == null
                ? produtoRepository.findAllByOrderByIdAsc()
                : produtoRepository.findByCategoria_IdOrderByIdAsc(categoriaId);
        return produtos.stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorId(Long id) {
        return paraResponse(buscar(id));
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest req) {
        Produto produto = new Produto();
        aplicar(produto, req);
        return paraResponse(produtoRepository.save(produto));
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest req) {
        Produto produto = buscar(id);
        aplicar(produto, req);
        return paraResponse(produto);
    }

    @Transactional
    public void deletar(Long id) {
        produtoRepository.delete(buscar(id));
        produtoRepository.flush(); // se o produto estiver em algum carrinho, vira 409 no handler
    }

    private Produto buscar(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
    }

    private void aplicar(Produto produto, ProdutoRequest req) {
        Categoria categoria = categoriaRepository.findById(req.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
        produto.setNome(req.nome().trim());
        produto.setDescricao(req.descricao());
        produto.setImagem(req.imagem() == null || req.imagem().isBlank() ? null : req.imagem().trim());
        produto.setPreco(req.preco());
        produto.setQuantidade(req.quantidade());
        produto.setTamanho(req.tamanho() == null || req.tamanho().isBlank() ? null : req.tamanho().trim());
        produto.setCategoria(categoria);
    }

    public ProdutoResponse paraResponse(Produto p) {
        return new ProdutoResponse(p.getId(), p.getNome(), p.getDescricao(), p.getImagem(), p.getPreco(),
                p.getQuantidade(), p.getTamanho(), p.getCategoria().getId(), p.getCategoria().getNome());
    }
}
