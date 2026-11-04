---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-04
---

# CONTRACT — HU058 / HU155 / HU167

| Método y endpoint | Roles | Respuesta |
|---|---|---|
| `GET /api/v1/docente/asignaturas` (existente, aditivo) | DOCENTE | `ApiListResponse` de `{ idAsignatura, nombreAsignatura, idGrupo, nombreGrupo, idPrograma, nombrePrograma, idTipoPrograma, nombreTipoPrograma }`. Los dos campos nuevos son nulos si el grupo no tiene programa |
| `GET /api/v1/tipos-programa` (existente, HU023) | Autenticado | Catálogo de tipos de programa activos (opciones del filtro "Nivel formativo") |
| `GET /api/v1/admin/estados` (nuevo) | ADMINISTRADOR | `ApiListResponse` de `{ catalogo, id, codigo, nombre }`. `catalogo` es `SOLICITUD` (`uv_estado`) o `ESTUDIANTE_GRUPO` (`uv_estado_estudiante_grupo`); orden por catálogo y nombre |

**Errores:** 403 sin el rol; 500 ante fallo técnico de la DB. No hay parámetros ni escritura: son catálogos de solo consulta.
