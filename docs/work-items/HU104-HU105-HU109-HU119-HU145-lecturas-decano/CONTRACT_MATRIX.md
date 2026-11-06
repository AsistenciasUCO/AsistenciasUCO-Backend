---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-26
---

# CONTRACT MATRIX — HU104-HU105-HU109-HU119-HU145-lecturas-decano

Formulario del [protocolo de alineación contractual](../../integration/CONTRACT_ALIGNMENT_PROTOCOL.md). Sin implementación. Sin secretos.

## Evidencia

| Sistema | Repo/versión | Archivo/objeto | Hash/commit | Autoridad |
|---|---|---|---|---|
| DB | gestion-asistencia-db `develop` | `usp_consultar_estudiantes_facultad_paginado`, `usp_consultar_estudiante_facultad_detalle`, `usp_consultar_estructura_facultad`, `usp_consultar_sesiones_facultad_paginado` | PR #44 (merge `b935f28`) | OWNER |
| Backend | AsistenciasUCO `develop` | `FacultadDecanoJpaRepository`, `DecanoFacultadConsultaController` | PR #41 (merge `758d555`) | CONSUMER |

## Matriz

| ID | Capacidad | Provider contract | Backend expectation | Status | Evidence | Impact | Owner action |
|---|---|---|---|---|---|---|---|
| C-104 | Directorio de estudiantes | READ_QUERY_RESULTSET + `totalRegistros`; `@busqueda`, `@idPrograma`, `@estaActivo` | `GET /api/v1/decano/estudiantes` | MATCH | T-104-01 + `FacultadDecanoJpaRepositoryTest` | — | — |
| C-105 | Ficha del estudiante | Filas de matrícula por `@idEstudiante` | `GET /api/v1/decano/estudiantes/{id}` (404 si no hay filas) | MATCH | T-105-01 | — | — |
| C-119 | Estructura de la facultad | Filas facultad/programa/plan con `clave` única | `GET /api/v1/decano/estructura` (árbol armado en el interactor) | MATCH | T-119-01 | — | — |
| C-145 | Cronograma de sesiones | READ_QUERY_RESULTSET + `totalRegistros`; `@fechaDesde`/`@fechaHasta` inclusivos, `@idPrograma` | `GET /api/v1/decano/sesiones` | MATCH | T-145-01, T-PAG-01 | — | 2026-11-06, aditivo (APP-04): `fechaHoraInicioUtc`/`fechaHoraFinUtc`/`estadoTemporal`; ver CERT-F3-CORRECCIONES |

## Tipos y campos

| Campo | DB type/nullability | Java type/nullability | Semántica | Status |
|---|---|---|---|---|
| `numeroIdentificacion` | INT | String (convertido) | Documento | MATCH |
| `programas` | NVARCHAR(MAX) (`STRING_AGG`) | String | Programas del estudiante | MATCH |
| `estaActivoEstudiante` / `estaActivoPlanEstudio` | INT (`IIF`) | Boolean | Bandera de estado | MATCH |
| `clave` | NVARCHAR | String (`@Id`) | `facultad\|programa\|plan` | MATCH |

## Códigos/enums

`PAG_001`, `VAL_001`.

## Error semantics

`PAG_001` → 400; usuario sin facultad de decano → 403; ficha sin filas → 404; fallo técnico → `DatabaseOperationException`.

## Transacción/atomicidad

Solo lectura; los SP capturan y restauran `SESSION_CONTEXT`.

## Tiempo/UTC

`@fechaDesde`/`@fechaHasta` son DATE (días completos); `fechaHoraInicio`/`fechaHoraFin` como LocalDateTime.

## Decisiones requeridas

| ID mismatch | OWNER | CONSUMER | Opciones | Decisión / aprobador / referencia |
|---|---|---|---|---|
| NO APLICA | — | — | — | Sin mismatches abiertos. |

## Bloqueos

Ninguno.

## Contrato congelado

APPROVED

Evidence of approval: PR integrados en `develop` con CI remoto en verde; [VALIDATION](VALIDATION.md).
