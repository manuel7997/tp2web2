package com.example.demo.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.dto.auth.LoginRequestDTO;
import com.example.demo.dto.auth.TokenDTO;
import com.example.demo.exception.CredencialesInvalidasException;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import com.example.demo.security.JwtService;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String hashFicticio;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.hashFicticio = passwordEncoder.encode("contrasena-ficticia");
    }

    /**
     * Valida email y contraseña contra el hash guardado y, si coinciden, emite
     * un JWT. Si el email no existe se compara igual contra un hash ficticio,
     * así el tiempo de respuesta no delata qué emails están registrados.
     * Cualquier falla es la misma CredencialesInvalidasException (401).
     */
    public TokenDTO login(LoginRequestDTO request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        Optional<Usuario> usuario = usuarioRepository.buscarPorEmail(email);

        String hash = usuario.map(Usuario::getPasswordHash).orElse(hashFicticio);
        boolean coincide = passwordEncoder.matches(request.password(), hash);

        if (usuario.isEmpty() || !coincide) {
            throw new CredencialesInvalidasException();
        }
                return new TokenDTO(jwtService.generarToken(usuario.get()), "Bearer", jwtService.getExpiracionSegundos());
    }
}