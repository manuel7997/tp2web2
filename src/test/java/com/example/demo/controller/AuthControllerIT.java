package com.example.demo.controller;

import com.example.demo.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerIT extends IntegrationTestBase {

    @Test
    void login_credencialesValidas_devuelve200ConToken() {
        String email = emailUnico();
        assertThat(post("/api/auth/registro", credenciales(email, PASSWORD), null).status()).isEqualTo(201);

        Respuesta respuesta = post("/api/auth/login", credenciales(email, PASSWORD), null);

        assertThat(respuesta.status()).isEqualTo(200);
        assertThat(extraerTexto(respuesta.body(), "token").split("\\.")).hasSize(3); // encabezado.datos.firma
        assertThat(extraerTexto(respuesta.body(), "tipo")).isEqualTo("Bearer");
    }

    @Test
    void login_contrasenaIncorrecta_devuelve401() {
        String email = emailUnico();
        post("/api/auth/registro", credenciales(email, PASSWORD), null);

        Respuesta respuesta = post("/api/auth/login", credenciales(email, "otraClave999"), null);

        assertThat(respuesta.status()).isEqualTo(401);
    }

    @Test
    void login_emailInexistente_devuelve401() {
        Respuesta respuesta = post("/api/auth/login", credenciales(emailUnico(), PASSWORD), null);

        assertThat(respuesta.status()).isEqualTo(401);
    }

    @Test
    void registro_emailRepetido_devuelve409() {
        String email = emailUnico();
        assertThat(post("/api/auth/registro", credenciales(email, PASSWORD), null).status()).isEqualTo(201);

        Respuesta repetido = post("/api/auth/registro", credenciales(email, PASSWORD), null);

        assertThat(repetido.status()).isEqualTo(409);
    }
}