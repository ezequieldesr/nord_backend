package com.nord.nord.controller;

import com.nord.nord.dto.AuthResponse;
import com.nord.nord.dto.LoginRequest;
import com.nord.nord.dto.RegistroRequest;
import com.nord.nord.exception.RegraDeNegocioException;
import com.nord.nord.model.Role;
import com.nord.nord.model.Usuario;
import com.nord.nord.repository.UsuarioRepository;
import com.nord.nord.security.JwtService;
import com.nord.nord.security.LoginAttemptService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public AuthController(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          LoginAttemptService loginAttemptService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registrar(@Valid @RequestBody RegistroRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraDeNegocioException("Este e-mail já está cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(req.nome().trim());
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(req.senha()));
        usuario.setRole(Role.CLIENTE); // quem se cadastra pela rota pública NUNCA vira admin
        usuarioRepository.save(usuario);

        return resposta(usuario);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        String email = req.email().trim().toLowerCase();

        loginAttemptService.verificarBloqueio(email);
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.senha()));
        } catch (AuthenticationException e) {
            loginAttemptService.registrarFalha(email);
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }
        loginAttemptService.registrarSucesso(email);

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos."));
        return resposta(usuario);
    }

    private AuthResponse resposta(Usuario u) {
        return new AuthResponse(jwtService.gerarToken(u), u.getId(), u.getNome(), u.getEmail(), u.getRole());
    }
}
