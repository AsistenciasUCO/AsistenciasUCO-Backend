---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-05
---

# CONTRACT — HU005 / HU016

| Método y endpoint | Roles | Respuesta |
|---|---|---|
| `GET /api/v1/categorias-justificacion` (nuevo) | Autenticado (ADMINISTRADOR, DECANO, COORDINADOR, DOCENTE, ESTUDIANTE) | `ApiListResponse` de `{ id, codigo, nombre, descripcion }`, ordenada por nombre. `descripcion` puede ser nula |
| `POST /api/v1/estudiante/reclamos` (existente, sin cambios) | ESTUDIANTE | `categoria` sigue siendo el texto del nombre de la categoría |

**Errores:** 401 sin sesión; 500 ante un fallo técnico de la DB. No hay parámetros ni escritura: es un catálogo de solo consulta y no tiene datos personales.
