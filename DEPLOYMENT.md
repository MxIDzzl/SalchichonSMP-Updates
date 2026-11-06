# Despliegue del portal y API

La web y la API son servicios separados. El navegador solo habla con Next.js; Next.js consulta la API desde el servidor. PostgreSQL nunca se expone al navegador ni a Internet.

## Orden de despliegue

1. Preparar PostgreSQL privado.
2. Instalar SalchichonHardcore con el esquema v7 y sincronización de zMClans.
3. Migrar SQLite a PostgreSQL en staging y verificar todos los conteos antes de cualquier corte real.
4. Aplicar migraciones API V1-V5 con credenciales DDL separadas.
5. Crear el rol de solo lectura usando [public-role-grants.example.sql](api/docs/public-role-grants.example.sql).
6. Desplegar la API y comprobar `/api/v1/health/ready`.
7. Desplegar Next.js con `SALCHICHON_API_URL` apuntando a la API privada.
8. Verificar inicio, directorio de jugadores, perfiles, Player Value, rankings, clanes, skins, anuncios y próxima actualización.

## PostgreSQL

- La base de datos debe ser privada y accesible solo desde el servidor Paper y la API.
- El usuario del plugin necesita los permisos de escritura que requiere SalchichonHardcore.
- La API pública debe usar exclusivamente el rol SELECT-only del archivo de ejemplo.
- Las migraciones Flyway usan credenciales DDL independientes.
- No habilitar el panel administrativo durante la primera publicación.

## API

Variables mínimas:
- `SALCHICHON_API_DB_URL`: URL JDBC privada de PostgreSQL.
- `SALCHICHON_API_DB_USER` y `SALCHICHON_API_DB_PASSWORD`: rol público de solo lectura.
- `SALCHICHON_API_FLYWAY_ENABLED=true` solo durante la aplicación controlada de V1-V5.
- `SALCHICHON_API_MIGRATION_DB_URL`, `SALCHICHON_API_MIGRATION_DB_USER` y `SALCHICHON_API_MIGRATION_DB_PASSWORD`: rol DDL temporal/separado.
- `SALCHICHON_WEB_ORIGINS`: dominio HTTPS del portal.
- `SALCHICHON_SERVER_TIMEZONE=America/Mexico_City`.
- `SALCHICHON_SEASON_NAME`, `SALCHICHON_SEASON_DESCRIPTION`, `SALCHICHON_SEASON_START_DATE` y `SALCHICHON_SEASON_DIFFICULTY`.

La progresión de dificultad se configura con `SALCHICHON_SEASON_DIFFICULTY_STAGES`, por ejemplo `1:HARD,14:HARD+,28:EXTREME`. Cada número es el primer día de temporada en el que aplica esa etapa. Si se deja vacío, la API muestra la dificultad base. La progresión cambia la etapa publicada en la web; no modifica niveles ni atributos de mobs, que siguen aplazados.

Mantener `SALCHICHON_ADMIN_ENABLED=false`. El panel /admin es informativo y no debe exponerse como consola administrativa hasta que se complete autenticación real y autorización multiusuario.

## Web

Configurar en el proceso Next.js:
- `SALCHICHON_API_URL`: URL privada alcanzable desde el servidor Next.js, por ejemplo `http://salchichon-api:8080`.

No usar `NEXT_PUBLIC_*` para credenciales ni tokens. Terminar TLS en un proxy inverso y limitar el tráfico interno a los servicios necesarios.

Build local:
```bash
npm install
npm run build
npm run start
```

Docker:
```bash
docker build -t salchichon-web .
docker run --rm -p 3000:3000 -e SALCHICHON_API_URL=http://API_PRIVADA:8080 salchichon-web
```

## Comprobaciones de aceptación

- La portada muestra datos reales o estados explícitos de no sincronización; nunca cifras de ejemplo.
- Los perfiles resuelven por UUID y muestran skin, estadísticas, Player Value y clan cuando existe.
- Los clanes provienen del snapshot de zMClans; la web no administra clanes.
- El directorio y ranking no muestran jugadores duplicados.
- Los anuncios vencidos y actualizaciones no futuras no aparecen como próximos.
- El rol público de API no puede modificar tablas.
- Las rutas /api/v1/admin/** permanecen deshabilitadas.
- PostgreSQL, backups y SQLite original permanecen privados y disponibles para rollback.

## Límites de la primera publicación

- Player Value permanece desactivado hasta terminar la GUI tipo cofre y las pruebas de recuperación de depósitos/retiros.
- Los intercambios siguen a cargo del plugin externo.
- El panel administrativo requiere autenticación/autorización antes de habilitar acciones.
- Los niveles/atributos de mobs se implementarán en una fase posterior.
