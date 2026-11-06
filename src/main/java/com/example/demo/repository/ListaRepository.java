package com.example.demo.repository;

import com.example.demo.model.Lista;

import java.util.List;
import java.util.Optional;

/**
 * Puerto (contrato) del almacenamiento de listas, con la misma forma que
 * {@link FavoritoRepository}.
 */
public interface ListaRepository {

    List<Lista> buscarTodas();

    List<Lista> buscarPorUsuarioId(Long usuarioId);

    Optional<Lista> buscarPorId(Long id);

    boolean existePorId(Long id);

    Lista guardar(Lista lista);

    void eliminarPorId(Long id);
}