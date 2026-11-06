package com.example.demo.controller;

import com.example.demo.dto.favorito.FavoritoDTO;
import com.example.demo.dto.favorito.FavoritoRequestDTO;
import com.example.demo.service.FavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favoritos")
@Tag(name = "Favoritos", description = "CRUD de productos favoritos, guardados en PostgreSQL; cada favorito pertenece a una lista. Requiere autenticación (token JWT).")
@SecurityRequirement(name = "bearerAuth")
public class FavoritoController {

    private final FavoritoService favoritoService;

    public FavoritoController(FavoritoService favoritoService) {
        this.favoritoService = favoritoService;
    }

    @GetMapping
    @Operation(summary = "Listar favoritos", description = "Devuelve todos los favoritos guardados, de cualquier lista.")
    public List<FavoritoDTO> listar() {
        return favoritoService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un favorito por id", description = "404 si el favorito no existe.")
    public FavoritoDTO obtenerPorId(@PathVariable Long id) {
        return favoritoService.obtenerPorId(id);
    }

    @PostMapping
    @Operation(summary = "Agregar un favorito", description = "Crea un favorito dentro de una lista. 400 si faltan datos obligatorios; 404 si la lista indicada no existe.")
    public ResponseEntity<FavoritoDTO> crear(@Valid @RequestBody FavoritoRequestDTO request) {
        FavoritoDTO creado = favoritoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un favorito existente", description = "404 si el favorito o la lista indicada no existen; 400 si faltan datos obligatorios.")
    public FavoritoDTO actualizar(@PathVariable Long id, @Valid @RequestBody FavoritoRequestDTO request) {
        return favoritoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un favorito", description = "204 si se eliminó; 404 si no existe.")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        favoritoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
