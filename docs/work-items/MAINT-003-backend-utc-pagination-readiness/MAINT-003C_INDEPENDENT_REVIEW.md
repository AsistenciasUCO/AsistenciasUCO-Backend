---
status: active
type: audit
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---

# MAINT-003C — revisión independiente (PR #18, UTC-D06 A+C+E, UTC-D02)

Revisión conceptualmente independiente: verifica y reporta; no corrige PR #18 ni las propuestas previas. Fuentes leídas en solo lectura: GitHub/SonarCloud públicos, código de las ramas, base local `gestionasistenciadb` (SQL Server 2022 `16.0.4265.3`) y repo owner DB `johnjduque/gestion-asistencia-db` `f2871a9` (sin commits nuevos desde 2026-10-01 tras `git fetch`).

## 1. PR #18 — checks del último commit

SHA auditado: `7e2827d1aff87b5371c349fdc9cd73b72d14f3a9` (head de `jose-valencia/maint-001-jpa-pagination-safety`, PR abierto, base `develop`, `mergeable_state=clean`).

| Check-run | Conclusión |
|---|---|
| Backend Quality Gate | success |
| SonarCloud Code Analysis | success |
| CodeQL / CodeQL Java Analysis | success / success |
| Trivy / Trivy Repository Scan / Trivy Production Docker Image Scan | success ×3 |
| ArchUnit Architectural Guardrails | success |
| Dependency Review | success |

Total 9/9 `success`, todos con `head_sha=7e2827d1`. Sonar `api/qualitygates/project_status?pullRequest=18`: `OK` (new coverage 90.3 %, duplicación 0.0 %, ratings A, hotspots revisados 100 %); `project_pull_requests/list` confirma análisis sobre `7e2827d1` (2026-10-09T22:54Z). PR #19 `3985b0ac` y PR #20 `b512bb40`: 9/9 `success` cada uno y Sonar `OK`.

## 2. PR #18 — coherencia de la clasificación de `MAINT-001/PLAN.md`

Contraste del texto de `7e2827d` con `git diff bfc4fd3..7e2827d` (merge-base con `develop`, 18 archivos):

| Afirmación del PLAN | Diff real | Dictamen |
|---|---|---|
| MAINT-00 histórico `DOCUMENTATION_ONLY`, no describe el PR acumulado | se separa explícitamente | COHERENTE (resuelve el P2 de la primera pasada) |
| Java de producción solo en adaptador de estudiantes / validador HTTP | `EstudianteJpaRepository.java`, `ConsultarEstudiantesRequestValidator.java` | COHERENTE |
| Tests JPA/HTTP/contrato, docs OpenAPI, script E2E Keycloak, work-item | presentes | COHERENTE |
| No cambia DB, SP, vistas, POM ni frontend | ningún archivo de esos tipos | COHERENTE |
| Rollback MAINT-01C por revert de `d0468e3`, `1c96162`, `e7803ca` | commits existen en el rango | COHERENTE |

Observaciones menores (no bloquean, P3 documental):

1. La enumeración de archivos afectados omite `.claude/skills/uco-persistencia/SKILL.md` (+4 líneas de guía), `docs/contracts/OPENAPI_STANDARD.md` (excepción de paginación por operación) e `infra/keycloak/.env.example`/`README.md`. Son documentación/configuración de ejemplo sin efecto runtime.
2. MAINT-01A conserva la frase «no añadir 400 inventado en este PR», mientras MAINT-01B añade el 400 por `page*size > Integer.MAX_VALUE` en el validador HTTP. No es contradicción material: el 400 usa el código existente `VALIDATION_ERROR`/`INVALID_PAGE` y quedó publicado en el OpenAPI (`d0468e3`), pero MAINT-01B se etiqueta `HTTP_VALIDATION_AND_SQL_PARITY_TESTS` cuando es un cambio de comportamiento HTTP acotado (antes llegaba al adapter).
3. Frontmatter `last-reviewed: 2026-10-08` no se actualizó.

**Dictamen PR #18:** `CHECKS_GREEN_9_OF_9 / SCOPE_CLASSIFICATION_COHERENT_WITH_MINOR_OMISSIONS`. Merge: no autorizado (decisión del usuario).

## 3. Evidencia DB adicional (solo lectura, 2026-10-09)

