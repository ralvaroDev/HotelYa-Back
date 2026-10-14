# Catálogo HotelYa

Backend del catálogo y del panel de dueños de hoteles. Utiliza Spring Boot 4.1.1, Java 25, Gradle, Spring Data JPA, PostgreSQL y Flyway. Los tipos Temática, Matrimonial y Premium son productos del hotel, no habitaciones físicas. El dueño configura cantidad total y disponible por tipo; las reservas quedan fuera de esta versión.

## Arranque local

Requisitos: Java 25, Docker en ejecución, Supabase CLI y Python 3 para comprobar puertos. El script descarga las imágenes oficiales de Supabase desde Docker Hub; se puede seleccionar otro espejo mediante `SUPABASE_INTERNAL_IMAGE_REGISTRY`.

Desde `HotelYa-Back`:

```bash
./scripts/run-local.sh
```

`run-local.sh` comprueba Docker y los contenedores de HotelYa: si Supabase ya está activo lo omite; si hace falta, lo inicia y espera sus comprobaciones de salud. Si el backend de esta carpeta ya está activo, también omite su arranque. El script carga el archivo privado `.env`, exporta sus valores con `set -a` y ejecuta la tarea estándar `bootRun` con el perfil `local`. Java hereda esas variables y Spring Boot las lee mediante las referencias de `application.properties`. El backend y Gradle no leen archivos `.env` por su cuenta.

El archivo `.env` está excluido de Git y no se incluye en el JAR. En una copia nueva, crearlo manualmente en la raíz de `HotelYa-Back` usando la tabla de configuración. Su sintaxis debe ser compatible con Bash; por ejemplo:

```bash
HOTELYA_DB_URL='jdbc:postgresql://127.0.0.1:55329/postgres?sslmode=disable'
HOTELYA_DB_USER='postgres.pooler-dev'
HOTELYA_DB_PASSWORD='postgres'
HOTELYA_DB_SCHEMA='hotelya'
HOTELYA_CORS_ORIGINS='http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173'
SERVER_PORT='8080'
```

Estos valores corresponden exclusivamente a Supabase local. El script informa si falta `.env` y no imprime sus valores. Los valores definidos en ese archivo reemplazan las variables de igual nombre heredadas por el script.

### Botón Ejecutar en IntelliJ IDEA / Android Studio

1. Abrir la carpeta `HotelYa-Back` como proyecto Gradle y seleccionar JDK 25.
2. En **Run → Edit Configurations → + → Shell Script**, seleccionar el archivo `scripts/run-local.sh` y el intérprete Bash. Guardar como `HotelYa local` y usar el botón ▶. La opción Shell Script requiere el soporte correspondiente del IDE.
3. Si el IDE no ofrece Shell Script, usar su terminal y ejecutar `./scripts/run-local.sh`.

El script inicia la base de datos cuando hace falta. Para apagar backend y Supabase juntos, ejecutar `./scripts/stop-local.sh` desde otra terminal. Ctrl+C o Stop del IDE detiene el backend; Supabase se conserva activo hasta ejecutar el script de parada.

Para usar **Gradle → Tasks → application → bootRun** directamente, configurar las variables en **Environment variables** y añadir `--args="--spring.profiles.active=local"` en **Tasks and arguments**. Esa ejecución directa no carga `.env`.

También se puede ejecutar el `main` de `HotelyaBackApplication` con su botón ▶: configurar las variables en **Environment variables**, JDK 25 y `--spring.profiles.active=local` en **Program arguments**.

### Dónde guardar la configuración

- `src/main/resources/application.properties`: opciones compartidas y referencias a variables, sin credenciales reales.
- `.env`: valores privados de desarrollo; solo los scripts externos lo cargan.
- Entorno del proceso: variables proporcionadas por la terminal, el IDE, Docker o la plataforma del servidor. Para producción, usar la configuración de secretos de esa plataforma.
- `gradle.properties`: configuración de Gradle; no se usa para las credenciales de ejecución del backend.

