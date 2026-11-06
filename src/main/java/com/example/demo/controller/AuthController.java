package com.example.demo.controller;

import com.example.demo.dto.auth.LoginRequestDTO;
import com.example.demo.dto.auth.RegistroRequestDTO;
import com.example.demo.dto.auth.TokenDTO;
import com.example.demo.dto.auth.UsuarioDTO;
import com.example.demo.service.AuthService;
import com.example.demo.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@Tag(name = "Autenticación", description = "Registro e inicio de sesión. Públicos: no requieren token.")
public class AuthController {

    private final UsuarioService usuarioService;
    private final AuthService authService;

    public AuthController(UsuarioService usuarioService, AuthService authService) {
        this.usuarioService = usuarioService;
        this.authService = authService;
    }

    @PostMapping("/registro")
    @Operation(summary = "Registrar un usuario",
            description = "Crea un usuario con rol USER. La contraseña (8 a 72 caracteres) se guarda hasheada con BCrypt, "
                    + "nunca en texto plano. El email se normaliza a minúsculas.")
    @ApiResponse(responseCode = "201", description = "Usuario creado (id, email y rol; nunca la contraseña)")
    @ApiResponse(responseCode = "400", description = "Email con formato inválido o contraseña fuera de 8 a 72 caracteres")
    @ApiResponse(responseCode = "409", description = "Ya existe un usuario con ese email")
    public ResponseEntity<UsuarioDTO> registrar(@Valid @RequestBody RegistroRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrar(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión",
            description = "Valida email y contraseña y devuelve un JWT firmado con el id y el rol del usuario. "
                    + "Se usa como \"Authorization: Bearer <token>\" en favoritos y listas (en Swagger, botón Authorize).")
    @ApiResponse(responseCode = "200", description = "Credenciales correctas: token JWT, tipo y segundos hasta que vence")
    @ApiResponse(responseCode = "400", description = "Falta el email o la contraseña")
    @ApiResponse(responseCode = "401", description = "Email o contraseña incorrectos (el mensaje es el mismo exista o no el email)")
    public TokenDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.login(request);
    }
}