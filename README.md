# HotelYa Backend

Backend del catálogo y del panel de dueños, desarrollado con Spring Boot, Java 25, Gradle y PostgreSQL.

## Ejecución local

Requisitos: Java 25, Docker en ejecución, Supabase CLI y Python 3.

Ejecutar desde `HotelYa-Back`:

```bash
./scripts/run-local.sh
```

Inicia Supabase local y el backend.

```bash
./scripts/stop-local.sh
```

Detiene el backend y Supabase local, conservando los datos.

## Seed local

Una vez iniciado el backend local, ejecutar:

```bash
./scripts/seed-local.sh
```

Carga el catálogo de ejemplo; no crea cuentas.

## Pruebas del panel

```bash
./gradlew test    # Ejecuta las pruebas unitarias y genera el reporte de cobertura.
./gradlew check   # Ejecuta las pruebas unitarias y verifica los mínimos de cobertura.
./gradlew build   # Compila, empaqueta y ejecuta las verificaciones.
```

## Capas de persistencia

- `controller`: atiende las peticiones HTTP y delega en los servicios.
- `service`: aplica las reglas de negocio y coordina la autorización y las transacciones.
- `repository`: accede a PostgreSQL mediante Spring Data JPA/Hibernate.
- `model`: define las entidades y sus mapeos Jakarta Persistence.

Flyway gestiona las migraciones del esquema; Hibernate valida el mapeo de las entidades.
