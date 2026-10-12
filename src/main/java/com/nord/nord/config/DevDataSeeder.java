package com.nord.nord.config;

import com.nord.nord.model.Categoria;
import com.nord.nord.model.Produto;
import com.nord.nord.repository.CategoriaRepository;
import com.nord.nord.repository.ProdutoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Dados de exemplo para você ver a loja funcionando. Roda só em dev. Cria exemplos no banco vazio e completa imagens ausentes no catálogo existente. */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;

    public DevDataSeeder(CategoriaRepository categoriaRepository, ProdutoRepository produtoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
    }

    @Override
    public void run(String... args) {
        if (categoriaRepository.count() > 0) {
            completarImagensAusentes();
            return;
        }
        Categoria camisas = categoria("Camisas");
        Categoria tenis = categoria("Tênis");
        Categoria bones = categoria("Bonés");
        Categoria calcas = categoria("Calças");

        produto("Camiseta Dry Trail", "Camiseta leve de secagem rápida, boa para treino e corrida.", 89.90, 30, "P,M,G,GG", camisas);
        produto("Camiseta Algodão Básica", "Algodão penteado, corte reto, para usar todo dia.", 59.90, 50, "P,M,G,GG", camisas);
        produto("Camisa Polo Nord", "Polo de piquê com gola reforçada.", 129.90, 20, "M,G,GG", camisas);
        produto("Tênis Corrida Fjord", "Amortecimento macio e solado com boa aderência para asfalto.", 399.90, 15, "38,39,40,41,42,43", tenis);
        produto("Tênis Urbano Branco", "Couro sintético, solado de borracha, combina com tudo.", 329.90, 12, "37,38,39,40,41,42", tenis);
        produto("Boné Aba Curva", "Ajuste traseiro em velcro, tecido leve.", 79.90, 40, null, bones);
        produto("Boné Trucker", "Tela traseira para ventilar. Poucas unidades.", 69.90, 3, null, bones);
        produto("Calça Jogger", "Moletom leve, cós com cordão e punho ajustado.", 189.90, 25, "P,M,G,GG", calcas);
        produto("Calça Corta-vento", "Tecido que protege da chuva fina e do vento.", 249.90, 18, "M,G,GG", calcas);
    }

    private Categoria categoria(String nome) {
        Categoria c = new Categoria();
        c.setNome(nome);
        return categoriaRepository.save(c);
    }

    private void produto(String nome, String descricao, double preco, int quantidade, String tamanho, Categoria categoria) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setImagem(CatalogImages.urlFor(nome));
        p.setDescricao(descricao);
        p.setPreco(preco);
        p.setQuantidade(quantidade);
        p.setTamanho(tamanho);
        p.setCategoria(categoria);
        produtoRepository.save(p);
    }
    private void completarImagensAusentes() {
        for (Produto produto : produtoRepository.findAll()) {
            String imagem = CatalogImages.urlFor(produto.getNome());
            if (imagem != null && (produto.getImagem() == null || produto.getImagem().isBlank())) {
                produto.setImagem(imagem);
                produtoRepository.save(produto);
            }
        }
    }}
