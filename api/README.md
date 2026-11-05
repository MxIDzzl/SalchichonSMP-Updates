# Salchichón API

Servicio Spring Boot independiente para el portal público y una superficie administrativa privada y desactivada por defecto. Se despliega por separado del plugin Paper y de Next.js. Comparte PostgreSQL privado; nunca expone una conexión de base de datos al navegador.

## Requisitos
- Java 25
- Maven 3.9+
- PostgreSQL con el esquema del plugin v1-v6 y las migraciones API V1-V4 aplicadas
- Rol runtime de solo lectura para la API pública

## Configuración
Variables de entorno:
- `SALCHICHON_API_DB_URL` (por defecto localhost:5432/salchichon)
- `SALCHICHON_API_DB_USER`, `SALCHICHON_API_DB_PASSWORD`
- `SALCHICHON_WEB_ORIGINS` (orígenes separados por coma)
- `SALCHICHON_SEASON_NAME`, `SALCHICHON_SEASON_DESCRIPTION`, `SALCHICHON_SEASON_DIFFICULTY`, `SALCHICHON_SEASON_START_DATE`, `SALCHICHON_SERVER_TIMEZONE`
- `SALCHICHON_API_FLYWAY_ENABLED` (por defecto false)
- `SALCHICHON_API_MIGRATION_DB_URL`, `SALCHICHON_API_MIGRATION_DB_USER`, `SALCHICHON_API_MIGRATION_DB_PASSWORD` (credenciales DDL separadas)
- `SALCHICHON_ADMIN_ENABLED` (por defecto false), `SALCHICHON_ADMIN_TOKEN` (mínimo 32 caracteres aleatorios), `SALCHICHON_ADMIN_ACTOR` (identidad de auditoría del servidor)
- `SALCHICHON_ADMIN_DB_URL`, `SALCHICHON_ADMIN_DB_USER`, `SALCHICHON_ADMIN_DB_PASSWORD` (pool privado de escritura)

No usar el usuario administrador de PostgreSQL como credencial runtime. El pool público permanece read-only. Flyway está desactivado por defecto y usa credenciales de migración separadas.

## Desarrollo
```bash
mvn clean verify
mvn spring-boot:run
```

## Endpoints públicos v1
- `GET /api/v1/health/live`
- `GET /api/v1/health/ready`
- `GET /api/v1/public/overview`
- `GET /api/v1/public/players?limit=20&offset=0&sort=value`
- `GET /api/v1/public/players/{uuid}` (incluye `hasLife`, calculado en la zona horaria configurada)
- `GET /api/v1/public/announcements?limit=10`
- `GET /api/v1/public/upcoming-update` (404 si no hay una actualización futura publicada)

Sort permitido: `value`, `deaths`, `playtime`, `totems`. Paginación limitada a 100 registros. No hay escrituras públicas.

## Administración privada (fase controlada)

Todas las rutas `/api/v1/admin/**` requieren `Authorization: Bearer <token>`. La comparación es en tiempo constante y la API falla cerrada si el token no está configurado o tiene menos de 32 caracteres. El actor se obtiene exclusivamente de `SALCHICHON_ADMIN_ACTOR`; nunca se acepta un actor enviado por el cliente. Cada mutación exige motivo y registra auditoría en la misma transacción.

Anuncios:
- `GET /api/v1/admin/announcements?limit=50&offset=0`: lista borradores y publicaciones.
- `POST /api/v1/admin/announcements`: crea anuncio o teaser.
- `PUT /api/v1/admin/announcements/{id}`: edita contenido y programación.
- `PATCH /api/v1/admin/announcements/{id}/publication`: publica/despublica.
- `GET /api/v1/admin/audit?limit=50&offset=0`: consulta auditoría.

Correcciones estadísticas:
- `PUT /api/v1/admin/players/{uuid}/stats/deaths`
- `PUT /api/v1/admin/players/{uuid}/stats/playtime`
- Body: `{"value":123,"reason":"Corrección por ..."}`
- Solo permite muertes oficiales y ticks de juego. La corrección actualiza `player_stats`, inserta un evento append-only en `stat_events` y registra el mismo cambio en `public_admin_audit`, todo atómicamente.
- Player Value, inventarios, tótems, vidas y datos de trade no se modifican desde esta ruta.

La administración usa un pool PostgreSQL separado. Su rol debe tener únicamente:
- SELECT sobre `player_data` y `player_stats`.
- INSERT/UPDATE sobre `public_announcements` y SELECT/INSERT sobre `public_admin_audit`.
- INSERT/UPDATE sobre `player_stats` e INSERT sobre `stat_events`.
- No conceder permisos sobre tablas de bóveda, operaciones, trade, `player_value_adjustments` ni el resto del estado del plugin.

Habilitar requiere `SALCHICHON_ADMIN_ENABLED=true`, token privado y credenciales separadas. Mantener el token en un gateway/backend privado: nunca en JavaScript del navegador ni en variables `NEXT_PUBLIC_*`. Esta fase usa una identidad de operador configurada, no OIDC/RBAC multiusuario. Antes de producción se requiere gateway autenticado, HTTPS, rate limiting, rotación/revocación y pruebas de staging.

## Skins de jugadores
La migración API V3 crea `player_skins`. El plugin sincroniza únicamente el hash público de textura de SkinsRestorer (API major 15); no se guardan firmas ni credenciales. Los perfiles sin hash permanecen sin avatar.

## Migraciones
Aplicar las migraciones API V1-V4 con credenciales DDL separadas. Mantener PostgreSQL privado y realizar backup verificado antes de cualquier migración. No habilitar los servicios administrativos ni cambiar el plugin a PostgreSQL sin ensayos completos de recuperación.
