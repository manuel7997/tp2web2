package com.example.demo.exception;

/**
 * El usuario está autenticado pero no tiene permiso sobre el recurso
 * (403 Forbidden). No es lo mismo que "no existe" (404).
 */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}