# Salchichón API

Servicio Spring Boot independiente y de solo lectura para el portal público. Se ejecuta y despliega por separado del plugin Paper y de Next.js. Comparte el PostgreSQL privado del servidor, pero nunca expone una conexión de base de datos al navegador.

## Requisitos
- Java 25
- Maven 3.9+
- PostgreSQL con el esquema v1-v5 del plugin aplicado y migraciones API aplicadas
- Rol PostgreSQL de solo lectura para el runtime de la API

## Configuración
Variables de entorno:
- `SALCHICHON_API_DB_URL` (por defecto localhost:5432/salchichon)
- `SALCHICHON_API_DB_USER`
- `SALCHICHON_API_DB_PASSWORD`
- `SALCHICHON_WEB_ORIGINS` (orígenes separados por coma)
- `SALCHICHON_SEASON_NAME`, `SALCHICHON_SEASON_DESCRIPTION`, `SALCHICHON_SEASON_DIFFICULTY`, `SALCHICHON_SEASON_START_DATE`, `SALCHICHON_SERVER_TIMEZONE`
- `SALCHICHON_API_FLYWAY_ENABLED` (por defecto false)
- `SALCHICHON_API_MIGRATION_DB_URL`, `SALCHICHON_API_MIGRATION_DB_USER`, `SALCHICHON_API_MIGRATION_DB_PASSWORD` (credenciales separadas para ejecutar Flyway; usar un rol de despliegue con permisos DDL)

Nunca usar el usuario administrador de PostgreSQL como credencial de ejecución. El rol de runtime debe tener únicamente SELECT sobre las tablas públicas. Ejecutar las migraciones con las variables `SALCHICHON_API_MIGRATION_DB_*` y una cuenta de despliegue separada. El pool de runtime permanece en modo read-only. Flyway está desactivado por defecto.

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
- `GET /api/v1/public/players/{uuid} (incluye `hasLife`, calculado con la fecha local configurada del servidor)`
- `GET /api/v1/public/announcements?limit=10`
- `GET /api/v1/public/upcoming-update` (404 when no future update is published)

Sort permitido: `value`, `deaths`, `playtime`, `totems`. Paginación limitada a 100 registros. No se aceptan escrituras públicas.

## Novedades programadas
La migración V2 añade `scheduled_at` y `is_teaser` a `public_announcements`. El endpoint upcoming devuelve la siguiente publicación futura marcada como publicada, respetando `expires_at`. Los teasers deben marcarse explícitamente como tales; nunca se infiere que un anuncio es filtración por su título.

## Estado de seguridad
Esta primera entrega es estrictamente de lectura pública. No incluye rutas administrativas, autenticación de administradores ni validación de licencias de plugin. Esas rutas no deben añadirse hasta definir y probar autenticación, autorización, rotación/revocación de tokens y auditoría. No abrir PostgreSQL a internet; exponer solo HTTPS de la API.

## Skins de jugadores

La migración API V3 crea la tabla compartida `player_skins`. El plugin Paper, cuando detecta SkinsRestorer API major 15, sincroniza de forma asíncrona únicamente el hash público de textura del perfil del jugador. La API devuelve ese campo como `skinTextureHash` en los perfiles y directorios. No se guardan firmas completas, credenciales ni propiedades privadas de SkinsRestorer. Los perfiles sin hash permanecen sin avatar; nunca se inventa una skin.
