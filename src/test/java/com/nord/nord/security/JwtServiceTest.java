package com.nord.nord.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import java.util.Base64;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
class JwtServiceTest {
    private final String secret = Base64.getEncoder().encodeToString(new byte[32]);
    @Test void tokenAssinadoAutenticaApenasSeuTitular() {
        var service = new JwtService(secret, 60000);
        var user = User.withUsername("a@example.com").password("x").roles("CLIENTE").build();
        var token = service.gerarToken(user);
        assertTrue(service.tokenValido(token, user));
        assertFalse(service.tokenValido(token, User.withUsername("b@example.com").password("x").roles("CLIENTE").build()));
    }
    @Test void rejeitaTokenExpiradoAssinaturaAlheiaESubjectAusente() {
        var service = new JwtService(secret, 60000);
        var key = Keys.hmacShaKeyFor(new byte[32]);
        var expired = Jwts.builder().setSubject("a").setExpiration(new Date(0)).signWith(key).compact();
        assertThrows(JwtException.class, () -> service.extrairEmail(expired));
        var noSubject = Jwts.builder().setIssuedAt(new Date()).signWith(key).compact();
        assertThrows(IllegalArgumentException.class, () -> service.extrairEmail(noSubject));
        var otherKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        var forged = Jwts.builder().setSubject("a").signWith(otherKey).compact();
        assertThrows(JwtException.class, () -> service.extrairEmail(forged));
    }
    @Test void bloquearAlterarOuRecriarContaRevogaToken() {
        var service = new JwtService(secret,60000);
        var u = new com.nord.nord.model.Usuario(); u.setId(1L); u.setEmail("a@example.com");
        String token=service.gerarToken(u);
        assertTrue(service.tokenValido(token,u));
        u.setAtivo(false); assertFalse(service.tokenValido(token,u));
        u.setAtivo(true); u.setAuthVersion(1); assertFalse(service.tokenValido(token,u));
        u.setAuthVersion(0); u.setId(2L); assertFalse(service.tokenValido(token,u));
    }
    @Test void configuracaoInvalidaNaoInicia() {
        assertThrows(RuntimeException.class, () -> new JwtService("YQ==", 1000));
        assertThrows(IllegalStateException.class, () -> new JwtService(secret, 0));
    }
}