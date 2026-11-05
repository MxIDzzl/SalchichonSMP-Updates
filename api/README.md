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
- `SALCHICHON_ADMIN_ENABLED` (por defecto false), `SALCHICHON_ADMIN_TOKEN` (mínimo 32 caracteres aleatorios), `SALCHICHON_ADMIN_ACTOR` (identidad de auditoría controlada por el servidor)
- `SALCHICHON_ADMIN_DB_URL`, `SALCHICHON_ADMIN_DB_USER`, `SALCHICHON_ADMIN_DB_PASSWORD` (pool de escritura separado; solo necesario al habilitar administración)

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

## Administración inicial (fase controlada)

La API incorpora una primera superficie administrativa para anuncios, desactivada por defecto. No es todavía el panel web completo ni autenticación multiusuario.

- `GET /api/v1/admin/announcements?limit=50&offset=0`: lista anuncios, incluidos borradores.
- `POST /api/v1/admin/announcements`: crea un anuncio o teaser, como borrador o publicado.
- `PUT /api/v1/admin/announcements/{id}`: edita título, cuerpo, categoría, programación, expiración y estado teaser.
- `PATCH /api/v1/admin/announcements/{id}/publication`: publica o despublica un anuncio existente.
- `GET /api/v1/admin/audit?limit=50&offset=0`: consulta el registro de auditoría.

Todas las rutas requieren `Authorization: Bearer <token>`. El token se compara en tiempo constante y la API falla cerrada si no está configurado o tiene menos de 32 caracteres. El actor de auditoría proviene de `SALCHICHON_ADMIN_ACTOR` del servidor; no se acepta un actor arbitrario enviado por el navegador. Cada escritura exige un motivo y registra actor, acción, entidad, ID, motivo, detalles y fecha en `public_admin_audit`.

La administración usa un segundo pool de PostgreSQL independiente del pool público read-only. Habilitarla requiere configurar `SALCHICHON_ADMIN_ENABLED=true`, token y credenciales separadas. El rol de escritura debe limitarse a INSERT/UPDATE sobre `public_announcements` y SELECT/INSERT sobre `public_admin_audit`; no reutilizar el rol de runtime ni el superusuario. Ejecutar Flyway con credenciales de migración separadas para aplicar V4.

**No enviar el token administrativo al navegador ni incluirlo en variables `NEXT_PUBLIC_*`.** Debe permanecer en un backend/gateway privado. Para un panel con varios administradores se requiere integrar un proveedor OIDC, usuarios individuales, roles y rotación/revocación por usuario antes de producción. No habilitar esta fase directamente en el servidor real sin HTTPS, proxy con límites de tasa, secretos robustos y ensayo en staging.
