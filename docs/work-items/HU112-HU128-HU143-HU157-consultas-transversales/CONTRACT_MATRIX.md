---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-02
---

# CONTRACT MATRIX — HU112-HU128-HU143-HU157-consultas-transversales

Formulario del [protocolo de alineación contractual](../../integration/CONTRACT_ALIGNMENT_PROTOCOL.md). Sin implementación. Sin secretos.

## Evidencia

| Sistema | Repo/versión | Archivo/objeto | Hash/commit | Autoridad |
|---|---|---|---|---|
| DB | gestion-asistencia-db, rama `feature/HU112-HU128-HU143-HU157-consultas-transversales` | `usp_consultar_programas_creditos_facultad_paginado`, `usp_consultar_docentes_facultad_paginado`, `usp_buscar_asignaturas_paginado`, `usp_buscar_estudiantes_institucion_paginado`, `usp_consultar_historial_estudiante` | pendiente del PR | OWNER |
| Backend | AsistenciasUCO, rama homónima | `ConsultasTransversalesJpaRepository`, `DecanoConsultasTransversalesController`, `AdminConsultasTransversalesController` | pendiente del PR | CONSUMER |

## Matriz

| ID | Capacidad | Provider contract | Backend expectation | Status | Evidence | Impact | Owner action |
|---|---|---|---|---|---|---|---|
| C-112 | Programas y créditos | READ_QUERY_RESULTSET de 13 columnas (12 + `totalRegistros`); `@busqueda`, `@estaActivo` | `GET /api/v1/decano/programas-creditos` | MATCH | T-112-01 + `ConsultasTransversalesJpaRepositoryTest` | — | — |
| C-128 | Docentes por programa | 10 columnas (9 + `totalRegistros`); `@idPrograma`, `@busqueda`, `@estaActivo` | `GET /api/v1/decano/docentes` | MATCH | T-128-01 | — | — |
| C-143 | Búsqueda de asignaturas | 15 columnas (14 + `totalRegistros`); `@busqueda`, `@idFacultad`, `@estaActiva` | `GET /api/v1/decano/asignaturas`, `GET /api/v1/admin/asignaturas` | MATCH | T-143-01 | — | — |
| C-157a | Buscador de estudiantes | 10 columnas (9 + `totalRegistros`); `@busqueda`, `@idFacultad`, `@estaActivo` | `GET /api/v1/admin/estudiantes` | MATCH | T-157-01 | — | — |
| C-157b | Ficha unificada | 27 columnas, una fila por matrícula; 0 filas fuera del ámbito | `GET /api/v1/{admin,decano}/estudiantes/{id}/historial` (404 si no hay filas) | MATCH | T-157-02 | — | — |

## Tipos y campos

| Campo | DB type/nullability | Java type/nullability | Semántica | Status |
|---|---|---|---|---|
| `id*` | UNIQUEIDENTIFIER (puede llegar como texto) | UUID (`NativeRowValues.uuid`) | Identificadores | MATCH |
| `numeroIdentificacion` | INT/BIGINT | String | Documento | MATCH |
| `estaActivo*` / `estaActiva*` | BIT o INT (`IIF`) | boolean (`1`/`true`) | Bandera de estado | MATCH |
| `totalSemestres`, `totalAsignaturas`, `totalCreditos` | INT (agregados, 0 sin filas) | int | Totales del plan | MATCH |
| `porcentajeInasistencia` | DECIMAL(5,2) | BigDecimal | % de faltas sin justa causa | MATCH |
| `programas`, `facultades` | NVARCHAR(MAX) (`STRING_AGG`) | String | Lista separada por comas | MATCH |

## Códigos/enums

`VAL_001`, `SEC_001`, `PAG_001`; `codigoPermanencia` y `codigoEstadoEstudiante` vienen tal como los entregan las vistas de HU069.

## Error semantics

`PAG_001`/`VAL_001` → 400; `SEC_001` → 403; historial sin filas → 404; fallo técnico → `DatabaseOperationException` (500).

## Transacción/atomicidad

Solo lectura; los SP capturan y restauran `SESSION_CONTEXT` y no condenan la transacción del llamador.

## Tiempo/UTC

Sin fechas en las respuestas (el período se identifica por código y nombre).

## Decisiones requeridas

| ID mismatch | OWNER | CONSUMER | Opciones | Decisión / aprobador / referencia |
|---|---|---|---|---|
| NO APLICA | — | — | — | Sin mismatches abiertos. Decisiones de alcance D1-D4 de John en el [PLAN](PLAN.md). |

## Bloqueos

Ninguno.

## Contrato congelado

PENDIENTE DE PR (aprobación de John al abrir y mergear los PR).
