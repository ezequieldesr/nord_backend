package com.nord.nord.config;

import com.nord.nord.model.Role;
import com.nord.nord.model.Usuario;
import com.nord.nord.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria o primeiro administrador ao subir a aplicação, se app.admin.email/password estiverem preenchidos.
 * Em produção os valores vêm de variáveis de ambiente (ADMIN_EMAIL / ADMIN_PASSWORD).
 */
@Slf4j
@Component
public class AdminSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;

    public AdminSeeder(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.admin.email:}") String email,
                       @Value("${app.admin.password:}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(String... args) {
        if (email.isBlank() || senha.isBlank()) {
            return;
        }
        if (senha.length() < 8) {
            log.warn("ADMIN_PASSWORD precisa ter ao menos 8 caracteres. Admin não foi criado.");
            return;
        }
        String emailNormalizado = email.trim().toLowerCase();
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setNome("Administrador");
        admin.setEmail(emailNormalizado);
        admin.setSenha(passwordEncoder.encode(senha));
        admin.setRole(Role.ADMIN);
        usuarioRepository.save(admin);
        log.info("Administrador criado: {}", emailNormalizado);
    }
}
