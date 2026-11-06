package com.example.demo.repository.jpa;

import com.example.demo.model.Lista;
import com.example.demo.repository.ListaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Adapter del puerto {@link ListaRepository} sobre JPA/PostgreSQL. */
@Repository
@Transactional(readOnly = true)
public class ListaRepositoryAdapter implements ListaRepository {

    private final ListaJpaRepository listaJpaRepository;
    private final UsuarioJpaRepository usuarioJpaRepository;

    public ListaRepositoryAdapter(ListaJpaRepository listaJpaRepository,
                                  UsuarioJpaRepository usuarioJpaRepository) {
        this.listaJpaRepository = listaJpaRepository;
        this.usuarioJpaRepository = usuarioJpaRepository;
    }

    @Override
    public List<Lista> buscarTodas() {
        return listaJpaRepository.findAll().stream().map(this::aDominio).toList();
    }

    @Override
    public List<Lista> buscarPorUsuarioId(Long usuarioId) {
        return listaJpaRepository.findByUsuarioId(usuarioId).stream().map(this::aDominio).toList();
    }

    @Override
    public Optional<Lista> buscarPorId(Long id) {
        return listaJpaRepository.findById(id).map(this::aDominio);
    }

    @Override
    public boolean existePorId(Long id) {
        return listaJpaRepository.existsById(id);
    }

    @Override
    @Transactional
    public Lista guardar(Lista lista) {
        // Referencia por id, sin consultar al usuario: el id sale del token ya validado.
        ListaEntity guardada = listaJpaRepository.save(new ListaEntity(
                lista.getId(),
                lista.getNombre(),
                usuarioJpaRepository.getReferenceById(lista.getUsuarioId())));
        return aDominio(guardada);
    }

    @Override
    @Transactional
    public void eliminarPorId(Long id) {
        listaJpaRepository.deleteById(id);
    }

    private Lista aDominio(ListaEntity e) {
        return new Lista(e.getId(), e.getNombre(), e.getUsuario().getId());
    }
}