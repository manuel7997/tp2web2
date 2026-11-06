package com.example.demo.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata general que aparece en Swagger UI (título, descripción, versión) y
 * el esquema de seguridad "bearerAuth": habilita el botón Authorize para pegar
 * el JWT obtenido en POST /api/auth/login. La documentación de cada endpoint
 * puntual va con @Operation en el controller correspondiente.
 */
@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT obtenido en POST /api/auth/login. Se pega solo el token, sin la palabra Bearer.")
public class OpenApiConfig {

    @Bean
    public OpenAPI demoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TP3 · Catálogo, Favoritos y Listas con seguridad JWT")
                        .description("Catálogo de productos (consumo de una API externa), favoritos y listas "
                                + "(persistidos en PostgreSQL con JPA/Hibernate y migraciones Flyway). "
                                + "/api/auth/** y /api/productos/** son públicos; favoritos y listas requieren "
                                + "autenticación: iniciá sesión en POST /api/auth/login y usá el botón Authorize. "
                                + "Cada lista pertenece a su creador; un ADMIN puede operar sobre todas.")
                        .version("v3"));
    }
}