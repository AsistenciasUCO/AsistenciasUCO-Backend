---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-04
---

# CONTRACT MATRIX — HU061-HU147-HU153-HU172-consolidado-auditoria-catalogos

Formulario del [protocolo de alineación contractual](../../integration/CONTRACT_ALIGNMENT_PROTOCOL.md). Sin implementación. Sin secretos.

## Evidencia

| Sistema | Repo/versión | Archivo/objeto | Hash/commit | Autoridad |
|---|---|---|---|---|
| DB | gestion-asistencia-db rama `feature/HU061-HU172-consolidado-asistencia-auditoria` | SP y vista del work item homónimo de la BD | pendiente de PR | OWNER |
| Backend | AsistenciasUCO rama `feature/HU061-HU172-consolidado-asistencia-auditoria` | `ConsultasTransversalesJpaRepository`, `TipoIdentificacionCommandJpaRepository`, `ConsolidadoAsistenciaController`, `AdminConsultasTransversalesController`, `AdminTipoIdentificacionController` | pendiente de PR | CONSUMER |

## Matriz

| ID | Capacidad | Provider contract | Backend expectation | Status | Evidence | Impact | Owner action |
|---|---|---|---|---|---|---|---|
| C-061 | Consolidado por grupo | READ_QUERY_RESULTSET de 21 columnas (20 + `totalRegistros`); `@idPeriodoAcademico`, `@idPrograma`, `@idAsignatura`, `@idDocente` | `GET /api/v1/coordinador/consolidado-asistencia` y `/decano/…` | MATCH | T-061-01 + `ConsultasConsolidadoAuditoriaJpaRepositoryTest` | — | — |
| C-172 | Rastro de auditoría | 18 columnas (17 + `totalRegistros`); `@desdeUtc`/`@hastaUtc` `DATETIMEOFFSET(7)` obligatorios (`GEN_002`) | `GET /api/v1/admin/auditoria` con rango UTC normalizado | MATCH | T-172-01 + T_172_UC/PAT | — | — |
| C-147 | Componentes | 4 columnas, sin paginación | `GET /api/v1/coordinador/componentes-formacion` | MATCH | T-147-01 + T_147_PAT_01 | — | — |
| C-153 | Guardar tipo de documento | Canónico de 4 columnas; `@idTipoIdentificacion` nulo = alta | `POST`/`PUT /api/v1/admin/tipos-identificacion` vía `JpaProcedureExecutor` | MATCH | T-153-01 + `TipoIdentificacionCommandJpaRepositoryTest` | — | — |
| C-P5 | Crear/editar asignatura | `ASI_001`, `PLA_002`, `PLA_004`, `SEC_002`, `GEN_001` | Cuerpo `{ codigo, nombre, creditos, semestreNumero, nombreArea, nombreComponente }`; `ASI_001` → 400, `GEN_001` → 404 | MATCH (antes MISMATCH: el frontend enviaba `area`/`componente`/`semestre`) | T-P5-01, T-ASI-01, RA-05 + `DbFailureClassifierAsignaturaTest` + spec del formulario | El formulario ahora envía los campos del contrato | — |
