package com.example.demo.exception;

/**
 * El login falló (401 Unauthorized). El mensaje es siempre el mismo, exista
 * o no el email, para no revelar qué usuarios están registrados.
 */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Email o contraseña incorrectos");
    }
}