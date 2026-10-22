---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-21
---

# PLAN — HU117 + HU110 + HU111: Gestión de Facultad por el Administrador

Fase 01 (Planificación y DoR) conforme a `GUIA_METODICA_IMPLEMENTACION_HU.md` y `AGENTS.md`.
**Dictamen DoR: `READY`** (D1–D5 aprobadas por el owner el 2026-10-21).

## 1. Identidad y objetivo

- **HUs:** HU110 (modificar denominación de la facultad), HU111 (alternar estado operativo), HU117 (reasignar decano responsable).
- **Work item:** `HU117-HU110-HU111-gestion-facultad`. Rama sugerida por repo: `feature/HU117-HU110-HU111-gestion-facultad`.
- **Actor:** Administrador Institucional. Nivel 1 de la hoja de ruta (Etapa 1.3 y 1.4).
- **Objetivo:** cerrar la custodia del Administrador sobre la Facultad. Hoy `PUT /admin/facultades/{id}` y `PATCH /admin/facultades/{id}/estado` lanzan `FeatureUnavailableException` (HTTP 501).
- **Por qué van juntas:** las tres tocan el mismo controller, la misma tabla `dbo.Facultad`, el mismo `FacultadCommandPort` y la misma pantalla `admin-facultades`.

## 2. AS-IS verificado en código (2026-10-21)

| Capa | Hallazgo |
| :--- | :--- |
| **DB** | `dbo.Facultad(id, nombre nvarchar(50), institucion, decano NOT NULL, estado bit)`. Existen `usp_crear_facultad` y `usp_crear_facultad_interno` (HU116). No existe SP de actualización ni de cambio de estado. `FK_Facultad_Decano` y `FK_Facultad_Institucion` en `zz_foreign_keys.sql`. No hay columna de sede. |
| **Backend** | `AdminPortalController` (`/api/v1/admin`): `PUT /facultades/{id}` y `PATCH /facultades/{id}/estado` son stubs 501. `FacultadCommandPort` solo tiene `crearFacultad`, implementado en `FacultadJpaRepository` con `JpaProcedureExecutor`. `SecurityConfig` ya protege `/api/v1/admin/**` con `ADMINISTRADOR`, por lo que **no hay que tocar `SecurityConfig`** (archivo caliente). |
| **Frontend** | La pantalla real es `features/admin/admin-facultades` (no `DeanFaculty`, como dice el catálogo). Ya tiene editar, alternar estado y "Cambiar Decano(a)". `AdminManagementService` llama a los endpoints 501 y **oculta el fallo**: `asignarDecanoFacultad` y `desvincularDecanoFacultad` hacen actualización optimista y `catchError(() => of(true))`, y `cambiarEstadoFacultad`/`actualizarFacultad` hacen `subscribe()` sin manejar el error. Hoy la UI muestra éxito aunque nada se persista. |
| **Catálogo** | `HU-CATALOG.md` marca HU110, HU111 y HU117 como `[BLOQUEADO 501]`. HU117 dice "sede y decano", pero `Facultad` no tiene sede. |

## 3. TARGET

1. **DB:** dos SP públicos con fachada + `*_interno` atómico, `SET XACT_ABORT ON`, tupla estándar y RBAC `ADMINISTRADOR`:
   - `usp_actualizar_facultad` (HU110 + HU117: nombre y/o decano).
   - `usp_cambiar_estado_facultad` (HU111).
2. **Backend:** habilitar `PUT /api/v1/admin/facultades/{id}` y `PATCH /api/v1/admin/facultades/{id}/estado` con casos de uso `ActualizarFacultad` y `CambiarEstadoFacultad`. Se amplía `FacultadCommandPort`. Respuesta `ApiDataResponse<FacultadDTO>` releyendo `uv_facultad`.
3. **Frontend:** `AdminManagementService` pasa a devolver `Observable` real. Se elimina la actualización optimista que miente y los `catchError(() => of(true))`. La UI refleja el resultado del backend y muestra `mensajeUsuario` en error. Se elimina `desvincularDecanoFacultad`, porque `decano` es `NOT NULL`.

## 4. Alcance y no alcance

**En alcance:** HU110, HU111, HU117 (decano), tests en las 3 capas, corrección del service del frontend, actualización de catálogo y hoja de ruta.

