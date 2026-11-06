package com.example.demo.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data genera la implementación en tiempo de ejecución. */
public interface ListaJpaRepository extends JpaRepository<ListaEntity, Long> {

    List<ListaEntity> findByUsuarioId(Long usuarioId);
}