---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-05
---

# PLAN — HU005 / HU016: motivos para justificar una inasistencia

## Identidad y objetivo

- **HU005 y HU016** (mismo texto en el plan de la BD: "Ver los posibles motivos para justificar una ausencia o llegada tarde"): el estudiante consulta las categorías con las que puede justificar un reclamo.
- **Work Item ID:** `HU005-HU016-categorias-justificacion`. Rama `feature/HU005-HU016-categorias-justificacion` en BD, backend y frontend.
- **Regla vigente (John, 2026-11-04):** los catálogos base son de solo consulta (CAT-01).

## Decisiones de John (2026-11-05)

- Los "motivos" son las 6 categorías del formulario de reclamo: Médico / Salud, Calamidad Doméstica, Académico / Representación, Fuerza Mayor, Laboral y Otro. Son las definitivas.
- La categoría pasa a una tabla de catálogo (`CategoriaJustificacion`). Se guardará en el reclamo cuando se haga el cambio coordinado de LB-004D (TD-060). Este work item no toca objetos congelados.

## Alcance

| Capa | Cambio |
|---|---|
| DB | Tabla `CategoriaJustificacion` (id, codigo, nombre, descripcion), seed de 6 filas, vista `uv_categoria_justificacion`, migración 12 y prueba T-005-01. Ver `docs/work-items/HU005-HU016-categorias-justificacion/PLAN.md` en el repo de BD |
| Backend | `GET /api/v1/categorias-justificacion` en `CategoriaJustificacionController`, sobre el puerto `ConsultasTransversales*` (lectura nativa de la vista, sin SP: no hay ámbito ni escritura). La ruta queda cubierta por `/api/v1/**` autenticado de `SecurityConfig`, que no cambia. Ruta nueva en `access-catalog.json` |
| Frontend | El formulario de reclamo lista las categorías del catálogo, en vez de la lista fija, y muestra la descripción de la elegida (HU005). Sigue enviando el **nombre** en `categoria`, así que el contrato de radicación no cambia. Si el catálogo no carga, no deja radicar y lo informa. El detalle del curso del estudiante muestra el bloque desplegable "¿Por qué motivos puedo justificar una inasistencia?" (HU016) |

## Fuera de alcance

La FK de la categoría en `SolicitudRevisionAsistencia` (TD-060), el plazo de radicación (HU026B), el soporte obligatorio por categoría y HU014 dependen de `usp_radicar_solicitud_revision_asistencia_v2` (manifiesto LB-004D). Quedan diseñados en `DISENO_CAMBIO_COORDINADO.md` del work item de la BD, para José Alejandro.

## Rollback

Revertir los commits de las 3 capas y aplicar `migrations/12_categoria_justificacion_down.sql`.