Referencias: [configuración externa de Spring Boot](https://docs.spring.io/spring-boot/reference/features/external-config.html) y [ejecución mediante Gradle](https://docs.spring.io/spring-boot/gradle-plugin/running.html).

Desde `HotelYa-Front`, en otra terminal:

```bash
npm ci
npm run dev
```

Abrir `http://127.0.0.1:5173`. Usar `http://127.0.0.1:8080` como API para que las cookies de renovación compartan el mismo host. El frontend usa Vite y TypeScript y consulta exclusivamente Spring Boot. Sus recursos multimedia siguen servidos por el frontend; PostgreSQL guarda sus referencias. Para cambiar el backend HTTP, editar `VITE_API_BASE_URL` en `HotelYa-Front/.env` y reiniciar Vite. Para el build, usar `HotelYa-Front/.env` o una variable del proceso: la URL se incorpora a los archivos de `dist`.

`npm run preview` comprueba el build en `http://127.0.0.1:4173`. Desarrollo y preview están permitidos por los orígenes locales de CORS. Si cambia el origen/puerto del frontend, actualizar también `HOTELYA_CORS_ORIGINS` y reiniciar Spring Boot. Ver el README del frontend para build, subrutas y pruebas.

## Puertos y aislamiento

Proyecto Supabase: `hotelya`. Sus contenedores y volúmenes son independientes de Click-Back y otros proyectos. No utilizar `--all` al detenerlo.

| Servicio | Puerto del equipo |
|---|---:|
| PostgreSQL auxiliar (shadow) | 55320 |
| API interna Supabase | 55321 |
| PostgreSQL | 55322 |
| Studio | 55323 |
| Correo web / SMTP, deshabilitados | 55324 / 55325 |
| Analítica, deshabilitada | 55327 |
| Inspector de funciones, deshabilitado | 55328 |
| Supavisor, pooler en modo sesión | 55329 |
| Backend Spring Boot | 8080 |
| Frontend Vite | 5173 |
| Preview del build | 4173 |

Studio: `http://127.0.0.1:55323`. El puerto interno del contenedor PostgreSQL sigue siendo 5432; su publicación en el equipo es 55322. La aplicación usa el **pooler 55329**, no el acceso directo 55322. Supavisor exige el identificador de tenant: el usuario local es `postgres.pooler-dev`, mientras que el acceso directo a PostgreSQL utiliza `postgres`.

Antes de crear la instancia, el script rechaza puertos ocupados sin detener procesos ajenos. Si HotelYa ya está activo, `supabase start` comprueba su estado. Para diagnosticar conflictos:

```bash
lsof -nP -iTCP:55322 -iTCP:55329 -sTCP:LISTEN
docker ps --format '{{.Names}} {{.Ports}}'
```

Apagar backend y Supabase de HotelYa, conservando sus datos:

```bash
./scripts/stop-local.sh
```

La parada identifica las JVM de este checkout por su clase principal y ruta; no detiene otros proyectos ni los daemons compartidos de Gradle. Si ya están apagados, se puede repetir. `start-supabase.sh` y `stop-supabase.sh` quedan como herramientas auxiliares para operar solo la base de datos. El servidor del frontend, que se ejecuta por separado, se detiene con Ctrl+C en su terminal. No ejecutar `supabase db reset` para aplicar migraciones de la aplicación: Flyway las aplica al iniciar Spring Boot.

## Configuración

| Variable | Uso |
|---|---|
| `HOTELYA_DB_URL` | JDBC del pooler; local: `jdbc:postgresql://127.0.0.1:55329/postgres?sslmode=disable` |
| `HOTELYA_DB_USER` / `HOTELYA_DB_PASSWORD` | Credenciales PostgreSQL; `postgres.pooler-dev`/`postgres` exclusivamente en desarrollo local |
| `HOTELYA_DB_SCHEMA` | Esquema de la aplicación; `hotelya` |
| `HOTELYA_CORS_ORIGINS` | Orígenes HTTP permitidos, separados por coma |
| `SERVER_PORT` | Puerto HTTP del backend; `8080` |

HikariCP admite como máximo cinco conexiones. Flyway es el único responsable del esquema y Hibernate valida las entidades (`ddl-auto=validate`). `open-in-view` está deshabilitado: las entidades se convierten a DTOs dentro del servicio transaccional.

El esquema `hotelya` no figura entre los esquemas publicados por PostgREST y las migraciones revocan su acceso a los roles de la Data API. No hay cliente Supabase, claves Supabase ni consultas REST a la base de datos en el frontend. El perfil base requiere variables explícitas; no incorpora credenciales de producción. No se ha configurado ni desplegado un proyecto remoto.

## Datos y API

- `GET /api/hotels`: arreglo de resúmenes (`id`, `name`, `description`, `location`, `logoPath`); un catálogo vacío devuelve `[]`.
- `GET /api/hotels/{id}`: datos del hotel y `roomTypes`, con `photos`, `details` y `rates`; hotel inexistente devuelve `404`.
- El catálogo público solo permite lectura; las escrituras requieren JWT y están bajo `/api/owner`.
- La ficha conserva `hotel-central.html?id=<id>`. Para enlaces antiguos sin parámetro, consulta el listado y muestra el primer hotel; si está vacío, muestra el estado vacío.

Solo el perfil `local` ejecuta la semilla mediante JPA. Si ya existe el hotel con código `lumis-hotel`, no modifica el hotel, sus tipos, fotos ni tarifas: conserva ediciones y eliminaciones deliberadas. Si se desea reponer datos eliminados, debe hacerse explícitamente, no mediante reinicios.

Los datos del hotel proceden del frontend existente: nombre, descripción, dirección, teléfono y referencias de logo, fachada y video. Esto conserva los datos del proyecto; no representa una nueva verificación del teléfono o de los servicios del hotel. WhatsApp, coordenadas, horarios, condiciones de entrada/salida y estacionamiento quedan en `null` mientras se desconozcan.

Las tarifas son **simuladas**, en PEN, y la ficha lo indica. Son tarifas de referencia: no se calcula el importe de una estancia ni se afirma disponibilidad.

| Tipo | Hora | Paquete de 3 horas | Hora adicional | Noche |
|---|---:|---:|---:|---:|
| Temática | 25 | 60 | 20 | 120 |
| Matrimonial | 20 | 50 | 15 | 100 |
| Premium | 35 | 90 | 25 | 180 |

No se importan los otros cinco hoteles mock, calificaciones ni reseñas. El modelo usa tipos asociados a cada hotel; no impone una clasificación global ni inventario físico.

## Verificación

```bash
./gradlew build
# Requiere Supabase HotelYa activo y sus credenciales locales.
./scripts/test-integration.sh
curl http://localhost:8080/api/hotels
```

Las pruebas de integración llevan la etiqueta `integration`, se ejecutan explícitamente con la tarea `integrationTest` y utilizan el esquema separado `hotelya_test`. Flyway crea y valida ese esquema; los registros de prueba se revierten al terminar cada prueba. No se limpia ni modifica el catálogo `hotelya`, ni se ejecutan resets de Supabase.

Cubren el acceso por 55329, la migración, listado vacío, ficha y 404, tipos/fotos/tarifas, valores desconocidos, semilla sin duplicados y conservación de ediciones, unicidad del código por hotel, exclusión de la Data API y CORS. Las pruebas de navegador verifican navegación, archivos multimedia, carga, vacío, error y reintento; las pruebas de fallos simulan respuestas HTTP y no modifican la base de datos.

## Panel de dueños y JWT

«Acceder» lleva al registro/login real; «Mi panel» abre la administración del único hotel de la cuenta. Un registro nuevo configura su hotel en borrador. El dueño puede editar la ficha, tipos, fotos, tarifas, cantidades y tarjetas de estadía, y publicar u ocultar el hotel.

Configuración adicional en el `.env` privado (el script `run-local.sh` la exporta a Spring):

| Variable | Uso |
|---|---|
| `HOTELYA_JWT_SECRET` | Secreto aleatorio de al menos 32 bytes, estable entre reinicios; generar con `openssl rand -base64 48` |
| `HOTELYA_UPLOAD_DIR` | Ruta absoluta persistente y escribible para archivos; localmente puede ser `HotelYa-Back/.uploads` |
| `HOTELYA_COOKIE_SECURE` | `false` para HTTP local; `true` para HTTPS, con cookies Secure y SameSite=None |
| `HOTELYA_LUMIS_OWNER_EMAIL` | Correo de la cuenta inicial de Lumi’s, solo con perfil `local` |
| `HOTELYA_LUMIS_OWNER_PASSWORD` | Contraseña inicial de esa cuenta, mínimo 8 caracteres y máximo 72 bytes |

Las credenciales de la cuenta inicial se configuran exclusivamente en el `.env` privado. No publicar ese archivo ni el directorio de archivos. Ambos valores de la cuenta inicial deben configurarse juntos. La inicialización conserva la contraseña y asociación existentes; no recrea habitaciones ni cambia precios o cantidades.

La cuenta tiene contraseña BCrypt. El access token es un JWT HS256 con emisor, audiencia, usuario y sesión, dura 15 minutos y vive en memoria del navegador. El refresh token aleatorio tiene vigencia absoluta de siete días, se rota, se guarda en una cookie HttpOnly y solo su hash permanece en PostgreSQL. Un refresh reutilizado revoca esa sesión. Logout revoca tanto la renovación como el acceso privado de sus JWT. Las operaciones con cookies exigen CSRF; las escrituras privadas utilizan Authorization Bearer. CORS permite únicamente los orígenes configurados y credenciales. El navegador no consulta Supabase Auth ni Storage: Supabase sigue siendo PostgreSQL privado para Spring.

La autenticación es propia; Supabase aporta PostgreSQL, sin usar Supabase Auth. El login sigue `AuthController → AuthService → AuthenticationManager → DaoAuthenticationProvider`, que carga la cuenta mediante `OwnerUserDetailsService` y verifica el hash con BCrypt. Tras autenticar, `AuthService` crea la sesión y `JwtService` emite el JWT HS256 de 15 minutos. Registro y renovación también usan `JwtService`. Las peticiones protegidas siguen validándose con el Resource Server de Spring Security, incluida la vigencia de la sesión.

API:

- `GET /api/auth/csrf`; `POST /api/auth/register`, `/login`, `/refresh`, `/logout`; `GET /api/auth/me`.
- `GET`, `POST`, `PUT /api/owner/hotel`; `PUT /api/owner/hotel/publication` con `{ "published": true|false }`.
- `POST /api/owner/hotel/room-types`; `PUT`, `DELETE /api/owner/hotel/room-types/{id}`. Cada actualización guarda las fotos, detalles y tarifas completas en una transacción.
- `PUT /api/owner/hotel/stay-cards`, arreglo ordenado de `{ "title", "description" }`.
- `GET`, `POST /api/owner/media`; `GET`, `DELETE /api/owner/media/{uuid}`. Upload usa multipart con campo `file`.
- `GET /api/media/{uuid}` para archivos asociados a fichas publicadas. Las vistas privadas usan JWT y las respuestas de archivos no se almacenan en caché.

Los errores de formularios tienen `{ "message", "fields" }`. Un identificador ajeno responde 404, la falta de sesión 401 y los conflictos de publicación o archivos usados 409. El backend toma siempre al dueño del JWT.

Las imágenes JPEG/PNG/WebP se decodifican y admiten hasta 10 MB y 40 millones de píxeles; WebP usa TwelveMonkeys ImageIO. MP4 admite hasta 100 MB y se validan sus contenedores y la existencia de una pista de video. PostgreSQL conserva metadatos y los archivos usan UUID en el directorio configurado. Eliminar primero la asociación y guardar, luego eliminar el archivo de la biblioteca; no se borran los recursos originales compartidos del frontend. Para desplegar, montar un volumen persistente y respaldarlo junto con la base de datos.

Publicar exige información básica y al menos un tipo con cantidades, una foto y una tarifa. `0 <= availableUnits <= totalUnits`; los tipos originales conservan ambas cantidades nulas hasta configurarlos. Los borradores y sus medios nuevos no aparecen en la API pública. Los hoteles existentes mantienen su publicación al migrar. La información básica, el estacionamiento y el contenido se guardan independientemente de las cantidades de habitaciones. Los hoteles migrados sin cantidades permiten configurarlas progresivamente. Una edición de habitaciones que quite el último tipo completo de un hotel publicado se rechaza: ocultarlo desde Inicio permite retirar esa configuración. La URL HTTPS del lugar y las horas diarias de apertura y cierre se guardan en campos dedicados; el cierre puede ser al día siguiente y dos horas iguales indican atención de 24 horas.

## Pruebas del panel

`./gradlew build` incluye pruebas de detección y decodificación de archivos. `./scripts/test-integration.sh` prueba cuentas, JWT vencidos, rotación, replay, logout, CSRF, permisos entre dueños, CRUD, archivos, validaciones y publicación en `hotelya_test`.

Desde el frontend, `npm run test:e2e:owner` inicia un backend aislado en 8081 y Vite en 5174, verifica el flujo completo en Chromium de escritorio y móvil y limpia sus datos. Requiere Docker y Supabase HotelYa activo. Los scripts `run-browser-test-backend.sh` y `clean-browser-test-data.sh` usan exclusivamente `hotelya_test`, nunca el catálogo `hotelya`. Ejecutar estas pruebas separadamente de las pruebas de integración Java porque comparten el esquema de pruebas. La configuración inicial de Lumi’s se desactiva en ese servidor de pruebas.

El editor de habitaciones carga fotografías con un `roomScope` ligado al código del tipo (o al identificador de su borrador). Estos archivos no se ofrecen en Multimedia y no pueden asignarse a otro tipo ni a la identidad del hotel. Las fotografías existentes se conservan. El texto de cada imagen se deriva del nombre del tipo; el dueño no edita texto alternativo.

La escritura de tarifas admite una única entrada de cada modalidad HOUR, PACKAGE y NIGHT. El dueño activa o desactiva las filas del editor; solo se guardan las activas. La moneda de escritura es PEN y las tarifas guardadas por el dueño se consideran reales. El contrato público conserva `currency` y `simulated` para los datos originales y los consumidores existentes. La duración corresponde solo a PACKAGE; el editor ya no solicita hora adicional.

### Capas de persistencia

Los controladores atienden HTTP; los servicios coordinan reglas de negocio, autorización y transacciones; los repositorios de `infrastructure` encapsulan el acceso a PostgreSQL mediante Spring Data JPA/Hibernate. Los servicios y la carga inicial usan repositorios, sin `EntityManager` ni consultas directas. Las entidades conservan su mapeo Jakarta Persistence.

Los repositorios de hotel y sesión declaran las consultas con bloqueo pesimista utilizadas por edición, renovación y cierre de sesión. Los cambios sobre entidades cargadas se guardan al finalizar la transacción del servicio mediante el seguimiento de cambios de JPA; los `flush()` de repositorio conservan los puntos de sincronización requeridos por el flujo.

`RefreshTokenService` encapsula la generación aleatoria, la rotación, el hash SHA-256, la comparación y la lectura del identificador de sesión del refresh token. `AuthService` coordina el ciclo de sesión, la persistencia mediante repositorios, la caducidad y la revocación por reutilización; `JwtService` emite los access tokens JWT.

### Organización de autenticación

- `application/auth`: `AuthService`, `JwtService`, `RefreshTokenService`, `SessionService` y los contratos internos `AuthCredentials`, `AuthUser` y `AuthTokens`.
- `presentation/auth`: controller y DTO HTTP en `dto`; el controller convierte los contratos de aplicación a respuestas públicas y coloca el refresh token únicamente en la cookie HttpOnly.
- `domain`: entidades JPA `OwnerAccount` y `OwnerSession`.
- `security`: configuración, adaptación de usuarios a Spring Security y `SessionJwtValidator`. Este validador comprueba `sid` y delega en `SessionService.isSessionActive` para rechazar sesiones ausentes, revocadas, vencidas o de otro usuario.
- `infrastructure`: repositorios y configuración de la cuenta local inicial.

La comprobación de sesión conserva la revocación inmediata del access token tras logout o reutilización del refresh token y consulta PostgreSQL en cada autenticación con JWT.
