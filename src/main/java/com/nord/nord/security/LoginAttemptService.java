package com.nord.nord.security;

import com.nord.nord.exception.LimiteTentativasException;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Limite local por conta. Em produção, complementar com limite por IP no proxy. */
@Service
public class LoginAttemptService {
    private static final int MAX_FALHAS = 5;
    private static final Duration JANELA = Duration.ofMinutes(15);
    private static final int LIMITE_DE_CHAVES = 10_000;
    private record Tentativas(int falhas, Instant inicio) {
        boolean expirada() { return !inicio.plus(JANELA).isAfter(Instant.now()); }
    }
    private final Map<String, Tentativas> registro = new HashMap<>();

    public synchronized void verificarBloqueio(String email) {
        registro.entrySet().removeIf(e -> e.getValue().expirada());
        Tentativas t = registro.get(chave(email));
        if ((t != null && t.falhas() >= MAX_FALHAS)
                || (t == null && registro.size() >= LIMITE_DE_CHAVES)) {
            throw new LimiteTentativasException("Muitas tentativas de login. Aguarde alguns minutos e tente de novo.");
        }
    }

    public synchronized void registrarFalha(String email) {
        String chave = chave(email);
        Tentativas atual = registro.get(chave);
        if (atual != null && !atual.expirada()) {
            registro.put(chave, new Tentativas(Math.min(MAX_FALHAS, atual.falhas() + 1), atual.inicio()));
        } else {
            registro.entrySet().removeIf(e -> e.getValue().expirada());
            if (registro.size() < LIMITE_DE_CHAVES) {
                registro.put(chave, new Tentativas(1, Instant.now()));
            }
        }
    }

    public synchronized void registrarSucesso(String email) { registro.remove(chave(email)); }
    private String chave(String email) { return email == null ? "" : email.trim().toLowerCase(Locale.ROOT); }
}