---
status: active
type: work-item
scope: backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-06
---

# PLAN — Retiro de sedes y espacios físicos (HU020/HU140 descartadas)

## Decisión de John (2026-11-06)

Las HU020, HU113, HU114, HU115, HU118, HU125, HU126, HU127, HU129, HU140 y HU141 no se piden y quedan DESCARTADAS. Con su texto original, del plan de la BD, duplican HU que ya existen. Lo que el catálogo les había agregado no se pide: registro calificado, suspensión de oferta, coherencia, bloqueo de facultad, departamentos, contratos, horas, escalafón, sedes y aulas.

De ese grupo, solo las sedes y los espacios físicos (HU020/HU140) tenían código: stubs que respondían 501. Este work item los retira. HU118 no tenía endpoint y la BD no cambia.

## Alcance

| Capa | Cambio |
|---|---|
| Backend | Se retiran de `AdminPortalController` los 8 endpoints 501: `GET/POST /api/v1/admin/sedes`, `PUT /api/v1/admin/sedes/{id}`, `PATCH /api/v1/admin/sedes/{id}/estado` y los 4 equivalentes de `/api/v1/admin/espacios-fisicos`. También se retiran sus 8 rutas de `access-catalog.json`. `SecurityConfig` deja la excepción de lectura (ADMINISTRADOR, DECANO, COORDINADOR) solo para `/api/v1/admin/areas`. Los scripts e2e del baseline usan `/api/v1/admin/areas` como ruta representativa del administrador |
| Frontend | Se retiran la pantalla `/app/admin/sedes` (`AdminSedesComponent`), su menú, los métodos de sedes y espacios de `AdminManagementService`, sus modelos, los mocks y la excepción de lectura de `sedes` en `role-scope.interceptor`. Las pantallas del decano "Áreas de Conocimiento" (antes "Áreas y Sedes") y "Mi Facultad" ya no consultan ni muestran sedes ni espacios físicos; las áreas no cambian |

## PDP

Rutas retiradas, para el `--prune` (solo con aprobación de John):

- `GET /api/v1/admin/sedes`
- `POST /api/v1/admin/sedes`
- `PUT /api/v1/admin/sedes/{id}`
- `PATCH /api/v1/admin/sedes/{id}/estado`
- `GET /api/v1/admin/espacios-fisicos`
- `POST /api/v1/admin/espacios-fisicos`
- `PUT /api/v1/admin/espacios-fisicos/{id}`
- `PATCH /api/v1/admin/espacios-fisicos/{id}/estado`

## Pruebas

- `AdminPortalControllerTest.sedes_y_espacios_fisicos_retirados_no_tienen_ruta`: el controlador ya no mapea ninguna ruta de `/sedes` ni de `/espacios-fisicos`.
- `AccessCatalogConsistencyTest`: el catálogo y los controladores coinciden.
- Frontend: `role-scope.interceptor.spec` (`admin/sedes` ya no es una excepción de lectura), `dean-infraestructura` y `dean-faculty` (no consultan sedes ni espacios).

## Rollback

Revertir el commit en backend y frontend.
