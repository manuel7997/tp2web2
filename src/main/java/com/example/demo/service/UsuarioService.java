package com.example.demo.service;

import com.example.demo.dto.auth.RegistroRequestDTO;
import com.example.demo.dto.auth.UsuarioDTO;
import com.example.demo.exception.ConflictoException;
import com.example.demo.model.Rol;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registra un usuario nuevo con rol USER. La contraseña se guarda
     * hasheada, nunca en texto plano. 409 si el email ya está registrado
     * (si dos registros simultáneos pasan este chequeo, la restricción
     * UNIQUE de la base hace fallar al segundo y también termina en 409).
     */
    public UsuarioDTO registrar(RegistroRequestDTO request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existePorEmail(email)) {
            throw new ConflictoException("Ya existe un usuario registrado con el email " + email);
        }

        Usuario usuario = new Usuario(null, email, passwordEncoder.encode(request.password()), Rol.USER);
        return aDTO(usuarioRepository.guardar(usuario));
    }

    private UsuarioDTO aDTO(Usuario usuario) {
        return new UsuarioDTO(usuario.getId(), usuario.getEmail(), usuario.getRol());
    }
}