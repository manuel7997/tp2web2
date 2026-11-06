package com.example.demo.repository;

import com.example.demo.model.Usuario;

import java.util.Optional;

/**
 * Puerto (contrato) del almacenamiento de usuarios, con la misma forma que
 * {@link FavoritoRepository} y {@link ListaRepository}. La implementación
 * concreta es {@code UsuarioRepositoryAdapter} (JPA + PostgreSQL).
 */
public interface UsuarioRepository {

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorEmail(String email);

    boolean existePorEmail(String email);

    Usuario guardar(Usuario usuario);
}