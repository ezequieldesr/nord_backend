package com.nord.nord.service;

import com.nord.nord.dto.AdicionarItemRequest;
import com.nord.nord.dto.CarrinhoResponse;
import com.nord.nord.dto.ItemCarrinhoResponse;
import com.nord.nord.exception.RecursoNaoEncontradoException;
import com.nord.nord.exception.RegraDeNegocioException;
import com.nord.nord.model.Carrinho;
import com.nord.nord.model.ItemCarrinho;
import com.nord.nord.model.Produto;
import com.nord.nord.repository.CarrinhoRepository;
import com.nord.nord.repository.ProdutoRepository;
import com.nord.nord.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * O carrinho é sempre o do usuário do TOKEN. Os itens são procurados DENTRO desse carrinho,
 * então não existe como alterar ou remover item do carrinho de outra pessoa.
 */
@Service
public class CarrinhoService {

    private final CarrinhoRepository carrinhoRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;

    public CarrinhoService(CarrinhoRepository carrinhoRepository,
                           ProdutoRepository produtoRepository,
                           UsuarioRepository usuarioRepository) {
        this.carrinhoRepository = carrinhoRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public CarrinhoResponse ver(Long usuarioId) {
        return paraResponse(buscarOuCriar(usuarioId));
    }

    @Transactional
    public CarrinhoResponse adicionarItem(Long usuarioId, AdicionarItemRequest req) {
        Carrinho carrinho = buscarOuCriar(usuarioId);

        Produto produto = produtoRepository.findById(req.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
        String tamanho = validarTamanho(produto, req.tamanho());

        Optional<ItemCarrinho> existente = carrinho.getItens().stream()
                .filter(i -> i.getProduto().getId().equals(produto.getId()) && i.getTamanho().equals(tamanho))
                .findFirst();

        int novaQuantidade = req.quantidade() + existente.map(ItemCarrinho::getQuantidade).orElse(0);
        garantirEstoque(produto, novaQuantidade);

        if (existente.isPresent()) {
            existente.get().setQuantidade(novaQuantidade);
        } else {
            ItemCarrinho item = new ItemCarrinho();
            item.setCarrinho(carrinho);
            item.setProduto(produto);
            item.setTamanho(tamanho);
            item.setQuantidade(novaQuantidade);
            carrinho.getItens().add(item);
        }

        // saveAndFlush garante que o item novo já tenha id antes de montar a resposta
        carrinhoRepository.saveAndFlush(carrinho);
        return paraResponse(carrinho);
    }

    @Transactional
    public CarrinhoResponse alterarQuantidade(Long usuarioId, Long itemId, Integer quantidade) {
        Carrinho carrinho = buscarOuCriar(usuarioId);
        ItemCarrinho item = carrinho.getItens().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado no seu carrinho."));

        garantirEstoque(item.getProduto(), quantidade);
        item.setQuantidade(quantidade);
        return paraResponse(carrinho);
    }

    @Transactional
    public CarrinhoResponse removerItem(Long usuarioId, Long itemId) {
        Carrinho carrinho = buscarOuCriar(usuarioId);
        // orphanRemoval: tirar da lista apaga a linha no banco
        boolean removido = carrinho.getItens().removeIf(i -> i.getId().equals(itemId));
        if (!removido) {
            throw new RecursoNaoEncontradoException("Item não encontrado no seu carrinho.");
        }
        return paraResponse(carrinho);
    }

    private Carrinho buscarOuCriar(Long usuarioId) {
        return carrinhoRepository.findByUsuario_Id(usuarioId).orElseGet(() -> {
            Carrinho novo = new Carrinho();
            novo.setUsuario(usuarioRepository.getReferenceById(usuarioId));
            return carrinhoRepository.save(novo);
        });
    }

    private String validarTamanho(Produto produto, String solicitado) {
        List<String> disponiveis = produto.tamanhosDisponiveis();
        if (disponiveis.isEmpty()) {
            return "Único";
        }
        return disponiveis.stream()
                .filter(t -> t.equalsIgnoreCase(solicitado.trim()))
                .findFirst()
                .orElseThrow(() -> new RegraDeNegocioException("Tamanho indisponível para este produto."));
    }

    private void garantirEstoque(Produto produto, int quantidadeDesejada) {
        if (quantidadeDesejada < 1 || quantidadeDesejada > 20) {
            throw new RegraDeNegocioException("Quantidade deve estar entre 1 e 20 por item.");
        }
        if (quantidadeDesejada > produto.getQuantidade()) {
            throw new RegraDeNegocioException("Estoque insuficiente: restam " + produto.getQuantidade()
                    + " unidade(s) de " + produto.getNome() + ".");
        }
    }

    private CarrinhoResponse paraResponse(Carrinho carrinho) {
        List<ItemCarrinhoResponse> itens = carrinho.getItens().stream()
                .map(i -> new ItemCarrinhoResponse(
                        i.getId(),
                        i.getProduto().getId(),
                        i.getProduto().getNome(),
                        i.getProduto().getImagem(),
                        i.getProduto().getPreco(),
                        i.getTamanho(),
                        i.getQuantidade(),
                        arredondar(i.getProduto().getPreco() * i.getQuantidade())))
                .toList();
        double total = arredondar(itens.stream().mapToDouble(ItemCarrinhoResponse::subtotal).sum());
        return new CarrinhoResponse(carrinho.getId(), itens, total);
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
