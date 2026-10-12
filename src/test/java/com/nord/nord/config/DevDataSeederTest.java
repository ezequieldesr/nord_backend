package com.nord.nord.config;
import com.nord.nord.model.Produto;
import com.nord.nord.repository.CategoriaRepository;
import com.nord.nord.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class DevDataSeederTest {
    @Test void completaImagensSemRecriarCatalogoNemSobrescreverCustomizadas() {
        var categorias = mock(CategoriaRepository.class);
        var produtos = mock(ProdutoRepository.class);
        when(categorias.count()).thenReturn(4L);
        var ausente = new Produto(); ausente.setNome("Camiseta Dry Trail"); ausente.setQuantidade(27);
        var personalizada = new Produto(); personalizada.setNome("Camisa Polo Nord"); personalizada.setImagem("https://example.com/custom.png");
        var outro = new Produto(); outro.setNome("Produto personalizado");
        when(produtos.findAll()).thenReturn(List.of(ausente, personalizada, outro));
        new DevDataSeeder(categorias, produtos).run();
        assertEquals("/api/catalogo/imagens/camiseta-dry-trail.png", ausente.getImagem());
        assertEquals(27, ausente.getQuantidade());
        assertEquals("https://example.com/custom.png", personalizada.getImagem());
        assertNull(outro.getImagem());
        verify(produtos).save(ausente);
        verify(produtos, never()).save(personalizada);
        verify(categorias, never()).save(any());
    }
}