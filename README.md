# TP2 · Persistencia, migraciones y arquitectura hexagonal

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

Los tests (`./mvnw test`) arrancan el contexto completo de Spring, así que
también necesitan la base levantada.

### 3. Confirmar que las migraciones corrieron

En el log de arranque aparecen líneas de Flyway del estilo
`Migrating schema "public" to version "1 - create favoritos"` (hasta la
versión 4). Para verificarlo en la base:

```
SELECT version, description, success FROM flyway_schema_history;"
```

Deben verse cuatro filas (V1 a V4) con `success = t`. Flyway crea esa
tabla sola.

## Documentación / Swagger UI

```
http://localhost:8080/swagger-ui.html
```

Muestra los tres grupos (**Productos**, **Favoritos** y **Listas**), cada
endpoint con su `@Operation`. En [`requests/tp2.http`](./requests/tp2.http)
hay una colección con casos de éxito y de error (para la extensión REST
Client de VS Code o IntelliJ).

## Endpoints disponibles

| Recurso | Método | Path | Qué hace | Código de éxito |
|---|---|---|---|---|
| Salud | GET | `/health` | Chequeo de salud básico | 200 |
| Salud | GET | `/ping` | Devuelve `pong`, sin JSON | 200 |
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

Ejemplos con curl:

```
curl -X POST http://localhost:8080/api/listas \
  -H "Content-Type: application/json" -d '{"nombre":"Regalos"}'

curl -X POST http://localhost:8080/api/favoritos \
  -H "Content-Type: application/json" \
  -d '{"productoId":1,"nombreProducto":"Essence Mascara Lash Princess","comentario":"me gustan","listaId":1}'

curl http://localhost:8080/api/listas/1/favoritos

curl -X POST http://localhost:8080/api/listas/1/mover-favoritos \
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