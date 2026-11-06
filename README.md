# TP2 · Persistencia, migraciones y arquitectura hexagonal

![CI](https://github.com/manuel7997/tp2web2/actions/workflows/ci.yml/badge.svg)

Este repositorio continúa en el **TP3** (seguridad con JWT, pruebas, integración continua y despliegue). Las secciones nuevas son **Seguridad (TP3)**, **Migraciones del TP3**, **Probar los endpoints protegidos**, **Tests**, **Integración continua (CI)** y **Despliegue en Render**.

Evolución del TP1: la API de catálogo y favoritos ahora persiste en
PostgreSQL con Spring Data JPA/Hibernate, versiona el esquema con Flyway y
suma un recurso nuevo (listas) relacionado con favoritos.

La aplicación expone tres grupos de endpoints:

1. Catálogo de productos (`/api/productos`) — de solo lectura, espejo de la
   API externa [DummyJSON](https://dummyjson.com/products). No cambió.
2. Favoritos (`/api/favoritos`) — CRUD completo, guardado en PostgreSQL.
   Cada favorito pertenece a una lista.
3. Listas (`/api/listas`) — recurso nuevo para organizar favoritos.

## Cómo levantar el proyecto

Requiere Java 25 y Docker (o una instalación local de PostgreSQL).

### 1. Levantar PostgreSQL

Con PostgreSQL instalado localmente: crear una base `tp2` y un usuario
`tp2` con contraseña `tp2` desde SQL Shell (psql):

CREATE USER tp2 WITH PASSWORD 'tp2';
CREATE DATABASE tp2 OWNER tp2;

`application.properties` usa esos valores por defecto. Se pueden cambiar
con las variables de entorno `DB_NAME`, `DB_USER`, `DB_PASSWORD`,
`DB_PORT` y `DB_HOST`.

Alternativa con Docker: `docker compose up -d` (usa el `docker-compose.yml`
de la raíz, con los mismos datos de conexión).

### 2. Levantar la aplicación

Usar siempre el wrapper, nunca un `mvn` instalado aparte:

```
# Windows
.\mvnw.cmd spring-boot:run

# macOS/Linux
./mvnw spring-boot:run
```

Cuando el log muestre `Started DemoApplication`, la app queda escuchando en
`http://localhost:8080`.

Para correr los tests (unitarios y de integración) ver la sección **Tests**: ya no necesitan esta base, las de integración levantan la suya con Testcontainers.

### 3. Confirmar que las migraciones corrieron

En el log de arranque aparecen líneas de Flyway del estilo
`Migrating schema "public" to version "1 - create favoritos"` (hasta la
versión 6). Para verificarlo en la base:

```
SELECT version, description, success FROM flyway_schema_history;"
```

Deben verse seis filas (V1 a V6) con `success = t`. Flyway crea esa
tabla sola.

## Documentación / Swagger UI

```
http://localhost:8080/swagger-ui.html
```

Muestra los cuatro grupos (**Autenticación**, **Productos**, **Favoritos** y **Listas**), cada
endpoint con su `@Operation`. En [`requests/tp2.http`](./requests/tp2.http)
hay una colección con casos de éxito y de error (para la extensión REST
Client de VS Code o IntelliJ).

## Endpoints disponibles

| Recurso | Método | Path | Qué hace | Código de éxito |
|---|---|---|---|---|
| Salud | GET | `/health` | Chequeo de salud básico | 200 |
| Salud | GET | `/ping` | Devuelve `pong`, sin JSON | 200 |
| Autenticación | POST | `/api/auth/registro` | Registra un usuario (rol `USER`, contraseña hasheada con BCrypt) | 201 / 400 / 409 |
| Autenticación | POST | `/api/auth/login` | Devuelve un JWT con el id y el rol del usuario | 200 / 400 / 401 |
| Productos | GET | `/api/productos` | Lista el catálogo (consumido de DummyJSON) | 200 |
| Productos | GET | `/api/productos/{id}` | Un producto; 404 si no existe | 200 / 404 |
| Favoritos | POST | `/api/favoritos` | Crea un favorito en una lista; 404 si la lista no existe | 201 / 404 |
| Favoritos | GET | `/api/favoritos` | Lista todos los favoritos | 200 |
| Favoritos | GET | `/api/favoritos/{id}` | Un favorito; 404 si no existe | 200 / 404 |
| Favoritos | PUT | `/api/favoritos/{id}` | Actualiza un favorito; 404 si no existe | 200 / 404 |
| Favoritos | DELETE | `/api/favoritos/{id}` | Elimina un favorito; 404 si no existe | 204 / 404 |
| Listas | POST | `/api/listas` | Crea una lista | 201 |
| Listas | GET | `/api/listas` | Lista las listas | 200 |
| Listas | GET | `/api/listas/{id}` | Una lista; 404 si no existe | 200 / 404 |
| Listas | GET | `/api/listas/{id}/favoritos` | Favoritos de una lista; 404 si no existe | 200 / 404 |
| Listas | DELETE | `/api/listas/{id}` | Elimina una lista vacía; 409 si tiene favoritos | 204 / 404 / 409 |
| Listas | POST | `/api/listas/{origenId}/mover-favoritos` | Mueve todos los favoritos a otra lista y elimina la origen (transaccional) | 200 / 404 / 400 |

Son públicos `/health`, `/ping`, `/api/productos/**` y `/api/auth/**`. Todo lo
demás (`/api/favoritos/**` y `/api/listas/**`) exige un token JWT: sin token, o
con uno inválido o vencido, responde `401`. Sobre las listas rige además la
propiedad: operar una lista de otro usuario responde `403` (salvo que el
usuario sea `ADMIN`), y las listas inexistentes siguen dando `404`.

Ejemplos con curl (en bash; los de favoritos y listas llevan el token del login):

```
curl -X POST http://localhost:8080/api/auth/registro \
  -H "Content-Type: application/json" \
  -d '{"email":"manu@example.com","password":"clave12345"}'

TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"manu@example.com","password":"clave12345"}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')
```

Con el token en `$TOKEN`:

```
curl -X POST http://localhost:8080/api/listas \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"nombre":"Regalos"}'

curl -X POST http://localhost:8080/api/favoritos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productoId":1,"nombreProducto":"Essence Mascara Lash Princess","comentario":"me gustan","listaId":1}'

curl http://localhost:8080/api/listas/1/favoritos \
  -H "Authorization: Bearer $TOKEN"

curl -X POST http://localhost:8080/api/listas/1/mover-favoritos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"listaDestinoId":2}'
```

## Manejo de errores

Formato uniforme (`ProblemDetail`, RFC 7807) desde un
`@RestControllerAdvice` central:

- Recurso inexistente (producto, favorito o lista) → `404 Not Found`.
- Datos de entrada inválidos (Bean Validation) → `400 Bad Request`, con el
  detalle de qué campo falló.
- Mover favoritos con la misma lista como origen y destino → `400`.
- Borrar una lista que todavía tiene favoritos → `409 Conflict`. Una
  violación de integridad que llegue desde la base también se traduce a
  `409` y no a un `500`.
- Falla al consumir DummyJSON (caída o timeout) → `502 Bad Gateway`.

## Qué cambió respecto del TP1 en la capa de persistencia

**Qué clases hubo que tocar para pasar de memoria a JPA** (punto 3):

- Nuevas: `FavoritoEntity`, `FavoritoJpaRepository` y
  `FavoritoRepositoryAdapter` (en `repository/jpa`).
- Eliminada: `FavoritoRepositoryEnMemoria`, para que haya un solo bean que
  implemente `FavoritoRepository` y Spring no tenga dos candidatos a
  inyectar.
- Configuración: dependencias en `pom.xml`, conexión en
  `application.properties` y migraciones en `db/migration`.

**Qué quedó exactamente igual en esa migración:** la interfaz
`FavoritoRepository`, `FavoritoService`, `FavoritoController`, los DTOs de
favoritos y el modelo de dominio `Favorito`. Esto fue posible porque
`FavoritoRepository` es un **puerto**: un contrato que describe qué
necesita el dominio del almacenamiento (buscar, guardar, eliminar) sin
decir cómo se hace. El service solo depende de ese contrato. La versión en
memoria del TP1 y `FavoritoRepositoryAdapter` son dos **adapters**
intercambiables detrás de la misma interfaz: uno guarda en un `Map`, el
otro traduce `Favorito` ↔ `FavoritoEntity` y delega en Spring Data. El
service nunca supo cuál de los dos tenía enfrente, por eso se puede
cambiar la infraestructura sin tocar la lógica.

Después, al sumar las listas (punto 5), sí se agregó `listaId` al dominio,
a los DTOs y al service de favoritos, porque es una regla nueva del
negocio (todo favorito pertenece a una lista que debe existir) y no un
cambio de infraestructura.

## Evolución del esquema: por qué una migración nueva (V4)

Las filas de `favoritos` creadas antes de agregar `lista_id` no tienen
lista, así que la columna no podía nacer `NOT NULL`. Se resolvió con una
migración nueva (`V4__lista_id_obligatorio.sql`: crea la lista
"Sin clasificar", reasigna los favoritos huérfanos y recién después exige
`NOT NULL`) y nunca editando `V1`, `V2` o `V3`. Flyway guarda en
`flyway_schema_history` un checksum de cada migración aplicada; si se
modifica una ya aplicada, el checksum no coincide y Flyway rechaza
arrancar, y además las bases que ya la corrieron no la volverían a
ejecutar, quedando con esquemas distintos. Con una migración nueva el
historial es lineal, repetible en cualquier entorno y cada base avanza
desde la versión en la que está.

## Transacciones: por qué `@Transactional` en mover favoritos

`ListaService.moverFavoritos` hace varias escrituras: actualiza la lista
de cada favorito y después elimina la lista origen. Con `@Transactional`
todo eso es una sola unidad: si una escritura falla a mitad de camino se
hace rollback y no queda aplicada ninguna. Sin la anotación, cada
`save`/`delete` del repositorio sería su propia transacción con su propio
commit. Si fallara el borrado de la lista (o alguno de los últimos
favoritos) después de que otros favoritos ya se movieron y confirmaron,
quedaría la base a medias: parte de los favoritos en la lista destino,
parte en la origen, y la operación reportada como error. Eso rompe la
**atomicidad** de ACID (se hizo "una parte" de una operación que debía ser
todo o nada) y deja datos **inconsistentes** respecto de lo que el usuario
pidió; además, otras consultas concurrentes podrían ver ese estado
intermedio, lo que toca el **aislamiento**.

## Qué está armado

- **`config/`**: `RestClientConfig` (cliente de DummyJSON) y `OpenApiConfig`.
- **`client/dummyjson`**, **`dto/producto`**, **`service/ProductoService`**,
  **`controller/ProductoController`**: catálogo de productos (sin cambios).
- **`model/`**: `Favorito` y `Lista`, dominio sin anotaciones de JPA.
- **`repository/`**: puertos `FavoritoRepository` y `ListaRepository`.
- **`repository/jpa/`**: adapters (`FavoritoRepositoryAdapter`,
  `ListaRepositoryAdapter`), entidades JPA y repositorios de Spring Data.
- **`service/`**: `FavoritoService` y `ListaService`.
- **`controller/`**: `FavoritoController` y `ListaController`.
- **`exception/`**: `GlobalExceptionHandler` y excepciones de negocio.
- **`src/main/resources/db/migration/`**: `V1` favoritos, `V2` listas,
  `V3` columna `lista_id`, `V4` `lista_id` obligatorio.
- **`docker-compose.yml`**: PostgreSQL para desarrollo.

## Evidencia

## Evidencia

Capturas de los casos de éxito y de error probados en el TP2 (listas,
favoritos, mover favoritos, persistencia tras reiniciar y migraciones de
Flyway) en [`EVIDENCIA/tp2/`](./EVIDENCIA/tp2). Las del TP1 siguen en
[`EVIDENCIA/`](./EVIDENCIA).

## Dependencias

- `spring-boot-starter-webmvc` — Spring MVC + Tomcat embebido.
- `spring-boot-starter-validation` — Bean Validation.
- `spring-boot-starter-data-jpa` — Spring Data JPA + Hibernate.
- `postgresql` (runtime) — driver JDBC.
- `spring-boot-starter-flyway` + `flyway-database-postgresql` — migraciones
  (en Spring Boot 4.x `flyway-core` solo no alcanza).
- `springdoc-openapi-starter-webmvc-ui` — Swagger UI / OpenAPI.

## Seguridad (TP3)

La API usa JWT. `POST /api/auth/registro` y `POST /api/auth/login` son públicos y el login devuelve un token. `/api/productos/**` sigue público; `/api/favoritos/**` y `/api/listas/**` exigen `Authorization: Bearer <token>`, y sin token, con uno inválido o vencido responden `401`.

### Dónde vive la regla de autorización

Cada lista tiene un dueño (`listas.usuario_id`, migración `V6`). La regla "una lista solo la opera su dueño, salvo que el usuario sea `ADMIN`" vive en la **capa de servicio**, en un único método: `ListaService.obtenerAutorizada(id, usuario)`. Lo usan todas las operaciones sobre una lista puntual (obtener, favoritos de la lista, eliminar y mover favoritos, en este último caso para las dos listas). `listar` aplica la misma regla filtrando por dueño (o devolviendo todas si es `ADMIN`) y `crear` toma el dueño del usuario autenticado, nunca del body. `obtenerAutorizada` busca la lista (`404` si no existe) y recién después compara el dueño (`403` si no es suya ni es admin).

Por qué ahí y no en otro lado:

- **No en el controller.** El controller solo traduce HTTP a llamadas al servicio. Si la regla estuviera ahí habría que repetirla en cada endpoint, y cualquier otro camino que llame al servicio (otro controller, un job, un test) se la saltearía. En el servicio hay un solo lugar y no se puede esquivar.
- **No en el filtro JWT.** El filtro corre antes de saber qué recurso se pide y solo conoce quién es el usuario (autenticación). Decidir si la lista 7 es suya exige consultar la base y entender la URL, o sea duplicar el ruteo y mezclar una regla de negocio con la infraestructura de seguridad. El filtro y `SecurityConfig` se quedan con lo que depende solo de la ruta y del token: qué es público y qué exige estar autenticado.
- **No con anotaciones (`@PreAuthorize`).** Se podría, pero la regla quedaría escondida en una expresión de texto, más difícil de leer y de probar, y `mover-favoritos` necesita validar dos listas. Además el servicio tiene que cargar la lista de todos modos para distinguir `404` de `403`.
- Al ser Java común, se prueba con tests unitarios sin levantar Spring: lista ajena lanza `AccesoDenegadoException` y `ADMIN` pasa.

El servicio recibe el usuario autenticado como parámetro (el controller lo toma del contexto de seguridad con `@AuthenticationPrincipal`), así no depende de estado global. Responder `403` y no `404` ante una lista ajena distingue "no existe" de "no es tuya", con el costo de que confirma que el id existe.

## Migraciones del TP3 (V5, V6) y el ajuste de V4

- `V5__create_usuarios.sql`: tabla `usuarios` (`id`, `email` único, `password_hash`, `rol` con un `CHECK` que admite `USER` o `ADMIN`).
- `V6__add_usuario_id_a_listas.sql`: agrega `listas.usuario_id` (clave foránea a `usuarios`, `NOT NULL`). Las listas que ya existían pasan al primer usuario registrado y recién después se exige `NOT NULL`.

**Ajuste de V4.** Esta migración se modificó después de aplicada en desarrollo: originalmente creaba siempre la lista "Sin clasificar", incluso en una base vacía. Al sumar `V6` (cada lista necesita dueño), una base nueva quedaba con esa lista y sin ningún usuario al que asignársela, y la migración fallaba. Lo detectaron las pruebas de integración, que parten de una base limpia. Ahora `V4` solo crea la lista si hay favoritos sin lista. Es una excepción a la regla de no editar migraciones ya aplicadas (ver "Evolución del esquema"): es seguro en bases nuevas (pruebas, CI, despliegue), pero una base que ya había corrido la `V4` original se niega a arrancar por checksum distinto, y hay que repararla (actualizando el checksum en `flyway_schema_history`) o recrearla.

## Probar los endpoints protegidos

**En Swagger UI** (`/swagger-ui.html`):

1. `POST /api/auth/registro` con un email y una contraseña de 8 a 72 caracteres.
2. `POST /api/auth/login` con las mismas credenciales y copiar el valor de `token`.
3. Botón **Authorize** (arriba a la derecha), pegar el token sin la palabra `Bearer`. Los candados de **Favoritos** y **Listas** quedan activos.

**Con la colección** [`requests/tp3.http`](./requests/tp3.http) (extensión REST Client): registro, login, `401` sin token o con token inválido, creación de una lista, `404` y `403` al pedir la lista de otro usuario.

Los usuarios siempre se registran con rol `USER`. Para probar `ADMIN` se cambia el rol en la base (`UPDATE usuarios SET rol = 'ADMIN' WHERE email = '...';`) y se vuelve a iniciar sesión, porque el rol viaja dentro del token.

## Tests

```
# Windows
.\mvnw.cmd test       # solo las pruebas unitarias
.\mvnw.cmd verify     # unitarias + integración

# macOS/Linux
./mvnw test
./mvnw verify
```

El sufijo del nombre decide qué plugin de Maven ejecuta cada clase:

- **`*Test` (Surefire, `mvn test`)**: pruebas unitarias con Mockito, sin Spring ni base de datos. `ListaServiceTest` (la lista creada queda asociada al usuario autenticado, lista ajena → `AccesoDenegadoException`, `ADMIN` pasa, `404` antes que `403`, mover o eliminar una lista ajena no modifica nada) y `UsuarioServiceTest` (nunca se guarda la contraseña en texto plano: se guarda un hash BCrypt; rol `USER` por defecto; email repetido → conflicto).
- **`*IT` (Failsafe, `mvn verify`)**: pruebas de integración. Arrancan la aplicación completa en un puerto aleatorio contra un **PostgreSQL real que Testcontainers levanta en un contenedor Docker** (`postgres:17-alpine`). La base nace vacía en cada ejecución y Flyway corre todas las migraciones, por lo que no tocan la base local. `AuthControllerIT` (login válido → `200` con token; credenciales inválidas → `401`; registro repetido → `409`), `ListaControllerIT` (lista propia → `200`; ajena → `403`; sin token o con token inválido → `401`; `ADMIN` sobre lista ajena → `200`; inexistente → `404`; cada usuario ve solo sus listas) y `DemoApplicationIT` (la aplicación arranca y Flyway aplica todas las migraciones).

Las pruebas de integración **necesitan Docker en ejecución** (Docker Desktop en Windows). Sin Docker fallan con `Could not find a valid Docker environment`. Los reportes quedan en `target/surefire-reports` (unitarias) y `target/failsafe-reports` (integración). Los avisos de Mockito sobre "self-attaching" y agentes dinámicos son esperables y no indican un problema.

## Integración continua (CI)

El workflow [`.github/workflows/ci.yml`](./.github/workflows/ci.yml) corre en cada `push` (y a mano desde la pestaña Actions): descarga el código, instala Java 25 (Temurin, con caché de Maven) y ejecuta `./mvnw -B -ntp verify`, o sea compila y corre las pruebas unitarias (Surefire) y las de integración (Failsafe). El runner de GitHub ya trae Docker, así que Testcontainers funciona sin configurar nada, y no hace falta una base preinstalada.

**Cómo leer el resultado:**

1. En el repositorio, pestaña **Actions**: cada ejecución muestra el mensaje del commit, la rama y su estado. Verde es éxito, rojo es fallo y amarillo es en curso. El mismo ícono aparece al lado de cada commit y en la insignia de arriba.
2. Entrar a la ejecución, abrir el job **Compilar y probar** y expandir el paso **Compilar y correr las pruebas**.
3. Si pasó, el log muestra el bloque de Surefire (`Tests run: 13, Failures: 0, Errors: 0` al momento de escribir esto), el de Failsafe (`Tests run: 12, ...`) y `BUILD SUCCESS`.
4. Si falló, buscar la primera línea `[ERROR]`: nombra el test (por ejemplo `UsuarioServiceTest.registrar_...`) con el valor esperado y el obtenido, y termina en `BUILD FAILURE`. Si falla una prueba unitaria, Maven corta antes de las de integración, así que no aparece el bloque de Failsafe.
5. Si el paso de Maven falla con `Permission denied`, el wrapper perdió el permiso de ejecución: `git update-index --chmod=+x mvnw` y subir el cambio.

**Cómo comprobar que el CI detecta errores:** romper un test a propósito, hacer `push` y ver la ejecución en rojo; después `git revert --no-edit HEAD`, `push` y ver que vuelve a verde.

## Despliegue en Render

URL pública: https://<tu-servicio>.onrender.com (por ejemplo `https://<tu-servicio>.onrender.com/health`). Es una instancia gratuita: se duerme tras 15 minutos sin tráfico y la primera petición después tarda cerca de un minuto en responder.

**Archivos que lo hacen posible.** Render no ejecuta Java directamente, así que se despliega con un `Dockerfile` de dos etapas (compila con `./mvnw package` en una imagen JDK 25 y ejecuta el jar en una imagen JRE 25, con un heap limitado a la memoria de la instancia gratuita). `server.port=${PORT:8080}` en `application.properties` hace que la app escuche en el puerto que Render le indica; en local sigue siendo 8080.

**Pasos que se siguieron:**

1. En Render, **New + → PostgreSQL**: plan **Free**, versión **17** (la misma de las pruebas), en una región elegida. Cuando figura **Available**, se copian los datos de la pestaña Info: **Hostname** interno (con la forma `dpg-xxxxxxxx-a`), Port, Database, Username y Password.
2. **New + → Web Service**, conectando este repositorio, con **Language: Docker**, el mismo branch que se despliega, la **misma región que la base** (con otra región no resuelve el hostname interno) y plan **Free**.
3. Variables de entorno del servicio (solo el valor, sin comillas ni texto de ejemplo):
   - `DB_HOST`: el hostname interno de la base.
   - `DB_PORT`: `5432`.
   - `DB_NAME`, `DB_USER`, `DB_PASSWORD`: los datos de la base.
   - `JWT_SECRET`: un secreto aleatorio de 48 bytes en base64, generado con PowerShell: `$b = New-Object byte[] 48; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)`. No se guarda en el repositorio: si faltara, la app usaría el secreto de desarrollo, que es público.
4. En **Advanced**, **Health Check Path** = `/health`, y **Create Web Service**. Cada `push` al branch redespliega solo.

**Cómo se verificó:**

- En **Logs** del servicio: `Successfully applied 6 migrations to schema "public", now at version v6` (las migraciones corrieron en la base remota), `Started DemoApplication` y ninguna línea `ERROR`. El estado del servicio pasa a **Live**.
- `GET https://<tu-servicio>.onrender.com/health` responde `{"status":"ok","service":"demo"}`.
- Registro y login contra la URL pública, `GET /api/listas` con el token → `200` y sin token → `401`.

**Problema que apareció:** el primer despliegue falló con `UnknownHostException` porque `DB_HOST` quedó con un texto de ejemplo en vez del hostname real. Los errores de conexión a la base se ven en los logs justo antes de que Flyway intente migrar.

**Limitaciones del plan gratuito:** la base expira a los 30 días de creada (hay un período de gracia de 14 días antes de que se borre) y solo se permite una base gratuita por cuenta. La instancia tiene 512 MB de RAM y 0,1 CPU, por eso el arranque es lento.

## Evidencia del TP3

Capturas en [`EVIDENCIA/tp3/`](./EVIDENCIA/tp3): pruebas unitarias y de integración en verde (`./mvnw verify`), CI en rojo con un test roto a propósito y de nuevo en verde, Swagger UI con el botón Authorize, y el despliegue en Render (servicio **Live**, variables de entorno con los secretos tapados, logs de Flyway y `/health` en la URL pública).

## Limitaciones conocidas

- La propiedad de los recursos (`403` si no es tuyo) se valida en las listas, que es lo que pide la consigna. **Favoritos** exige token pero no valida de quién es cada favorito: cualquier usuario autenticado puede verlos o modificarlos.
- El JWT es stateless: no se puede revocar antes de que venza (60 minutos por defecto, configurable con `JWT_EXPIRATION_MINUTES`).
- Un usuario `ADMIN` solo se crea cambiando el rol en la base.