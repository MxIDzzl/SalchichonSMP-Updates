# Escuadrón Salchichón — Portal oficial

Este repositorio contiene la aplicación web independiente del plugin de Minecraft: portal público y punto de entrada al futuro panel administrativo.

## Stack
Next.js 15 · React 19 · TypeScript.

## Desarrollo
Requiere Node.js 20.9+.
```bash
npm install
npm run dev
```
Abre http://localhost:3000.

## Rutas
- `/`: portal público, temporada, estadísticas y novedades.
- `/admin`: entrada al área privada. Las acciones administrativas aún no están habilitadas.

## Estado
La interfaz pública ya está construida. Las estadísticas y novedades dinámicas esperan la API independiente y por ahora se muestran como no disponibles, sin cifras ficticias. El panel administrativo no modifica datos hasta implementar autenticación, autorización y auditoría en el backend.

La web nunca debe conectarse directamente a PostgreSQL. No guardar secretos en variables `NEXT_PUBLIC_*`. Configurar HTTPS y limitar CORS a los dominios oficiales.
