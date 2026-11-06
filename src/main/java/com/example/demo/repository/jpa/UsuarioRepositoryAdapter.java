package com.example.demo.repository.jpa;

import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Adapter del puerto {@link UsuarioRepository} sobre JPA/PostgreSQL. */
@Repository
@Transactional(readOnly = true)
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository usuarioJpaRepository;

    public UsuarioRepositoryAdapter(UsuarioJpaRepository usuarioJpaRepository) {
        this.usuarioJpaRepository = usuarioJpaRepository;
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarioJpaRepository.findById(id).map(this::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioJpaRepository.findByEmail(email).map(this::aDominio);
    }

    @Override
    public boolean existePorEmail(String email) {
        return usuarioJpaRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public Usuario guardar(Usuario usuario) {
        UsuarioEntity guardado = usuarioJpaRepository.save(
                new UsuarioEntity(usuario.getId(), usuario.getEmail(), usuario.getPasswordHash(), usuario.getRol()));
        return aDominio(guardado);
    }

    private Usuario aDominio(UsuarioEntity e) {
        return new Usuario(e.getId(), e.getEmail(), e.getPasswordHash(), e.getRol());
    }
}