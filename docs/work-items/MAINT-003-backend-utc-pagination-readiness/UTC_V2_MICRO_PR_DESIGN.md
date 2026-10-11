---
status: proposed
type: plan
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---
> **Archivo de evidencia histórica (2026-10-09).** Registra decisiones y pruebas de la fase inicial. Desde entonces los PR #18, #19 y #20 se fusionaron a `develop` el 2026-10-10. La implementación UTC v2 de MAINT-003K sigue en una rama separada, todavía sin integración a `develop`; la DB correspondiente también sigue pendiente de PR y aprobación. Los estados `OPEN`, `NOT_READY`, `NOT_RUN` y las propuestas siguientes describen el momento en que se redactó este documento, no el estado actual de todos los repositorios. Ver `PR21_CONFLICT_RESOLUTION.md`.


# MAINT-003B — diseño del micro-PR UTC v2

**DoR:** `NOT_READY` para implementación. Bloqueado por UTC-D06 ([alternativas](UTC_D06_HISTORICAL_PROVENANCE_OPTIONS.md)). D01–D05 y D07–D09 se aceptan solo como diseño inicial sujeto a pruebas; no están firmados.

## Estado preparado

| Pieza | Rama / SHA | Estado |
|---|---|---|
| Codec `HttpUtcInstantCodec` sin consumidores | PR #20 `jose-valencia/maint-003-backend-utc-pagination-readiness` | GREEN (7 tests) |
| Guarda: v1 rechaza offsets (`ERR_FECHA_HORA_INVALIDA`) y ningún controller mapea `/api/v2/**` | PR #20 `b512bb4` | GREEN 3/0/0/0; control negativo con controller v2 sembrado → falla |
| RED HTTP v2 `SesionV2HttpContractRedTest` (8) | `jose-valencia/maint-003b-utc-v2-red` `e85feff`, **sin PR** | RED: 8 fallos "ningún @RestController mapea /api/v2/sesiones" |
| RED contrato `OpenApiSesionesV2ContractRedTest` (4) | misma rama | 3 RED (falta v2) + 1 GREEN (v1 congelado) |

La rama RED no tiene PR a propósito: los workflows solo corren en PR/push a `develop`/`master`, así que un RED publicado no ensucia CI.

## Alcance cuando D06 se firme

1. **Contrato** (rol contratos): OpenAPI v2 `POST /api/v2/sesiones`, `PATCH /api/v2/sesiones/{sesionId}`, schemas `CrearSesionV2Request`/`ActualizarSesionV2Request` (`format: date-time`, patrón con offset obligatorio), `x-roles: [DOCENTE]`, 201/200 con envelopes vigentes, sin PUT. GET v2 solo después de fijar la forma de E (alternativa D06) y añadir su RED.
2. **Implementación** (rol implementador, sin tocar los RED): DTO request v2 String, validador con `HttpUtcInstantCodec`, controller `/api/v2/sesiones` que reutiliza `CrearSesionInputPort`/`ActualizarSesionInputPort` con `LocalDateTime` UTC y actor de `AuthenticatedUserResolver`. Reglas `SecurityConfig` para `/api/v2/sesiones/**` = DOCENTE. Actualizar la guarda de PR #20 de forma explícita y trazable a la firma D06 (por ejemplo, permitir solo POST/PATCH).
3. **Persistencia de procedencia:** si D06 = A, el owner DB publica la marca y la firma SP; el adapter JPA la envía solo desde v2. Sin esa pieza, POST/PATCH v2 no se activan.

Fuera de alcance siempre: `UPDATE` de horas históricas, `AT TIME ZONE` masivo, DDL desde backend, cambios de v1, horarios recurrentes (`dia` + `LocalTime`, D07).

## Punto abierto detectado al escribir RED

UTC-D02 propone `400 VALIDATION_ERROR` para una fecha naive en v2. El AS-IS v1 responde `400 ERR_FECHA_HORA_INVALIDA` para una fecha mal formada (comprobado por la guarda). El RED v2 sigue la propuesta (`VALIDATION_ERROR` + `details[0].field`); contratos debe confirmar si la divergencia es intencional antes de implementar.

## Pruebas de integración SQL diseñadas (no codificadas)

Codificarlas exige el endpoint y el mecanismo de autenticación de los IT HTTP; se escriben en la fase de implementación. Oráculo sobre el clon `gestionasistenciadb_fase1`, nunca la base fuente:

| ID | Acción | Oráculo SQL | Oráculo HTTP |
|---|---|---|---|
| UTC-IT-01 | POST v2 `16:00+02:00`/`17:00+02:00` (Berlín verano) | `SELECT fechaHoraInicio, fechaHoraFin FROM dbo.Sesion WHERE id=@nueva` = `14:00`/`15:00` | 201 |
| UTC-IT-02 | mismo instante con `Z`, `-05:00`, `+01:00` (Londres verano) | bytes `datetime2` idénticos a UTC-IT-01 | 201 |
| UTC-IT-03 | Berlín overlap `02:30+02:00` y `02:30+01:00` | `00:30` y `01:30`, filas distintas | 201 ×2 |
| UTC-IT-04 | Londres invierno `10:00+00:00` | `10:00` | 201 |
| UTC-IT-05 | PATCH v2 sobre fila v2 | antes/después en SQL; sin doble conversión | 200 |
| UTC-IT-06 | naive / sin offset | ninguna fila nueva (`COUNT` antes = después) | 400 |
| UTC-IT-07 | JWT real ESTUDIANTE/COORDINADOR; DOCENTE no titular | sin fila nueva | 403 / 403 / 403 ownership |
| UTC-IT-08 | v1 tras escrituras v2 | GET v1 sigue devolviendo ISO local sin offset | 200 sin cambio de forma |
| UTC-IT-09 | marca de procedencia (si D06 = A) | v2 escribe marca confirmada; v1 PATCH posterior la deja indeterminada | — |
| UTC-IT-10 | POST v2 `10:00-05:00`/`16:00+02:00` (reloj de pared posterior, instante anterior) | ninguna fila nueva | 400 `ERR_RANGO_FECHAS_SESION_INVALIDO` |
| UTC-IT-11 | GET v2 de fila con marca `NULL` y de fila `UTC_CONFIRMADO_V2` | valores SQL sin cambio | `Z` solo en la confirmada; la `NULL` sin `Z` (forma E1) |

Añadidos en MAINT-003C ([revisión](MAINT-003C_INDEPENDENT_REVIEW.md)); RED unitarios/HTTP del perfil y errores D02 en `jose-valencia/maint-003c-utc-v2-red-hardening`.

Cleanup: solo filas creadas por el IT en el clon, por `id` capturado.

## Gates de salida

`clean verify`, `-Pintegration verify` con 0 skips focales, OpenAPI + conformidad, ArchUnit, JaCoCo 80/70, Sonar, CodeQL, Trivy y JWT real por rol. Rollback: revert del micro-PR; la marca de procedencia (si existe) la revierte el owner DB con su propia migración.
