package com.nord.nord.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMs;

    public JwtService(@Value("${app.jwt.secret}") String segredoBase64,
                      @Value("${app.jwt.expiration-ms}") long expiracaoMs) {
        byte[] bytes = Decoders.BASE64.decode(segredoBase64);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret precisa ter no mínimo 32 bytes (em Base64). Gere com: openssl rand -base64 48");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        if (expiracaoMs <= 0) throw new IllegalStateException("A validade JWT deve ser positiva.");
        this.expiracaoMs = expiracaoMs;
    }

    public String gerarToken(UserDetails usuario) {
        Date agora = new Date();
        var builder = Jwts.builder();
        if (usuario instanceof com.nord.nord.model.Usuario u) builder.claim("uid", u.getId()).claim("av", u.getAuthVersion());
        return builder
                .setSubject(usuario.getUsername())
                .setIssuedAt(agora)
                .setExpiration(new Date(agora.getTime() + expiracaoMs))
                .signWith(chave, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Lança JwtException se o token for inválido, adulterado ou estiver expirado. */
    public String extrairEmail(String token) {
        String email = extrairClaims(token).getSubject();
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Token sem subject.");
        return email;
    }

    public boolean tokenValido(String token, UserDetails usuario) {
        Claims claims = extrairClaims(token);
        if (!usuario.isEnabled() || !usuario.getUsername().equals(claims.getSubject())) return false;
        if (usuario instanceof com.nord.nord.model.Usuario u) {
            return claims.get("uid") instanceof Number id && id.longValue() == u.getId()
                && claims.get("av") instanceof Number version && version.longValue() == u.getAuthVersion();
        }
        return true;
    }

    private Claims extrairClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(chave)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
