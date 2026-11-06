package com.example.demo.dto.auth;

import com.example.demo.model.Rol;

/** Lo que la API devuelve de un usuario. Nunca incluye la contraseña ni su hash. */
public record UsuarioDTO(
        Long id,
        String email,
        Rol rol
) {
}