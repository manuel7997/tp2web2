package com.example.demo.controller;

import com.example.demo.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListaControllerIT extends IntegrationTestBase {

    @Test
    void obtener_listaPropia_devuelve200() {
        String token = registrarYObtenerToken(emailUnico());
        String nombre = nombreUnico("Regalos");
        long id = crearLista(token, nombre);

        Respuesta respuesta = get("/api/listas/" + id, token);

        assertThat(respuesta.status()).isEqualTo(200);
        assertThat(respuesta.body()).contains(nombre);
    }

    @Test
    void obtener_listaAjena_devuelve403() {
        String tokenDuenio = registrarYObtenerToken(emailUnico());
        String tokenOtro = registrarYObtenerToken(emailUnico());
        long id = crearLista(tokenDuenio, nombreUnico("Privada"));

        Respuesta respuesta = get("/api/listas/" + id, tokenOtro);

        assertThat(respuesta.status()).isEqualTo(403);
    }

    @Test
    void listar_sinToken_devuelve401() {
        Respuesta respuesta = get("/api/listas", null);

        assertThat(respuesta.status()).isEqualTo(401);
    }

    @Test
    void listar_tokenInvalido_devuelve401() {
        Respuesta respuesta = get("/api/listas", "esto.no.es.un.jwt");

        assertThat(respuesta.status()).isEqualTo(401);
    }

    @Test
    void obtener_listaAjenaComoAdmin_devuelve200() {
        String tokenDuenio = registrarYObtenerToken(emailUnico());
        String tokenAdmin = crearAdminYObtenerToken();
        String nombre = nombreUnico("Ajena");
        long id = crearLista(tokenDuenio, nombre);

        Respuesta respuesta = get("/api/listas/" + id, tokenAdmin);

        assertThat(respuesta.status()).isEqualTo(200);
        assertThat(respuesta.body()).contains(nombre);
    }

    @Test
    void obtener_listaInexistente_devuelve404() {
        String token = registrarYObtenerToken(emailUnico());

        Respuesta respuesta = get("/api/listas/999999999", token);

        assertThat(respuesta.status()).isEqualTo(404);
    }

    @Test
    void listar_usuarioComun_devuelveSoloSusListas() {
        String tokenAna = registrarYObtenerToken(emailUnico());
        String tokenBeto = registrarYObtenerToken(emailUnico());
        String nombreAna = nombreUnico("DeAna");
        String nombreBeto = nombreUnico("DeBeto");
        crearLista(tokenAna, nombreAna);
        crearLista(tokenBeto, nombreBeto);

        Respuesta respuesta = get("/api/listas", tokenAna);

        assertThat(respuesta.status()).isEqualTo(200);
        assertThat(respuesta.body()).contains(nombreAna).doesNotContain(nombreBeto);
    }
}