| Hecho | Consulta |
|---|---|
| `Sesion.fechaHoraInicio/Fin` y `DetalleAsistencia.fechaHoraInicio/Fin` son `datetime2(7)` NOT NULL | `sys.columns` |
| `dbo.Sesion` sin triggers, sin CHECK, no temporal, sin CDC | `sys.triggers`, `sys.check_constraints`, `sys.tables` |
| Escriben `Sesion`: `usp_crear_sesion` (INSERT), `usp_actualizar_sesion` (UPDATE), `usp_generar_sesiones_grupo` (INSERT con `AT TIME ZONE`) y la semilla `14_grupos_sesiones.sql` (MERGE directo) | `sys.sql_modules`, repo owner |
| Leen `Sesion`/`uv_sesion`: 7 SP más (`usp_registrar_asistencias_sesion`, `usp_registrar_asistencia_estudiante_autonomo`, `usp_ejecutar_cierre_masivo_periodo`, `usp_radicar/resolver_solicitud_revision_asistencia`, `usp_sincronizar_asistencia_estudiante_interno`, 3 `usp_validar_*_interno`) y 4 vistas (`uv_sesion`, `uv_auth_sesion`, `uv_asistencia`, `uv_detalle_asistencia`) | `sys.sql_expression_dependencies` |
| Las vistas usan listas de columnas explícitas (sin `SELECT *`) | `OBJECT_DEFINITION` |
| `usp_actualizar_sesion` actualiza solo `Sesion`; las copias en `DetalleAsistencia` solo se refrescan cuando `usp_sincronizar_asistencia_estudiante_interno` vuelve a registrar asistencia | SP owner líneas 136-142 y 154-163 |
| Fuente: 3 sesiones, 9 detalles (todos con fecha copiada) | `COUNT` |
| Golden Path DB congelado (DB-GP-001C): «DB SCHEMA CHANGE AFTER FREEZE: REQUIRES NEW WORK ITEM / CONTRACT DECISION», manifest SHA-256 de 5 vistas + 4 SP de sesión/asistencia | `DB_DEVELOP_FREEZE_ADDENDUM.md` |
| El backend invoca `usp_crear_sesion`/`usp_actualizar_sesion` con parámetros **nombrados** | `SesionJpaRepository` |

## 4. Auditoría UTC-D06 — alternativas A+C+E

La recomendación A+C+E de `UTC_D06_HISTORICAL_PROVENANCE_OPTIONS.md` es correcta en dirección: es la única combinación que distingue filas por ruta de escritura y no por fecha, y no reinterpreta horas. B queda bien descartada (auditoría incompleta: PATCH/PUT v1 no auditados, generación auditada como `GRUPO`). Hallazgos que la propuesta definitiva debe cerrar:

| ID | Severidad | Hallazgo | Corrección propuesta |
|---|---|---|---|
| D06-F1 | P1 | A no tiene valor para «el owner clasificó/convirtió esta fila» (C). Con solo `UTC_CONFIRMADO_V2`/`UTC_GENERADO_DB`, una fila clasificada por el owner queda igual que una indeterminada o se etiqueta con un origen falso | Añadir `UTC_CLASIFICADO_OWNER` (solo escribible por el proceso del owner) |
| D06-F2 | P1 | A no protege contra escrituras directas fuera de SP (precedente: semillas 08:00→18:00 sin auditoría). Una fila marcada con su hora alterada a mano conservaría la marca | Regla: cualquier cambio de `fechaHora*` sin marca explícita deja `NULL`; opción de trigger `AFTER UPDATE` a decisión del owner |
| D06-F3 | P1 | La marca debe escribirse en la **misma sentencia** INSERT/UPDATE que las horas; un segundo paso deja ventanas con horas nuevas y marca vieja | Parámetro del SP, no llamada separada |
| D06-F4 | P1 | A exige cambiar objetos congelados (DB-GP-001C): `dbo.Sesion`, `uv_sesion`, `uv_auth_sesion`, `usp_crear_sesion`, `usp_actualizar_sesion`, `usp_generar_sesiones_grupo` y regenerar el manifest | Nuevo work item DB del owner; el backend no lo ejecuta |
| D06-F5 | P2 | C está formulada sobre 3 IDs de la base **local**. La procedencia es por entorno: cada base desplegada necesita su inventario | C como procedimiento por entorno, con las 3 filas locales como primer caso |
| D06-F6 | P2 | «`DetalleAsistencia` necesitaría la misma regla»: sus copias pueden estar desactualizadas respecto de la sesión (D06 no lo menciona) y no hay evidencia de que el backend las exponga por HTTP (`UvDetalleAsistenciaEntity` no mapea `fechaHora*`) | No marcar `DetalleAsistencia`; registrar la obsolescencia de copias como pregunta independiente al owner |
| D06-F7 | P2 | E no fija forma; «excluir filas» rompería cardinalidad de `GET /grupo/{id}` y una futura paginación; un 409 no aplica a listas | E1: marcador por ítem (ver propuesta) |
| D06-F8 | P2 | v1 expone horas de sesión fuera de `/api/v1/sesiones`: `GET /api/v1/estudiante/materias/{materiaId}/sesiones` y `GET /api/v1/grupos/{grupoId}/reportes/asistencia-excel` (celda `toString()` de `LocalDateTime`). La propuesta v2 no los menciona | Quedan v1 sin cambio; documentar como superficie legacy; su v2, si se pide, consume la misma marca |
| D06-F9 | P3 | `UTC_GENERADO_DB` es fiable solo mientras `TIEMPO/ZONA_HORARIA_SQLSERVER` no tenga DST (hoy `SA Pacific Standard Time`, sin DST): en un gap `AT TIME ZONE` desplaza silenciosamente | Nota en el contrato DB; fuera del alcance actual |

