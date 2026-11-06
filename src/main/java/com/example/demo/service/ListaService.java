package com.example.demo.service;

import com.example.demo.dto.favorito.FavoritoDTO;
import com.example.demo.dto.lista.ListaDTO;
import com.example.demo.dto.lista.ListaRequestDTO;
import com.example.demo.dto.lista.MoverFavoritosResponseDTO;
import com.example.demo.exception.AccesoDenegadoException;
import com.example.demo.exception.ConflictoException;
import com.example.demo.exception.RecursoNoEncontradoException;
import com.example.demo.exception.SolicitudInvalidaException;
import com.example.demo.model.Favorito;
import com.example.demo.model.Lista;
import com.example.demo.model.Rol;
import com.example.demo.repository.FavoritoRepository;
import com.example.demo.repository.ListaRepository;
import com.example.demo.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso de las listas. La regla de autorización vive acá: una lista
 * solo la opera su dueño, salvo que el usuario sea ADMIN. Todas las
 * operaciones sobre una lista puntual pasan por {@link #obtenerAutorizada}.
 */
@Service
public class ListaService {

    private final ListaRepository listaRepository;
    private final FavoritoRepository favoritoRepository;
    private final FavoritoService favoritoService;

    public ListaService(ListaRepository listaRepository,
                        FavoritoRepository favoritoRepository,
                        FavoritoService favoritoService) {
        this.listaRepository = listaRepository;
        this.favoritoRepository = favoritoRepository;
        this.favoritoService = favoritoService;
    }

    /** El dueño es siempre el usuario autenticado: nunca un dato que mande el cliente. */
    public ListaDTO crear(ListaRequestDTO request, UsuarioAutenticado usuario) {
        Lista lista = new Lista(null, request.nombre().trim(), usuario.id());
        return aDTO(listaRepository.guardar(lista));
    }

    /** Un usuario ve solo sus listas; ADMIN las ve todas. */
    public List<ListaDTO> listar(UsuarioAutenticado usuario) {
        List<Lista> listas = esAdmin(usuario)
                ? listaRepository.buscarTodas()
                : listaRepository.buscarPorUsuarioId(usuario.id());
        return listas.stream()
                .map(this::aDTO)
                .toList();
    }

    public ListaDTO obtenerPorId(Long id, UsuarioAutenticado usuario) {
        return aDTO(obtenerAutorizada(id, usuario));
    }

    public List<FavoritoDTO> favoritosDeLista(Long id, UsuarioAutenticado usuario) {
        obtenerAutorizada(id, usuario);
        return favoritoService.listarPorLista(id);
    }

    /**
     * Elimina una lista solo si está vacía. Si todavía tiene favoritos
     * responde 409 en vez de dejar que la base falle con un 500.
     */
    @Transactional
    public void eliminar(Long id, UsuarioAutenticado usuario) {
        obtenerAutorizada(id, usuario);
        if (favoritoRepository.existePorListaId(id)) {
            throw new ConflictoException(
                    "No se puede eliminar la lista " + id + ": todavía tiene favoritos");
        }
        listaRepository.eliminarPorId(id);
    }

    /**
     * Reasigna todos los favoritos de la lista origen a la lista destino y
     * elimina la lista origen. Es una operación atómica: con
     * {@code @Transactional}, si cualquier escritura falla se hace rollback
     * de todas las anteriores y la base queda como estaba. Las dos listas
     * tienen que ser del usuario (o ser ADMIN): no se puede mover favoritos
     * hacia ni desde una lista ajena.
     */
    @Transactional
    public MoverFavoritosResponseDTO moverFavoritos(Long origenId, Long destinoId, UsuarioAutenticado usuario) {
        if (origenId.equals(destinoId)) {
            throw new SolicitudInvalidaException("La lista origen y la lista destino no pueden ser la misma");
        }
        obtenerAutorizada(origenId, usuario);
        obtenerAutorizada(destinoId, usuario);

        List<Favorito> favoritos = favoritoRepository.buscarPorListaId(origenId);
        for (Favorito favorito : favoritos) {
            favorito.setListaId(destinoId);
            favoritoRepository.guardar(favorito);
        }
        listaRepository.eliminarPorId(origenId);

        return new MoverFavoritosResponseDTO(origenId, destinoId, favoritos.size());
    }

    /**
     * Punto único de la regla de propiedad. Primero comprueba que la lista
     * exista (404) y después que sea del usuario o que este sea ADMIN (403),
     * para distinguir "no existe" de "no es tuya".
     */
    private Lista obtenerAutorizada(Long id, UsuarioAutenticado usuario) {
        Lista lista = listaRepository.buscarPorId(id)
                .orElseThrow(() -> noExiste(id));
        if (!esAdmin(usuario) && !lista.getUsuarioId().equals(usuario.id())) {
            throw new AccesoDenegadoException("No tiene permiso para acceder a la lista " + id);
        }
        return lista;
    }

    private boolean esAdmin(UsuarioAutenticado usuario) {
        return usuario.rol() == Rol.ADMIN;
    }

    private RecursoNoEncontradoException noExiste(Long id) {
        return new RecursoNoEncontradoException("No existe una lista con id " + id);
    }

    private ListaDTO aDTO(Lista lista) {
        return new ListaDTO(lista.getId(), lista.getNombre());
    }
}