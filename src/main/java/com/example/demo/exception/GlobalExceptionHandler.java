package com.example.demo.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador centralizado de errores de toda la API. En vez de que cada
 * controller arme su propio JSON de error, cualquier excepción que llegue
 * hasta acá se traduce a un mismo formato (ProblemDetail, RFC 7807):
 * { "status", "title", "detail", ... }.
 *
 * Cuando se implemente el controller de favoritos en clase, no hace falta
 * tocar esta clase: alcanza con que FavoritoService lance
 * RecursoNoEncontradoException o que el DTO de entrada tenga anotaciones
 * de Bean Validation para que estos mismos handlers respondan.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail handleNoEncontrado(RecursoNoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ProblemDetail handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problema.setTitle("Credenciales inválidas");
        return problema;
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ProblemDetail handleAccesoDenegado(AccesoDenegadoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problema.setTitle("Acceso denegado");
        return problema;
    }

    @ExceptionHandler(ConflictoException.class)
    public ProblemDetail handleConflicto(ConflictoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problema.setTitle("Conflicto con el estado actual");
        return problema;
    }

    /** Red de seguridad: si la base rechaza una operación por una restricción (FK, NOT NULL), es un conflicto, no un 500. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegridad(DataIntegrityViolationException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "La operación viola una restricción de integridad de la base de datos");
        problema.setTitle("Conflicto de integridad");
        return problema;
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ProblemDetail handleSolicitudInvalida(SolicitudInvalidaException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problema.setTitle("Solicitud inválida");
        return problema;
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ProblemDetail handleServicioExterno(ServicioExternoException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        problema.setTitle("Falla al consumir un servicio externo");
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.put(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Uno o más campos no son válidos");
        problema.setTitle("Error de validación");
        problema.setProperty("errores", errores);
        return problema;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenerico(Exception ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado");
        problema.setTitle("Error interno");
        return problema;
    }
}