## 5. Auditoría UTC-D02 — contrato de errores y perfil de wire

AS-IS v1 comprobado en código:

- `POST /api/v1/sesiones`: `CrearSesionRequestValidator` valida solo `grupo` y `nombre` (→ `VALIDATION_ERROR` + `details`). Las fechas se parsean después en `SesionHttpMapper` con `HttpTemporalParser` → `400 ERR_FECHA_HORA_INVALIDA` **sin** `details`. Una fecha ausente llega `null` al dominio → `ERR_RANGO_FECHAS_SESION_INVALIDO`.
- `PATCH`/`PUT /api/v1/sesiones/{id}`: sin validador HTTP; mismos códigos por mapper/dominio.
- El contrato Golden Path §G admite `ERR_*` de catálogo para validaciones HTTP; `ERR_FECHA_HORA_INVALIDA` es contrato v1 vigente y la guarda `SesionUtcActivationGuardTest` lo congela.

| ID | Severidad | Hallazgo | Evidencia |
|---|---|---|---|
| D02-F1 | P1 | `HttpUtcInstantCodec` usa `ISO_OFFSET_DATE_TIME`, que parsea el offset en modo *lenient*: acepta `+02`, `+02:00:30`, hora sin segundos y 9 decimales. D02 dice `Z` o `±HH:mm` (RFC 3339) | jshell JDK 25 sobre la misma llamada; RED `HttpUtcInstantCodecStrictProfileRedTest` 4 fallos |
| D02-F2 | P1 | El codec lanza `IllegalArgumentException`; `GlobalExceptionHandler` no la maneja → `500 INTERNAL_ERROR` si un mapper v2 llama al codec sin validador previo | handlers declarados; RED HTTP `d02OffsetOutsideTheProfileIsFieldInvalidFormatNotInternalError` |
| D02-F3 | P2 | D02 no dice qué responde v2 con `fin <= inicio` **después** de normalizar (p. ej. `10:00-05:00` → `15:00Z` y `16:00+02:00` → `14:00Z`) | `CrearSesionDomain`/`ActualizarSesionDomain` → `ERR_RANGO_FECHAS_SESION_INVALIDO` |
| D02-F4 | P2 | 9 decimales no caben en `datetime2(7)`: SQL Server redondea y rompe UTC-D04 («mismos bytes») | `sys.columns.scale = 7` |
| D02-F5 | P2 | v1 PATCH no tiene validador HTTP; si v2 reutiliza el patrón, PATCH v2 dejaría pasar fechas inválidas hasta el mapper | `SesionController` |
| D02-F6 | P3 | Divergencia `ERR_FECHA_HORA_INVALIDA` (v1) vs `VALIDATION_ERROR` (v2) | intencional según la propuesta definitiva; v1 no cambia |

Decisión propuesta para contratos: [UTC_D02_ERROR_CONTRACT_PROPOSAL](UTC_D02_ERROR_CONTRACT_PROPOSAL.md). RED añadidos en la rama `jose-valencia/maint-003c-utc-v2-red-hardening`.

## 6. Compatibilidad v1/v2 por superficie

| Superficie | Hoy (v1) | Con marca A desplegada, v2 apagado | Con v2 activo |
|---|---|---|---|
| `POST /api/v1/sesiones` | ISO local sin offset, persiste tal cual | igual; SP deja marca `NULL` por default | igual; marca `NULL` |
| `PATCH`/`PUT /api/v1/sesiones/{id}` | idem | igual; marca → `NULL` | igual; **degrada** una fila v2 a `NULL` (efecto solo visible desde GET v2) |
| `GET /api/v1/sesiones/{id}`, `/grupo/{id}`, `POST /consultas` | `LocalDateTime` sin offset | sin cambio (la entidad JPA no mapea la columna nueva; `ddl-auto: none`, no se valida esquema) | sin cambio |
| `POST /api/v1/sesiones/grupo/{id}/generacion` | SP convierte a UTC | igual; marca `UTC_GENERADO_DB` | igual |
| `GET /api/v1/estudiante/materias/{id}/sesiones` | `LocalDateTime` sin offset | sin cambio | sin cambio (fuera de v2) |
| `GET /api/v1/grupos/{id}/reportes/asistencia-excel` | `LocalDateTime.toString()` | sin cambio | sin cambio (fuera de v2) |
| `POST`/`PATCH /api/v2/sesiones` | no existe (guarda PR #20) | no existe | `Z`/`±HH:mm`, UTC, marca `UTC_CONFIRMADO_V2` |
| `GET /api/v2/sesiones/**` | no existe | no existe | solo tras fijar E; `Z` únicamente con marca confirmada |

**Dictamen:** `D06_DIRECTION_SOUND_WITH_P1_GAPS / D02_NEEDS_PROFILE_AND_ERROR_DECISION / OWNER_DECISION_PENDING`. Propuesta definitiva: [UTC_D06_OWNER_DB_PROPOSAL](UTC_D06_OWNER_DB_PROPOSAL.md).
