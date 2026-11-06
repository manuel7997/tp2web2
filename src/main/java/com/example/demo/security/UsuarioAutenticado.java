package com.example.demo.security;

import com.example.demo.model.Rol;

/**
 * Identidad del usuario que hace el request, extraída del JWT. Queda
 * disponible en el SecurityContext (es el "principal" de la autenticación).
 */
public record UsuarioAutenticado(
        Long id,
        String email,
        Rol rol
) {
}