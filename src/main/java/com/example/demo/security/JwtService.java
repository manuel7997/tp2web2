package com.example.demo.security;

import com.example.demo.model.Rol;
import com.example.demo.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Emite y valida JWT firmados con HS256. El secreto viene de la variable de entorno JWT_SECRET. */
@Service
public class JwtService {

    private final SecretKey clave;
    private final Duration duracion;

    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.expiration-minutes}") long minutosDeVida) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) debe tener al menos 32 caracteres para firmar con HS256");
        }
        this.clave = Keys.hmacShaKeyFor(bytes);
        this.duracion = Duration.ofMinutes(minutosDeVida);
    }

    /** JWT con el id del usuario en "sub" y el rol (y el email) como claims. */
    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("email", usuario.getEmail())
                .claim("rol", usuario.getRol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave)
                .compact();
    }

    public long getExpiracionSegundos() {
        return duracion.toSeconds();
    }

    /** Devuelve la identidad si el token es válido; vacío si está mal firmado, vencido o malformado. */
    public Optional<UsuarioAutenticado> leerToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long id = Long.valueOf(claims.getSubject());
            String email = claims.get("email", String.class);
            Rol rol = Rol.valueOf(claims.get("rol", String.class));
            return Optional.of(new UsuarioAutenticado(id, email, rol));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}