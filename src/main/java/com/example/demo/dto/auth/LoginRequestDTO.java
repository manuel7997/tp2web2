package com.example.demo.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(

        @NotBlank(message = "El email es obligatorio")
        String email,

        // Máximo 72: BCrypt no admite contraseñas más largas.
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(max = 72, message = "La contraseña no puede superar los 72 caracteres")
        String password
) {
}