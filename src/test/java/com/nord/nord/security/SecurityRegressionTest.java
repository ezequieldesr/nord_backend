package com.nord.nord.security;

import com.nord.nord.dto.*;
import com.nord.nord.exception.*;
import com.nord.nord.model.*;
import com.nord.nord.repository.*;
import com.nord.nord.service.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityRegressionTest {
    @Test void limiteDeLoginNormalizaContaELiberaAposSucesso() {
        var service = new LoginAttemptService();
        for (int i = 0; i < 5; i++) service.registrarFalha(" USER@example.com ");
        assertThrows(LimiteTentativasException.class, () -> service.verificarBloqueio("user@example.com"));
        service.registrarSucesso("user@example.com");
        assertDoesNotThrow(() -> service.verificarBloqueio("user@example.com"));
    }
    @Test void limiteDeMemoriaNaoPodeSerContornadoComNovasContas() {
        var service = new LoginAttemptService();
        for (int i = 0; i < 10001; i++) service.registrarFalha("user" + i);
        assertThrows(LimiteTentativasException.class, () -> service.verificarBloqueio("outra"));
        service.registrarSucesso("user0");
        assertDoesNotThrow(() -> service.verificarBloqueio("outra"));
    }
    @Test void senhaMultibyteRespeitaLimiteBcrypt() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertFalse(validator.validate(new RegistroRequest("Nome", "a@example.com", "é".repeat(37))).isEmpty());
            assertFalse(validator.validate(new LoginRequest("a@example.com", "é".repeat(37))).isEmpty());
            assertTrue(validator.validate(new RegistroRequest("Nome", "a@example.com", "a".repeat(72))).isEmpty());
        }
    }
    @Test void naoAlteraEnderecoAlheio() {
        var repo = mock(EnderecoRepository.class);
        when(repo.findByIdAndUsuario_Id(99L, 1L)).thenReturn(Optional.empty());
        var service = new EnderecoService(repo, mock(UsuarioRepository.class));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.deletar(1L, 99L));
        verify(repo, never()).delete(any());
    }
    @Test void adicoesRepetidasNaoUltrapassamVinte() {
        var repo = mock(CarrinhoRepository.class);
        var produtos = mock(ProdutoRepository.class);
        var produto = new Produto(); produto.setId(1L); produto.setQuantidade(100);
        var carrinho = new Carrinho();
        var item = new ItemCarrinho(); item.setProduto(produto); item.setTamanho("Único"); item.setQuantidade(20);
        carrinho.getItens().add(item);
        when(repo.findByUsuario_Id(1L)).thenReturn(Optional.of(carrinho));
        when(produtos.findById(1L)).thenReturn(Optional.of(produto));
        var service = new CarrinhoService(repo, produtos, mock(UsuarioRepository.class));
        assertThrows(RegraDeNegocioException.class, () -> service.adicionarItem(1L, new AdicionarItemRequest(1L, "Único", 1)));
        verify(repo, never()).saveAndFlush(any());
    }
    @Test void naoRemoveItemAlheio() {
        var repo = mock(CarrinhoRepository.class);
        when(repo.findByUsuario_Id(1L)).thenReturn(Optional.of(new Carrinho()));
        var service = new CarrinhoService(repo, mock(ProdutoRepository.class), mock(UsuarioRepository.class));
        assertThrows(RecursoNaoEncontradoException.class, () -> service.removerItem(1L, 99L));
    }
}