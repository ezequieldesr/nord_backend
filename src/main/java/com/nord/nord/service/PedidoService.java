package com.nord.nord.service;

import com.nord.nord.dto.CriarPedidoRequest;
import com.nord.nord.dto.ItemPedidoResponse;
import com.nord.nord.dto.PedidoResponse;
import com.nord.nord.exception.RecursoNaoEncontradoException;
import com.nord.nord.exception.RegraDeNegocioException;
import com.nord.nord.model.Carrinho;
import com.nord.nord.model.Endereco;
import com.nord.nord.model.ItemCarrinho;
import com.nord.nord.model.ItemPedido;
import com.nord.nord.model.Pedido;
import com.nord.nord.model.Produto;
import com.nord.nord.model.Status;
import com.nord.nord.model.Usuario;
import com.nord.nord.repository.CarrinhoRepository;
import com.nord.nord.repository.EnderecoRepository;
import com.nord.nord.repository.PedidoRepository;
import com.nord.nord.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final CarrinhoRepository carrinhoRepository;
    private final EnderecoRepository enderecoRepository;
    private final UsuarioRepository usuarioRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         CarrinhoRepository carrinhoRepository,
                         EnderecoRepository enderecoRepository,
                         UsuarioRepository usuarioRepository) {
        this.pedidoRepository = pedidoRepository;
        this.carrinhoRepository = carrinhoRepository;
        this.enderecoRepository = enderecoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Transforma o carrinho do usuário em pedido. Tudo acontece numa única transação:
     * se qualquer item estiver sem estoque, NADA é gravado (nem pedido, nem baixa de estoque).
     */
    @Transactional
    public PedidoResponse criar(Long usuarioId, CriarPedidoRequest req) {
        Carrinho carrinho = carrinhoRepository.findByUsuario_Id(usuarioId)
                .orElseThrow(() -> new RegraDeNegocioException("Seu carrinho está vazio."));
        if (carrinho.getItens().isEmpty()) {
            throw new RegraDeNegocioException("Seu carrinho está vazio.");
        }

        Endereco endereco = enderecoRepository.findByIdAndUsuario_Id(req.enderecoId(), usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço não encontrado."));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        int parcelas = "CARTAO".equals(req.formaPagamento()) ? req.quantidadeParcelas() : 1;

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setDataPedido(LocalDateTime.now());
        pedido.setStatus(Status.AGUARDANDO_PAGAMENTO);
        pedido.setFormaPagamento(req.formaPagamento());
        pedido.setQuantidadeParcelas(parcelas);
        pedido.setEnderecoEntrega(endereco.formatado());

        double total = 0;
        for (ItemCarrinho ic : carrinho.getItens()) {
            Produto produto = ic.getProduto();

            if (produto.getQuantidade() < ic.getQuantidade()) {
                throw new RegraDeNegocioException("Estoque insuficiente para " + produto.getNome()
                        + ". Restam " + produto.getQuantidade() + " unidade(s).");
            }
            produto.setQuantidade(produto.getQuantidade() - ic.getQuantidade());

            // O preço, o nome e a imagem são COPIADOS: o pedido não muda se o produto mudar depois.
            ItemPedido ip = new ItemPedido();
            ip.setPedido(pedido);
            ip.setNomeProduto(produto.getNome());
            ip.setImagem(produto.getImagem());
            ip.setTamanho(ic.getTamanho());
            ip.setQuantidade(ic.getQuantidade());
            ip.setPrecoUnitario(produto.getPreco());
            pedido.getItens().add(ip);

            total += produto.getPreco() * ic.getQuantidade();
        }

        total = arredondar(total);
        pedido.setValorTotal(total);
        pedido.setValorParcela(arredondar(total / parcelas));

        // Se outra compra levou o mesmo estoque ao mesmo tempo, o @Version do Produto falha aqui (vira 409).
        pedidoRepository.saveAndFlush(pedido);

        carrinho.getItens().clear(); // orphanRemoval apaga os itens do carrinho
        return paraResponse(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarDoUsuario(Long usuarioId) {
        return pedidoRepository.findByUsuario_IdOrderByDataPedidoDesc(usuarioId).stream()
                .map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarTodos() {
        return pedidoRepository.findAllByOrderByDataPedidoDesc().stream()
                .map(this::paraResponse).toList();
    }

    @Transactional
    public PedidoResponse atualizarStatus(Long pedidoId, Status novoStatus) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
        if (novoStatus.ordinal() < pedido.getStatus().ordinal()) {
            throw new RegraDeNegocioException("O status de um pedido não pode voltar para uma etapa anterior.");
        }
        pedido.setStatus(novoStatus);
        return paraResponse(pedido);
    }

    private PedidoResponse paraResponse(Pedido p) {
        List<ItemPedidoResponse> itens = p.getItens().stream()
                .map(i -> new ItemPedidoResponse(i.getNomeProduto(), i.getImagem(), i.getTamanho(),
                        i.getQuantidade(), i.getPrecoUnitario()))
                .toList();
        return new PedidoResponse(p.getId(), p.getDataPedido(), p.getStatus(), p.getFormaPagamento(),
                p.getQuantidadeParcelas(), p.getValorParcela(), p.getValorTotal(), p.getEnderecoEntrega(),
                p.getUsuario().getNome(), p.getUsuario().getEmail(), itens);
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
