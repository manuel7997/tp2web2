package com.example.demo.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Spring Data genera la implementación en tiempo de ejecución. */
public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, Long> {

    Optional<UsuarioEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}