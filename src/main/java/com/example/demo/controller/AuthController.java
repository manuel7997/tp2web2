package com.example.demo.controller;

import com.example.demo.dto.auth.LoginRequestDTO;
import com.example.demo.dto.auth.RegistroRequestDTO;
import com.example.demo.dto.auth.TokenDTO;
import com.example.demo.dto.auth.UsuarioDTO;
import com.example.demo.service.AuthService;
import com.example.demo.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro e inicio de sesión (públicos, no requieren token)")
public class AuthController {

    private final UsuarioService usuarioService;
    private final AuthService authService;

    public AuthController(UsuarioService usuarioService, AuthService authService) {
        this.usuarioService = usuarioService;
        this.authService = authService;
    }

    @PostMapping("/registro")
    @Operation(summary = "Registrar un usuario",
            description = "Crea un usuario con rol USER y la contraseña hasheada. "
                    + "400 si el email o la contraseña no son válidos; 409 si el email ya está registrado.")
    public ResponseEntity<UsuarioDTO> registrar(@Valid @RequestBody RegistroRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrar(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
            description = "Valida email y contraseña y devuelve un JWT con el id y el rol del usuario. "
                    + "Se usa como \"Authorization: Bearer <token>\". 401 si las credenciales son incorrectas.")
    public TokenDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.login(request);
    }
}