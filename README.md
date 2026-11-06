# ✦ ESCUADRÓN SALCHICHÓN SMP ✦

<div align="center">

### Portal oficial · API · Actualizaciones

*La información pública del servidor, sus jugadores y su comunidad.*

</div>

Este repositorio contiene dos servicios separados:

- **Portal Next.js:** portada, estadísticas, directorio y perfiles de jugadores, rankings, clanes y archivo de novedades.
- **API Spring Boot:** lectura pública de datos de PostgreSQL y endpoints administrativos privados, desactivados por defecto.

El plugin Paper vive en [SalchichonHardcore](https://github.com/MxIDzzl/SalchichonHardcore). La web no administra clanes ni intercambios: lee el snapshot oficial de zMClans y las estadísticas del plugin; los trades siguen a cargo del plugin externo del servidor.

## Estado del portal

La portada y las páginas públicas muestran información real cuando la API está conectada. Si faltan datos, se indica que aún no están sincronizados; no se rellenan cifras, temporadas ni clanes ficticios.

La dificultad puede avanzar automáticamente por día de temporada mediante la configuración de etapas de la API. Esta primera fase solo publica la etapa actual; los niveles y atributos especiales de mobs quedan para una fase posterior.

## Despliegue

Consulta [DEPLOYMENT.md](DEPLOYMENT.md) para los requisitos, orden de despliegue, variables de entorno, permisos de PostgreSQL y verificaciones de aceptación.

## Desarrollo

Web:
```bash
npm install
npm run dev
npm run build
```

API:
```bash
cd api
mvn clean verify
mvn spring-boot:run
```

La web consulta la API desde el servidor mediante `SALCHICHON_API_URL`. Nunca expongas credenciales de PostgreSQL ni tokens en variables `NEXT_PUBLIC_*`.

## Seguridad y límites de la primera publicación

- PostgreSQL debe permanecer privado.
- La API pública utiliza un rol SELECT-only.
- El panel /admin permanece sin operaciones y la API administrativa desactivada hasta completar autenticación y autorización reales.
- Player Value ya tiene selector tipo cofre; permanece desactivado hasta completar pruebas de recuperación y staging.
- Los intercambios los gestiona el plugin externo.
- La sincronización de clanes es de solo lectura desde zMClans.

## Enlaces

- [Plugin Paper](https://github.com/MxIDzzl/SalchichonHardcore)
- [Workflow de API](https://github.com/MxIDzzl/SalchichonSMP-Updates/actions/workflows/api-ci.yml)
- [Workflow de web](https://github.com/MxIDzzl/SalchichonSMP-Updates/actions/workflows/web-ci.yml)
