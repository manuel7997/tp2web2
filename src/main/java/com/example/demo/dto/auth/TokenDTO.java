package com.example.demo.dto.auth;

/** Respuesta del login: el JWT y cómo usarlo ("Authorization: Bearer <token>"). */
public record TokenDTO(
        String token,
        String tipo,
        long expiraEnSegundos
) {
}