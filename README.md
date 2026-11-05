# Escuadrón Salchichón — Portal oficial

Este repositorio contiene dos servicios desplegables por separado:
- `/`: portal Next.js público y futura consola administrativa.
- `api/`: servicio Spring Boot independiente para datos públicos del servidor.

El plugin Paper sigue en su propio repositorio. La API y la web comparten este repositorio por ahora, pero cada una tiene su propio proceso de build y despliegue.

## Web
Requiere Node.js 20.9+.
```bash
npm install
npm run dev
```
Configura `SALCHICHON_API_URL` como variable de entorno del servidor Next.js (por ejemplo `http://127.0.0.1:8080`). Es una URL server-side; no colocar secretos en variables `NEXT_PUBLIC_*`.

## Despliegue independiente de la web

La web incluye un Dockerfile multi-stage y se ejecuta como proceso independiente:
```bash
docker build -t salchichon-web .
docker run --rm -p 3000:3000 -e SALCHICHON_API_URL=http://host.docker.internal:8080 salchichon-web
```
En Linux, configura la dirección privada real de la API en `SALCHICHON_API_URL`; no publiques esta variable como `NEXT_PUBLIC_*`. Termina TLS en un proxy inverso y limita el acceso de red entre web y API.

## API
Requiere Java 25, Maven 3.9+ y PostgreSQL.
```bash
cd api
mvn clean verify
mvn spring-boot:run
```
Consulta `api/README.md` para endpoints, variables de entorno, CORS y permisos PostgreSQL.

## Seguridad y estado
La web consulta la API desde el servidor Next.js; no se conecta directamente a PostgreSQL. La API pública es de solo lectura y debe usar un rol PostgreSQL SELECT-only. No expongas PostgreSQL a internet. El panel administrativo todavía no permite mutaciones; faltan autenticación, autorización y auditoría. La validación de licencias del plugin también está pendiente.
