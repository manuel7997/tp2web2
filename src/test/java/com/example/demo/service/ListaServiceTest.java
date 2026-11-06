package com.example.demo.service;

import com.example.demo.dto.lista.ListaDTO;
import com.example.demo.dto.lista.ListaRequestDTO;
import com.example.demo.exception.AccesoDenegadoException;
import com.example.demo.exception.RecursoNoEncontradoException;
import com.example.demo.model.Lista;
import com.example.demo.model.Rol;
import com.example.demo.repository.FavoritoRepository;
import com.example.demo.repository.ListaRepository;
import com.example.demo.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListaServiceTest {

    private static final UsuarioAutenticado ANA = new UsuarioAutenticado(7L, "ana@example.com", Rol.USER);
    private static final UsuarioAutenticado BETO = new UsuarioAutenticado(8L, "beto@example.com", Rol.USER);
    private static final UsuarioAutenticado ADMIN = new UsuarioAutenticado(1L, "admin@example.com", Rol.ADMIN);

    @Mock
    private ListaRepository listaRepository;

    @Mock
    private FavoritoRepository favoritoRepository;

    @Mock
    private FavoritoService favoritoService;

    private ListaService service;

    @BeforeEach
    void preparar() {
        service = new ListaService(listaRepository, favoritoRepository, favoritoService);
    }

    // ---------- crear ----------

    @Test
    void crear_usuarioAutenticado_asociaLaListaAlUsuario() {
        when(listaRepository.guardar(any(Lista.class))).thenAnswer(invocacion -> {
            Lista lista = invocacion.getArgument(0);
            lista.setId(10L);
            return lista;
        });

        ListaDTO creada = service.crear(new ListaRequestDTO("  Regalos  "), ANA);

        ArgumentCaptor<Lista> guardada = ArgumentCaptor.forClass(Lista.class);
        verify(listaRepository).guardar(guardada.capture());
        assertThat(guardada.getValue().getUsuarioId()).isEqualTo(ANA.id());
        assertThat(guardada.getValue().getNombre()).isEqualTo("Regalos");
        assertThat(creada.id()).isEqualTo(10L);
    }

    @Test
    void crear_otroUsuario_asociaLaListaAEseUsuario() {
        when(listaRepository.guardar(any(Lista.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        service.crear(new ListaRequestDTO("Oferta"), BETO);

        ArgumentCaptor<Lista> guardada = ArgumentCaptor.forClass(Lista.class);
        verify(listaRepository).guardar(guardada.capture());
        assertThat(guardada.getValue().getUsuarioId()).isEqualTo(BETO.id());
    }

    // ---------- obtener (404 antes que 403) ----------

    @Test
    void obtener_listaPropia_devuelveLaLista() {
        when(listaRepository.buscarPorId(5L)).thenReturn(Optional.of(new Lista(5L, "Mia", ANA.id())));

        ListaDTO lista = service.obtenerPorId(5L, ANA);

        assertThat(lista.id()).isEqualTo(5L);
        assertThat(lista.nombre()).isEqualTo("Mia");
    }

    @Test
    void obtener_listaAjena_lanzaAccesoDenegado() {
        when(listaRepository.buscarPorId(5L)).thenReturn(Optional.of(new Lista(5L, "DeAna", ANA.id())));

        assertThatThrownBy(() -> service.obtenerPorId(5L, BETO))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void obtener_listaAjenaComoAdmin_devuelveLaLista() {
        when(listaRepository.buscarPorId(5L)).thenReturn(Optional.of(new Lista(5L, "DeAna", ANA.id())));

        ListaDTO lista = service.obtenerPorId(5L, ADMIN);

        assertThat(lista.id()).isEqualTo(5L);
    }

    @Test
    void obtener_listaInexistente_lanzaRecursoNoEncontrado() {
        when(listaRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(99L, ANA))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    // ---------- listar ----------

    @Test
    void listar_usuarioComun_consultaSoloSusListas() {
        when(listaRepository.buscarPorUsuarioId(ANA.id())).thenReturn(List.of(new Lista(1L, "DeAna", ANA.id())));

        List<ListaDTO> listas = service.listar(ANA);

        assertThat(listas).extracting(ListaDTO::nombre).containsExactly("DeAna");
        verify(listaRepository, never()).buscarTodas();
    }

    @Test
    void listar_admin_consultaTodasLasListas() {
        when(listaRepository.buscarTodas()).thenReturn(List.of(
                new Lista(1L, "DeAna", ANA.id()),
                new Lista(2L, "DeBeto", BETO.id())));

        List<ListaDTO> listas = service.listar(ADMIN);

        assertThat(listas).hasSize(2);
        verify(listaRepository, never()).buscarPorUsuarioId(any());
    }

    // ---------- operaciones que escriben: ajena no toca nada ----------

    @Test
    void eliminar_listaAjena_lanzaAccesoDenegadoYNoElimina() {
        when(listaRepository.buscarPorId(5L)).thenReturn(Optional.of(new Lista(5L, "DeAna", ANA.id())));

        assertThatThrownBy(() -> service.eliminar(5L, BETO))
                .isInstanceOf(AccesoDenegadoException.class);

        verify(listaRepository, never()).eliminarPorId(any());
    }

    @Test
    void moverFavoritos_destinoAjeno_lanzaAccesoDenegadoYNoModificaNada() {
        when(listaRepository.buscarPorId(1L)).thenReturn(Optional.of(new Lista(1L, "DeAna", ANA.id())));
        when(listaRepository.buscarPorId(2L)).thenReturn(Optional.of(new Lista(2L, "DeBeto", BETO.id())));

        assertThatThrownBy(() -> service.moverFavoritos(1L, 2L, ANA))
                .isInstanceOf(AccesoDenegadoException.class);

        verify(favoritoRepository, never()).guardar(any());
        verify(listaRepository, never()).eliminarPorId(any());
    }
}