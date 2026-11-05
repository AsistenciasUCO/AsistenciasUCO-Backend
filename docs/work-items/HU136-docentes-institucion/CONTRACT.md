---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-05
---

# CONTRACT — HU136

| Método y endpoint | Roles | Respuesta |
|---|---|---|
| `GET /api/v1/admin/docentes` (nuevo) | ADMINISTRADOR | `ApiListResponse` de `{ idDocente, numeroIdentificacion, nombreCompleto, correo, activo, programas, totalProgramas, totalGrupos, tieneVinculoActivo }` y `total`. Docentes de la institución del administrador. Filtros opcionales: `busqueda` (nombre o documento), `idFacultad`, `idPrograma`, `estaActivo` y `estadoVinculo` (`ACTIVO` por defecto, `SIN_VINCULO` o `TODOS`; otro valor da 400). Facultad y programa filtran por vínculos activos |
| `GET /api/v1/decano/docentes` (HU128, aditivo) | DECANO, ADMINISTRADOR | La respuesta suma `tieneVinculoActivo`, siempre `true` en el modo decano |
| `POST /api/v1/admin/instituciones`, `PUT /api/v1/admin/instituciones/{id}`, `PATCH /api/v1/admin/instituciones/{id}/toggle-estado` | — | Retiradas (antes respondían 501) |

**Errores:** 400 (paginación, `PAG_001`); 403 si el perfil no corresponde (`SEC_001`).
