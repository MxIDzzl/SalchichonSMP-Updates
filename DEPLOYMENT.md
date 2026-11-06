# Despliegue del portal y API

La web y la API son servicios separados. El navegador solo habla con Next.js; Next.js consulta la API desde el servidor. MySQL nunca se expone al navegador ni a Internet.

## Arquitectura de datos

- Paper + SalchichonHardcore: backend SQLite actual; MySQL está disponible para staging y será el backend compartido de plugin/API cuando se apruebe el cambio.
- MySQL: una sola base de datos compartida por el plugin y la API, con usuarios y permisos separados cuando el hosting lo permita.
- API Spring Boot: lectura pública mediante un usuario SELECT-only; migraciones Flyway con credenciales DDL separadas.
- Next.js: consulta la API desde el servidor mediante `SALCHICHON_API_URL`; no recibe credenciales de base de datos.

## Orden de despliegue futuro (solo staging hasta aprobar los gates)

1. Confirmar la versión exacta de MySQL/MariaDB del proveedor y que permite conexiones desde los procesos Paper y API.
2. Crear una base MySQL vacía y un usuario de escritura para el plugin. Si el proveedor permite cuentas separadas, aplicar [plugin-role-grants.example.sql](api/docs/plugin-role-grants.example.sql) y limitar el host al servidor Paper. Mantener SQLite intacta y respaldada.
3. Instalar SalchichonHardcore en un servidor de staging; habilitar `database.mysql.enabled: true`. Si se importa la base anterior, habilitar `database.mysql.import-sqlite-on-startup: true` solo para el primer arranque. El importador exige destino vacío, valida SQLite, copia y verifica conteos/IDs, y restablece el indicador tras éxito.
4. Aplicar migraciones API V1-V5 con credenciales DDL separadas y baseline 0.
5. Crear el usuario de solo lectura usando [public-role-grants.example.sql](api/docs/public-role-grants.example.sql).
6. Desplegar la API y comprobar `/api/v1/health/ready`.
7. Desplegar Next.js con `SALCHICHON_API_URL` apuntando a la API privada.
8. Verificar portada, directorio, perfiles, Player Value (solo lectura si sigue desactivado), rankings, clanes, skins, anuncios y próxima actualización.
9. Ensayar backup/restore, reconexión, recuperación ante interrupciones y rollback a SQLite antes de cualquier corte de producción.

## Campos del panel de hosting MySQL

- **Endpoint:** host y puerto que entrega el proveedor; usa el host en la URL JDBC y conserva el puerto indicado (normalmente 3306).
- **Connections from:** permite únicamente la IP de salida del servidor Paper y la del proceso API. No hace falta permitir el navegador ni las IP de los jugadores.
- **Username / Password:** guárdalos como variables de entorno del servicio; no los publiques en repositorios ni en el chat.
- **JDBC Connection String:** úsala como base para `database.mysql.jdbc-url` del plugin y `SALCHICHON_API_DB_URL` de la API, cambiando solo parámetros si el proveedor exige alguno.

Si el panel genera una sola cuenta por base de datos y no permite cuentas/grants separados, ambos procesos podrían tener que compartir credenciales. La API mantiene Hikari en modo read-only, pero eso no sustituye los permisos SELECT-only de MySQL; considera esa limitación antes de producción.

## MySQL

- La base de datos debe ser privada y accesible solo desde Paper y la API.
- El usuario del plugin necesita los permisos de escritura que requiere SalchichonHardcore; el ejemplo de grants evita conceder `DROP`, `ALTER` o `GRANT OPTION`.
- La API pública debe usar exclusivamente el rol SELECT-only del archivo de ejemplo.
- Las migraciones Flyway usan credenciales DDL independientes.
- Si el hosting no permite crear usuarios separados, la API conserva Hikari en modo read-only, pero se pierde el aislamiento de permisos a nivel de base de datos; debe documentarse y valorarse antes de producción.
- No habilitar el panel administrativo durante la primera publicación.

## API

Variables mínimas:
- `SALCHICHON_API_DB_URL`: URL JDBC privada MySQL, por ejemplo `jdbc:mysql://HOST:3306/salchichon?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC`.
- `SALCHICHON_API_DB_USER` y `SALCHICHON_API_DB_PASSWORD`: usuario público de solo lectura.
- `SALCHICHON_API_FLYWAY_ENABLED=true` solo durante la aplicación controlada de V1-V5.
- `SALCHICHON_API_MIGRATION_DB_URL`, `SALCHICHON_API_MIGRATION_DB_USER` y `SALCHICHON_API_MIGRATION_DB_PASSWORD`: credenciales DDL temporales/separadas.
- `SALCHICHON_WEB_ORIGINS`: dominio HTTPS del portal.
- `SALCHICHON_SERVER_TIMEZONE=America/Mexico_City`.
- `SALCHICHON_SEASON_NAME`, `SALCHICHON_SEASON_DESCRIPTION`, `SALCHICHON_SEASON_START_DATE` y `SALCHICHON_SEASON_DIFFICULTY`.

Al iniciar, la API registra en sus logs el nombre y la versión del motor SQL conectado. El plugin hace lo mismo en la consola de Paper. Estos datos se mantienen en logs privados y no se exponen en endpoints públicos.

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
- MySQL, backups y SQLite original permanecen privados y disponibles para rollback.

## Límites de la primera publicación

- Player Value incluye el selector tipo cofre, pero permanece desactivado hasta completar pruebas de recuperación de depósitos/retiros y validación en staging.
- Los intercambios siguen a cargo del plugin externo.
- El panel administrativo requiere autenticación/autorización antes de habilitar acciones.
- Los niveles/atributos de mobs se implementarán en una fase posterior.
