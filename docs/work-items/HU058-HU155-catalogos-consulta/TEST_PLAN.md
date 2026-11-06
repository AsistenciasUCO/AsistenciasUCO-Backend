---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-04
---

# TEST_PLAN — HU058 / HU155 / HU167

## DB

| ID | Caso |
|---|---|
| T-155-01 (`test_consolidado_auditoria_catalogos.sql`) | `uv_estado` expone `codigo` y `nombre` de los 5 estados de solicitud del seed; `uv_estado_estudiante_grupo` lista los estados del estudiante (incluye `A` y `CI`) |

## Backend

| Test | Caso |
|---|---|
| `AsignaturaDocenteJpaRepositoryTest` | Agrega el tipo de programa de cada grupo desde `uv_programa`; un grupo sin programa queda con el tipo nulo; sin programas no consulta `uv_programa` |
| `ViewEntityProjectionMappingTest`, `ConsultarAsignaturasDocenteUseCaseImplTest`, `DocentePortalControllerTest` | Proyección, dominio y DTO con los dos campos nuevos |
| `AsignaturaDocenteJdbcBaseline` (paridad IT) | La línea base JDBC hace el mismo `LEFT JOIN` a `uv_programa` |
| `ConsultasConsolidadoAuditoriaUseCaseImplTest`, `…InteractorTest`, `…JpaRepositoryTest`, `AdminConsultasTransversalesControllerTest` | `GET /admin/estados`: delega, mapea catálogo, código y nombre, y lee las dos vistas |
| `AccessCatalogConsistencyTest` | Ruta nueva `GET /api/v1/admin/estados` |

## Frontend

| Spec | Caso |
|---|---|
| `teacher-grupos-list.component.spec.ts` | Opciones del filtro "Nivel formativo" con el catálogo real; filtrar por postgrado y volver a "Todos"; tipo visible en la tarjeta; sin catálogo no hay filtro |
| `teacher-grupos.component.spec.ts` | Carga el catálogo de tipos de programa |
| `course.service.spec.ts` | Conserva el tipo de programa de la asignación |
| `admin-estados.component.spec.ts` | Dos tablas sin columna de estado ni acciones; separación por catálogo; error del backend |
| `consultas-transversales.service.spec.ts`, `mock-consultas-transversales.spec.ts` | `GET /admin/estados` y su simulación |

Validación manual: MV-014 del ledger.
