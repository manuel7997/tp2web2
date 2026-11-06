package com.example.demo;

import com.example.demo.model.Rol;
import com.example.demo.model.Usuario;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base de las pruebas de integración: levanta la aplicación completa en un
 * puerto aleatorio contra un PostgreSQL real que Testcontainers arranca en
 * un contenedor Docker. La base nace vacía en cada ejecución y Flyway corre
 * todas las migraciones al iniciar el contexto.
 *
 * El contenedor es un singleton (se arranca una vez por JVM y lo limpia
 * Testcontainers al terminar) porque Spring reutiliza el contexto entre
 * clases de prueba: si cada clase arrancara y frenara su propio contenedor,
 * el contexto cacheado quedaría apuntando a una base que ya no existe.
 *
 * Como todas las clases comparten la base, cada prueba usa emails y nombres
 * únicos y no depende de lo que dejó otra.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "clave12345";

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void propiedadesDeLaBase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    private static final HttpClient CLIENTE = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    @Value("${local.server.port}")
    private int puerto;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** Status y cuerpo de una respuesta HTTP. */
    public record Respuesta(int status, String body) {
    }

    // ---------- HTTP ----------

    protected Respuesta get(String ruta, String token) {
        return enviar("GET", ruta, null, token);
    }

    protected Respuesta post(String ruta, String json, String token) {
        return enviar("POST", ruta, json, token);
    }

    private Respuesta enviar(String metodo, String ruta, String json, String token) {
        HttpRequest.Builder pedido = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .header("Content-Type", "application/json");
        if (token != null) {
            pedido.header("Authorization", "Bearer " + token);
        }
        pedido.method(metodo, json == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json));
        try {
            HttpResponse<String> respuesta = CLIENTE.send(pedido.build(), HttpResponse.BodyHandlers.ofString());
            return new Respuesta(respuesta.statusCode(), respuesta.body());
        } catch (IOException e) {
            throw new IllegalStateException("Falló el pedido " + metodo + " " + ruta, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Pedido interrumpido: " + metodo + " " + ruta, e);
        }
    }

    // ---------- Datos de prueba ----------

    protected static String emailUnico() {
        return "usuario-" + UUID.randomUUID() + "@example.com";
    }

    protected static String nombreUnico(String prefijo) {
        return prefijo + "-" + UUID.randomUUID();
    }

    protected static String credenciales(String email, String password) {
        return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
    }

    /** Registra un usuario (rol USER) y devuelve su token. */
    protected String registrarYObtenerToken(String email) {
        Respuesta registro = post("/api/auth/registro", credenciales(email, PASSWORD), null);
        assertThat(registro.status()).as("registro de %s", email).isEqualTo(201);
        return tokenDe(email);
    }

    /** Crea directamente en la base un usuario con rol ADMIN (por la API todos nacen USER) y devuelve su token. */
    protected String crearAdminYObtenerToken() {
        String email = emailUnico();
        usuarioRepository.guardar(new Usuario(null, email, passwordEncoder.encode(PASSWORD), Rol.ADMIN));
        return tokenDe(email);
    }

    protected String tokenDe(String email) {
        Respuesta login = post("/api/auth/login", credenciales(email, PASSWORD), null);
        assertThat(login.status()).as("login de %s", email).isEqualTo(200);
        return extraerTexto(login.body(), "token");
    }

    /** Crea una lista con el token dado y devuelve su id. */
    protected long crearLista(String token, String nombre) {
        Respuesta respuesta = post("/api/listas", "{\"nombre\":\"%s\"}".formatted(nombre), token);
        assertThat(respuesta.status()).as("crear lista %s", nombre).isEqualTo(201);
        return Long.parseLong(extraerNumero(respuesta.body(), "id"));
    }

    // ---------- Lectura minima de JSON (sin depender de una librería) ----------

    protected static String extraerTexto(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        assertThat(m.find()).as("campo de texto '%s' en %s", campo, json).isTrue();
        return m.group(1);
    }

    protected static String extraerNumero(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*(\\d+)").matcher(json);
        assertThat(m.find()).as("campo numérico '%s' en %s", campo, json).isTrue();
        return m.group(1);
    }
}