**Fuera de alcance:**
- HU118 (bloqueo temporal de operaciones de la facultad): es otra HU y requiere definir efectos operativos.
- Cascadas sobre programas, coordinadores o grupos al inactivar (ver decisión D3).
- Sedes: no existen en el modelo (ver D1).
- Alta o inactivación de la cuenta del Decano (HU117-B ya certificada; IAM-001 en el programa REG-000).
- Cambio de institución de la facultad.

## 5. Perímetro de rutas

- **Permitidas:** `GestioAsistenciaDB/schema/stored-procedures/usp_actualizar_facultad*.sql`, `usp_cambiar_estado_facultad*.sql`, `GestioAsistenciaDB/test/test_usp_actualizar_facultad.sql`, `test_usp_cambiar_estado_facultad.sql`; backend `application/features/admin/{actualizarfacultad,cambiarestadofacultad}/**`, `FacultadCommandPort`, `FacultadJpaRepository`, `AdminPortalController`, wiring de admin y sus tests; frontend `core/services/admin-management.service*.ts`, `features/admin/admin-facultades/**`, `mock-backend.interceptor*.ts` solo si hace falta.
- **Prohibidas (archivos calientes):** `SecurityConfig`, `RbacSecurityFilterChainTest`, `dean-*`, `admin-areas`, `admin-catalogs` (trabajo de otros), `schema/tables/**` y contratos WORM sin proceso formal.
- **`test_suite.sql` y `HTTP_AS_IS_MATRIX.md`:** se tocan al final y en un commit aparte, porque otros work items también los editan.

## 6. Riesgos y rollback

- **Colisión con otros PRs:** HU076 (`DeanFaculty`), HU062/089/091 y HU151/152 modifican wiring y OpenAPI. Se mitiga con merge frecuente y avisando al congelar el contrato.
- **Regresión de UX:** quitar la actualización optimista cambia el comportamiento percibido; los specs del componente y del service se actualizan por cambio formal de contrato, no para forzar verde.
- **Integridad:** reasignar decano sin regla de unicidad puede dejar un decano con dos facultades y romper `UserScopeService.resolveFacultadId()` (ver D2).
- **Rollback:** los SP son nuevos (`DROP PROCEDURE`); el controller vuelve a los stubs con `git revert`. No hay cambios de DDL.

## 7. Decisiones abiertas para el owner

| # | Decisión | Recomendación |
| :-: | :--- | :--- |
| **D1** | HU117 dice "sede y decano", pero no existe sede en `Facultad`. ¿Se interpreta como reasignar solo el decano? | **Solo decano.** Registrar la parte "sede" como deuda técnica y como HU del catálogo de sedes (HU de `/admin/sedes`, hoy 501). |
| **D2** | ¿Un decano puede ser titular de más de una facultad activa? | **No.** Rechazar con conflicto si el decano nuevo ya es titular de otra facultad activa. Protege el scope por facultad. |
| **D3** | Al inactivar una facultad, ¿se bloquea si tiene programas activos o solo se cambia el flag? | **Solo flag, sin cascada.** `uv_decano` ya refleja `estaActivoDecano` según la facultad; el bloqueo operativo es HU118. |
| **D4** | ¿`PATCH /estado` es toggle o recibe el estado deseado? | **Recibe `{ "activa": true|false }`** (idempotente). El toggle ciego se corrompe con doble clic o con dos administradores. |
| **D5** | ¿Se permite renombrar y reasignar en una sola llamada `PUT`? | **Sí**, con campos opcionales: al menos uno es obligatorio. |

## 8. Definition of Ready

| Criterio | Estado |
| :--- | :---: |
| Objetivo y alcance delimitados | ✅ |
| AS-IS verificado en las 3 capas | ✅ |
| Contratos identificados (`CONTRACT.md` en borrador) | ✅ |
| Matriz de pruebas inicial (`TEST_PLAN.md`) | ✅ |
| Decisiones D1–D5 resueltas por el owner | ⏳ |
| Worktrees de DB, Frontend e Infra creados | ⏳ (se crean al aprobar) |

**Dictamen: `NOT_READY`.** Pasa a `READY` al aprobar D1–D5 y el CONTRACT.
