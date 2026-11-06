---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-05
---

# PLAN — HU136 (docentes de la institución), HU135 y HU161/HU162/HU164

## Decisiones de John (2026-11-05)

- **HU136**, según el texto original del plan de la BD: "Ver la lista de todos los profesores de la universidad". Es un directorio de solo consulta para el ADMINISTRADOR. Se descarta el "contador en tiempo real".
- **Ajuste aprobado por John (2026-11-05):** el administrador también ve a los docentes sin vínculo activo, por ejemplo los retirados de todos sus programas.
  - Filtro "Vínculo": con vínculo activo (por defecto), sin vínculo activo o todos.
  - En la fila de un docente sin vínculo, programa y grupos quedan vacíos.
  - Un docente que nunca tuvo vínculo no se puede atribuir a una institución y no aparece.
  - El modo DECANO (HU128) no cambia.
- **HU135** ("Ver la lista de todos los estudiantes"): queda cubierta por el buscador de estudiantes del administrador (HU157). Solo cambia la documentación; los contadores del tablero siguen igual.
- **HU161, HU162 y HU164** (crear, editar y activar instituciones): DESCARTADAS. `Institucion` es un catálogo de solo consulta (ampliación de CAT-01). La consulta `GET /api/v1/admin/instituciones` (HU163) se conserva.

## Alcance

| Capa | Cambio |
|---|---|
| DB | `usp_consultar_docentes_facultad_paginado` acepta ADMINISTRADOR (las facultades de su institución) y un filtro opcional `@idFacultad`. El resultset es el mismo y HU128 no cambia. Prueba T-136-01 |
| Backend | `GET /api/v1/admin/docentes` (`busqueda`, `idFacultad`, `idPrograma`, `estaActivo`, `pagina`, `tamano`) en `AdminConsultasTransversalesController`. `ConsultasTransversalesQueryPort.consultarDocentes` gana `idFacultad`; la variante del decano lo delega con `null`. Se retiran los 3 endpoints 501 de `/admin/instituciones` de `AdminPortalController` y del `access-catalog`. `SecurityConfig` no cambia |
| Frontend | `AdminDocentesComponent` (`/app/admin/docentes`, menú "Docentes"), mismo estilo que Estudiantes del administrador. La pantalla de catálogos del administrador deja de ofrecer crear, editar y activar instituciones |

## PDP

- Ruta nueva: `GET /api/v1/admin/docentes`.
- Rutas retiradas, para el `--prune` (solo con aprobación de John): `POST /api/v1/admin/instituciones`, `PUT /api/v1/admin/instituciones/{id}` y `PATCH /api/v1/admin/instituciones/{id}/toggle-estado`.

## Rollback

Revertir los commits.
