# Salchichón API

Servicio Spring Boot independiente para el portal público y una superficie administrativa privada, desactivada por defecto. Se despliega por separado del plugin Paper y de Next.js. La base de datos es **MySQL 8.4** (o una versión MariaDB que pase las mismas pruebas); el navegador nunca recibe credenciales ni conexiones de base de datos.

## Requisitos
- Java 25
- Maven 3.9+
- MySQL 8.4 y MariaDB 11.4 están cubiertos por CI; el motor y versión reales del hosting se registran al iniciar la API.
- Esquema MySQL del plugin y migraciones API V1-V5 aplicadas
- Un usuario runtime público con permisos SELECT solamente (ejemplo en `docs/public-role-grants.example.sql`)

## Configuración

Variables de entorno:

- `SALCHICHON_API_DB_URL` (por defecto `jdbc:mysql://127.0.0.1:3306/salchichon?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC`)
- `SALCHICHON_API_DB_USER`, `SALCHICHON_API_DB_PASSWORD`
- `SALCHICHON_WEB_ORIGINS` (orígenes separados por coma)
- `SALCHICHON_SEASON_NAME`, `SALCHICHON_SEASON_DESCRIPTION`, `SALCHICHON_SEASON_DIFFICULTY`, `SALCHICHON_SEASON_DIFFICULTY_STAGES`, `SALCHICHON_SEASON_START_DATE`, `SALCHICHON_SERVER_TIMEZONE`
- `SALCHICHON_API_FLYWAY_ENABLED` (por defecto false)
- `SALCHICHON_API_MIGRATION_DB_URL`, `SALCHICHON_API_MIGRATION_DB_USER`, `SALCHICHON_API_MIGRATION_DB_PASSWORD` (credenciales DDL separadas)
- `SALCHICHON_ADMIN_ENABLED` (por defecto false), `SALCHICHON_ADMIN_TOKEN` (mínimo 32 caracteres aleatorios), `SALCHICHON_ADMIN_ACTOR` (identidad de auditoría del servidor)
- `SALCHICHON_ADMIN_DB_URL`, `SALCHICHON_ADMIN_DB_USER`, `SALCHICHON_ADMIN_DB_PASSWORD` (pool privado de escritura)

Los pools público y administrativo fijan sus sesiones MySQL a UTC. Las credenciales públicas y administrativas deben ser usuarios distintos. No uses el usuario DDL como credencial runtime. Flyway está desactivado por defecto y usa credenciales de migración separadas.

La dificultad puede avanzar automáticamente por día de temporada mediante `SALCHICHON_SEASON_DIFFICULTY_STAGES`, con formato `1:HARD,14:HARD+,28:EXTREME`; cada umbral indica el primer día en que entra en vigor. Si se deja vacío o la fecha de inicio no está configurada, se usa `SALCHICHON_SEASON_DIFFICULTY`. Este ajuste solo publica la etapa: no cambia todavía los atributos ni niveles de mobs.

## Desarrollo y CI

```bash
mvn -f api/pom.xml clean verify
mvn -f api/pom.xml spring-boot:run
```

GitHub Actions levanta MySQL 8.4 y MariaDB 11.4 en una matriz independiente; las 25 pruebas pasan en ambos motores. Esto valida las versiones de CI, no sustituye la comprobación de la versión exacta del proveedor. Para ejecutar esas pruebas localmente, define `SALCHICHON_MYSQL_TEST_URL`, `SALCHICHON_MYSQL_TEST_USER` y `SALCHICHON_MYSQL_TEST_PASSWORD`.

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

### Permisos MySQL del rol administrativo

Los ejemplos ejecutables de permisos por tabla/columna están en:
- `docs/public-role-grants.example.sql`
- `docs/admin-role-grants.example.sql`

Reemplaza nombres de base/usuario, contraseña y host antes de ejecutarlos. Limita el host del usuario a la IP del servidor API siempre que el proveedor lo permita. El rol administrativo recibe permisos de escritura solo en anuncios, auditoría y las columnas necesarias para corregir muertes oficiales/tiempo de juego. La inspección de trades tiene SELECT por columnas y excluye `item_payload`; no recibe acceso a bóveda, Player Value ni operaciones de entrega.

Habilitar requiere `SALCHICHON_ADMIN_ENABLED=true`, token privado y credenciales separadas. Mantén el token en un gateway/backend privado: nunca en JavaScript del navegador ni en variables `NEXT_PUBLIC_*`. Esta fase usa una identidad de operador configurada, no OIDC/RBAC multiusuario. Antes de producción se requiere gateway autenticado, HTTPS, rate limiting, rotación/revocación y pruebas de staging.

## Skins de jugadores

La migración API V3 crea `player_skins`. El plugin sincroniza únicamente el hash público de textura de SkinsRestorer (API major 15); no se guardan firmas ni credenciales. Los perfiles sin hash permanecen sin avatar.

## Migraciones y recuperación

Aplicar el esquema MySQL del plugin y las migraciones API V1-V5 con credenciales DDL separadas y después de un backup verificado. La API pública debe usar un usuario SELECT-only. No habilitar los servicios administrativos ni cambiar el plugin a MySQL en producción sin ensayos completos de importación SQLite, recuperación y restauración de backup.

## Sincronización de clanes

El plugin Paper consulta el API público de zMClans en el hilo principal (como exige su contrato), crea un snapshot inmutable y lo persiste de forma transaccional cada cinco minutos. La web solo lee las tablas `public_clans` y `player_clan_memberships`; nunca gestiona invitaciones, rangos, miembros ni cambios de clan. Los registros que desaparecen del snapshot se marcan inactivos, no se borran. Si zMClans no está disponible, se conserva el último snapshot válido.
