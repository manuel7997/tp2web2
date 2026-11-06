package com.example.demo.controller;

import com.example.demo.dto.favorito.FavoritoDTO;
import com.example.demo.dto.lista.ListaDTO;
import com.example.demo.dto.lista.ListaRequestDTO;
import com.example.demo.dto.lista.MoverFavoritosRequestDTO;
import com.example.demo.dto.lista.MoverFavoritosResponseDTO;
import com.example.demo.security.UsuarioAutenticado;
import com.example.demo.service.ListaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * El controller solo traduce HTTP: toma el usuario autenticado del token y
 * se lo pasa al service, que es quien decide si puede o no operar la lista.
 */
@RestController
@RequestMapping("/api/listas")
@Tag(name = "Listas", description = "Listas para organizar favoritos (por ejemplo \"Regalos\"). Requieren token; cada lista pertenece a su creador.")
public class ListaController {

    private final ListaService listaService;

    public ListaController(ListaService listaService) {
        this.listaService = listaService;
    }

    @PostMapping
    @Operation(summary = "Crear una lista",
            description = "Crea una lista cuyo dueño es el usuario autenticado. 400 si el nombre está vacío o supera los 100 caracteres.")
    public ResponseEntity<ListaDTO> crear(@Valid @RequestBody ListaRequestDTO request,
                                          @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(listaService.crear(request, usuario));
    }

    @GetMapping
    @Operation(summary = "Listar listas", description = "Devuelve las listas del usuario autenticado (todas, si es ADMIN).")
    public List<ListaDTO> listar(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return listaService.listar(usuario);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una lista por id", description = "404 si la lista no existe; 403 si es de otro usuario.")
    public ListaDTO obtenerPorId(@PathVariable Long id,
                                 @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return listaService.obtenerPorId(id, usuario);
    }

    @GetMapping("/{id}/favoritos")
    @Operation(summary = "Favoritos de una lista",
            description = "Devuelve los favoritos que pertenecen a la lista. 404 si la lista no existe; 403 si es de otro usuario.")
    public List<FavoritoDTO> favoritosDeLista(@PathVariable Long id,
                                              @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return listaService.favoritosDeLista(id, usuario);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una lista vacía",
            description = "204 si se eliminó; 404 si no existe; 403 si es de otro usuario; 409 si todavía tiene favoritos.")
    public ResponseEntity<Void> eliminar(@PathVariable Long id,
                                         @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        listaService.eliminar(id, usuario);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{origenId}/mover-favoritos")
    @Operation(summary = "Mover favoritos a otra lista y eliminar la lista origen",
            description = "Reasigna todos los favoritos de la lista origen a la lista destino y elimina la origen, "
                    + "todo en una única transacción (o se hace todo o no se hace nada). "
                    + "404 si alguna de las dos listas no existe; 403 si alguna es de otro usuario; "
                    + "400 si origen y destino son la misma.")
    public MoverFavoritosResponseDTO moverFavoritos(@PathVariable Long origenId,
                                                   @Valid @RequestBody MoverFavoritosRequestDTO request,
                                                   @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return listaService.moverFavoritos(origenId, request.listaDestinoId(), usuario);
    }
}