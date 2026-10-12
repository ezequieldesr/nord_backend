package com.nord.nord.security;

import com.nord.nord.controller.*;
import com.nord.nord.repository.UsuarioRepository;
import com.nord.nord.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, ProdutoController.class, CategoriaController.class, CatalogImageController.class,
    AdminUsuarioController.class, AdminPedidoController.class, CarrinhoController.class, EnderecoController.class,
    PedidoController.class, UsuarioController.class}, properties = {
    "app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "app.jwt.expiration-ms=60000",
    "app.cors.allowed-origins=https://allowed.example"})
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class, CustomUserDetailService.class})
class EndpointSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean UsuarioRepository usuarios;
    @MockBean AdminUsuarioService adminUsuarios;
    @MockBean ProdutoService produtos;
    @MockBean CategoriaService categorias;
    @MockBean PedidoService pedidos;
    @MockBean CarrinhoService carrinho;
    @MockBean EnderecoService enderecos;
    @MockBean LoginAttemptService tentativas;

    @ParameterizedTest
    @ValueSource(strings = {"/api/usuarios/me", "/api/carrinho", "/api/enderecos", "/api/pedidos/meus", "/api/admin/pedidos", "/api/admin/usuarios"})
    void visitanteNaoAcessaDadosPrivados(String rota) throws Exception {
        mvc.perform(get(rota)).andExpect(status().isUnauthorized());
    }
    @ParameterizedTest
    @ValueSource(strings = {"/api/produtos", "/api/categorias"})
    void catalogoPublicoSomenteLeitura(String rota) throws Exception {
        mvc.perform(get(rota)).andExpect(status().isOk());
        mvc.perform(post(rota).contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post(rota).with(user("cliente").roles("CLIENTE"))
            .contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(put(rota + "/1").with(user("cliente").roles("CLIENTE"))
            .contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(delete(rota + "/1").with(user("cliente").roles("CLIENTE")))
            .andExpect(status().isForbidden());
    }
    @Test void clienteNaoAdministraPedidos() throws Exception {
        mvc.perform(get("/api/admin/pedidos").with(user("cliente").roles("CLIENTE")))
            .andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/pedidos/1/status").with(user("cliente").roles("CLIENTE"))
            .contentType("application/json").content("{\"status\":\"ENVIADO\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/pedidos").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk());
    }
    @Test void clienteNaoAdministraUsuarios() throws Exception {
        mvc.perform(get("/api/admin/usuarios").with(user("cliente").roles("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/usuarios/2").with(user("cliente").roles("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/usuarios/2").with(user("cliente").roles("CLIENTE")).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        org.mockito.Mockito.when(adminUsuarios.listar(0)).thenReturn(org.springframework.data.domain.Page.empty());
        mvc.perform(get("/api/admin/usuarios").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray());
    }
    @Test void tokenInvalidoNaoAutentica() throws Exception {
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer adulterado"))
            .andExpect(status().isUnauthorized());
    }
    @Test void corsRejeitaOrigemEstranha() throws Exception {
        mvc.perform(options("/api/carrinho").header("Origin", "https://evil.example")
            .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden());
    }
    @Test void headersDeSeguranca() throws Exception {
        mvc.perform(get("/api/produtos"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().exists("Content-Security-Policy"));
    }
    @Test void loginComEntradaInvalida() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json")
            .content("{\"email\":\"invalido\",\"senha\":\"\"}"))
            .andExpect(status().isBadRequest());
    }
    @Autowired JwtService jwt;
    @Test void tokenRealRetornaSomenteDadosDoTitular() throws Exception {
        var titular = new com.nord.nord.model.Usuario();
        titular.setId(1L); titular.setEmail("a@example.com"); titular.setNome("Cliente");
        titular.setSenha("hash-nao-publico");
        org.mockito.Mockito.when(usuarios.findByEmail("a@example.com"))
            .thenReturn(java.util.Optional.of(titular));
        mvc.perform(get("/api/usuarios/me").header("Authorization", "Bearer " + jwt.gerarToken(titular)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.senha").doesNotExist()).andExpect(jsonPath("$.password").doesNotExist());
    }
    @ParameterizedTest
    @ValueSource(strings = {"/api/carrinho/itens", "/api/enderecos", "/api/pedidos"})
    void visitanteNaoCriaRecursosPrivados(String rota) throws Exception {
        mvc.perform(post(rota).contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
    }    @ParameterizedTest
    @ValueSource(strings = {"/", "/index.html", "/js/api.js", "/css/styles.css"})
    void frontendAntigoNaoEServido(String rota) throws Exception {
        mvc.perform(get(rota)).andExpect(status().isNotFound());
    }    @ParameterizedTest
    @ValueSource(strings = {"camiseta-dry-trail.png", "camiseta-algodao-basica.png", "camisa-polo-nord.png",
            "tenis-corrida-fjord.png", "tenis-urbano-branco.png", "bone-aba-curva.png", "bone-trucker.png",
            "calca-jogger.png", "calca-corta-vento.png"})
    void imagensDoCatalogoSaoPublicas(String arquivo) throws Exception {
        mvc.perform(get("/api/catalogo/imagens/" + arquivo)).andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", "max-age=604800, public"));
    }
    @Test void naoExpoeArquivoForaDoCatalogo() throws Exception {
        mvc.perform(get("/api/catalogo/imagens/application.properties")).andExpect(status().isNotFound());
    }}