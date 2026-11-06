# Salchichón API

Servicio Spring Boot independiente para el portal público y una superficie administrativa privada y desactivada por defecto. Se despliega por separado del plugin Paper y de Next.js. Comparte PostgreSQL privado; nunca expone una conexión de base de datos al navegador.

## Requisitos
- Java 25
- Maven 3.9+
- PostgreSQL con el esquema del plugin v1-v7 y las migraciones API V1-V5 aplicadas
- Rol runtime de solo lectura para la API pública (ejemplo en `docs/public-role-grants.example.sql`)

## Configuración
Variables de entorno:
- `SALCHICHON_API_DB_URL` (por defecto localhost:5432/salchichon)
- `SALCHICHON_API_DB_USER`, `SALCHICHON_API_DB_PASSWORD`
- `SALCHICHON_WEB_ORIGINS` (orígenes separados por coma)
- `SALCHICHON_SEASON_NAME`, `SALCHICHON_SEASON_DESCRIPTION`, `SALCHICHON_SEASON_DIFFICULTY`, `SALCHICHON_SEASON_DIFFICULTY_STAGES`, `SALCHICHON_SEASON_START_DATE`, `SALCHICHON_SERVER_TIMEZONE`
- `SALCHICHON_API_FLYWAY_ENABLED` (por defecto false)
- `SALCHICHON_API_MIGRATION_DB_URL`, `SALCHICHON_API_MIGRATION_DB_USER`, `SALCHICHON_API_MIGRATION_DB_PASSWORD` (credenciales DDL separadas)
- `SALCHICHON_ADMIN_ENABLED` (por defecto false), `SALCHICHON_ADMIN_TOKEN` (mínimo 32 caracteres aleatorios), `SALCHICHON_ADMIN_ACTOR` (identidad de auditoría del servidor)
- `SALCHICHON_ADMIN_DB_URL`, `SALCHICHON_ADMIN_DB_USER`, `SALCHICHON_ADMIN_DB_PASSWORD` (pool privado de escritura)

La dificultad puede avanzar automáticamente por día de temporada mediante `SALCHICHON_SEASON_DIFFICULTY_STAGES`, con formato `1:HARD,14:HARD+,28:EXTREME`; cada umbral indica el primer día en que entra en vigor. Si se deja vacío o la fecha de inicio no está configurada, se usa `SALCHICHON_SEASON_DIFFICULTY`. Este ajuste solo publica la etapa: no cambia todavía los atributos ni niveles de mobs.

No usar el usuario administrador de PostgreSQL como credencial runtime. El pool público permanece read-only. El ejemplo `docs/public-role-grants.example.sql` limita el rol público a SELECT sobre `player_data`, `player_stats`, `player_skins`, `public_clans`, `player_clan_memberships` y `public_announcements`; no concede escritura ni acceso a bóveda/trades. Flyway está desactivado por defecto y usa credenciales de migración separadas.

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
- `GET /api/v1/public/players/{uuid}` (incluye `hasLife` y clan activo, calculados desde los snapshots sincronizados)
- `GET /api/v1/public/clans?limit=20&offset=0&sort=points`
- `GET /api/v1/public/clans/{uuid}` (incluye miembros, rangos y estadísticas de zMClans)
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
- Solo permite muertes oficiales y ticks de juego. La corrección actualiza `player_stats`, inserta un evento append-only en `stat_events` y registra el cambio en `public_admin_audit`, todo atómicamente.
- Player Value, inventarios, tótems, vidas y datos de trade no se modifican desde esta ruta.

Inspección de recuperación de trades:
- `GET /api/v1/admin/trades/recovery?limit=50&offset=0`
- Lista sesiones marcadas `RECOVERY_REQUIRED` y operaciones de ítems en `PREPARED`, `DELIVERY_PENDING`, `DELIVERING`, `RETURN_PENDING` o `RECOVERY_REQUIRED`.
- Incluye participantes, propietario/destinatario, estados, huellas SHA-256 y fechas para que el staff pueda investigar.
- Nunca devuelve el payload serializado de los ítems ni ofrece una acción de entrega/cancelación forzada. La resolución requiere verificar inventario y marcadores PDC dentro del servidor, no inferirla desde el panel.

### Permisos PostgreSQL del rol administrativo
Limitar el rol a lo estrictamente necesario. Hay un ejemplo ejecutable de permisos por tabla/columna en [`docs/admin-role-grants.example.sql`](docs/admin-role-grants.example.sql); reemplazar el nombre de rol y ejecutarlo como propietario de la base de datos:
- SELECT sobre `player_data`, `player_stats` y `trade_sessions`.
- SELECT **solo por columnas** en `trade_item_operations`: `operation_id`, `trade_id`, `owner_uuid`, `recipient_uuid`, `item_fingerprint`, `status`, `created_at`, `updated_at` y `delivered_at`. No conceder SELECT sobre `item_payload`; el endpoint no necesita ni devuelve ese contenido.
- INSERT/UPDATE sobre `public_announcements`.
- SELECT/INSERT sobre `public_admin_audit`.
- INSERT/UPDATE sobre `player_stats` e INSERT sobre `stat_events`.
- No conceder permisos sobre bóveda, `player_value_items`, `player_value_adjustments`, `trade_escrow_items` ni otras tablas operativas. En `trade_item_operations`, evitar SELECT a nivel de tabla: usar GRANT de columnas para excluir `item_payload`.

Habilitar requiere `SALCHICHON_ADMIN_ENABLED=true`, token privado y credenciales separadas. Mantener el token en un gateway/backend privado: nunca en JavaScript del navegador ni en variables `NEXT_PUBLIC_*`. Esta fase usa una identidad de operador configurada, no OIDC/RBAC multiusuario. Antes de producción se requiere gateway autenticado, HTTPS, rate limiting, rotación/revocación y pruebas de staging.

## Skins de jugadores
La migración API V3 crea `player_skins`. El plugin sincroniza únicamente el hash público de textura de SkinsRestorer (API major 15); no se guardan firmas ni credenciales. Los perfiles sin hash permanecen sin avatar.

## Migraciones y recuperación
Aplicar las migraciones API V1-V5 con credenciales DDL separadas. Aplicar el esquema del plugin v1-v7 tras backup verificado. PostgreSQL debe permanecer privado. No habilitar los servicios administrativos ni cambiar el plugin a PostgreSQL sin ensayos completos de recuperación.


## Sincronización de clanes
El plugin Paper consulta el API público de zMClans en el hilo principal (como exige su contrato), crea un snapshot inmutable y lo persiste de forma transaccional cada cinco minutos. La web solo lee las tablas public_clans y player_clan_memberships; nunca gestiona invitaciones, rangos, miembros ni cambios de clan. Los registros que desaparecen del snapshot se marcan inactivos, no se borran. Si zMClans no está disponible, se conserva el último snapshot válido.
