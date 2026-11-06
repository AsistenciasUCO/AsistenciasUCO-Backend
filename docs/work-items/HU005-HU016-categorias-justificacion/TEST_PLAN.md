---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-05
---

# TEST_PLAN — HU005 / HU016

## DB

| ID | Caso |
|---|---|
| T-005-01 (`test_consolidado_auditoria_catalogos.sql`) | Las 6 categorías del seed con nombre y descripción; el nombre de MED es el que envía el formulario; no hay SP de escritura del catálogo; el rol runtime tiene `SELECT` sobre `uv_categoria_justificacion` |

## Backend

| Test | Caso |
|---|---|
| `CategoriaJustificacionControllerTest` | `GET /categorias-justificacion` devuelve la lista con total; catálogo vacío; constructor sin puerto |
| `ConsultasConsolidadoAuditoriaUseCaseImplTest`, `…InteractorTest`, `…JpaRepositoryTest` | Delega en el puerto, mapea código, nombre y descripción, y lee la vista (código con espacios recortado; descripción nula) |
| `AccessCatalogConsistencyTest` | Ruta nueva `GET /api/v1/categorias-justificacion` |

## Frontend

| Spec | Caso |
|---|---|
| `student-claim-form.component.spec.ts` | Opciones del catálogo, preselección de la primera y descripción de la elegida; sin catálogo no radica y lo informa; sigue emitiendo el nombre |
| `student-courses.component.spec.ts` | Carga el catálogo y muestra los motivos en el detalle; si la carga falla queda vacío (sin lista de respaldo) |

Validación manual: MV-015 del ledger.
