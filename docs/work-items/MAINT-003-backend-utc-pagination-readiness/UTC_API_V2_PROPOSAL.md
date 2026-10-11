---
status: proposed
type: contract-decision
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---
> **Archivo de evidencia histórica (2026-10-09).** Registra decisiones y pruebas de la fase inicial. Desde entonces los PR #18, #19 y #20 se fusionaron a `develop` el 2026-10-10. La implementación UTC v2 de MAINT-003K sigue en una rama separada, todavía sin integración a `develop`; la DB correspondiente también sigue pendiente de PR y aprobación. Los estados `OPEN`, `NOT_READY`, `NOT_RUN` y las propuestas siguientes describen el momento en que se redactó este documento, no el estado actual de todos los repositorios. Ver `PR21_CONFLICT_RESOLUTION.md`.


# Propuesta contractual UTC v2 para sesiones

**Estado:** `CONTRACT_DECISION_REQUIRED`. Este documento no publica endpoints ni autoriza implementación, migración, merge o cambio de la API v1.

## Evidencia AS-IS

- `POST /api/v1/sesiones`, `GET /api/v1/sesiones/{sesionId}`, `GET /api/v1/sesiones/grupo/{grupoId}` y `PATCH /api/v1/sesiones/{sesionId}` existen. El `PUT` de actualización continúa como compatibilidad deprecated.
- Requests y responses v1 transportan `fechaHoraInicio`/`fechaHoraFin` sin offset. El adaptador usa `HttpTemporalParser` y el modelo usa `LocalDateTime`.
- `dbo.Sesion.fechaHoraInicio/Fin` son `datetime2`; `usp_crear_sesion` y `usp_actualizar_sesion` reciben `datetime2`, validan el rango y no convierten zona.
- La copia controlada contiene 3 sesiones, sin nulos ni rangos invertidos, entre `2026-08-17` y `2026-10-06`, duración 120–180 minutos y horas almacenadas 18/20. Ninguna coincidió con el horario del grupo ni como reloj local ni aplicando UTC−05:00.
- Dictamen histórico: **`HISTORICAL_TZ_UNDETERMINED`**. No hay un evento externo trazable que permita afirmar si las tres filas son UTC o wall-clock legacy.

## Decisiones propuestas para aprobación

| ID | Decisión propuesta |
|---|---|
| UTC-D01 | Publicar exclusivamente una superficie v2: `POST /api/v2/sesiones`, `GET /api/v2/sesiones/{sesionId}`, `GET /api/v2/sesiones/grupo/{grupoId}` y `PATCH /api/v2/sesiones/{sesionId}`. |
| UTC-D02 | En POST/PATCH, `fechaHoraInicio` y `fechaHoraFin` son obligatorias y aceptan RFC 3339 con offset explícito (`Z` o `±HH:mm`). Una cadena naive produce `400 VALIDATION_ERROR`. PATCH conserva la semántica actual de reemplazo de nombre y rango, no patch parcial. |
| UTC-D03 | El adaptador HTTP convierte `OffsetDateTime` al mismo instante UTC y entrega `LocalDateTime` UTC al puerto/SP. No usa la zona de la JVM, del servidor, de Bogotá ni del navegador como default. |
| UTC-D04 | Todo GET v2 serializa canónicamente UTC con sufijo `Z`. Dos offsets que representan el mismo instante producen la misma salida y los mismos bytes temporales en SQL. |
| UTC-D05 | El schema y los SP permanecen sin cambio: `datetime2` representa UTC por convención únicamente para escrituras v2 nuevas. El backend no administra ni migra el esquema DB. |
| UTC-D06 | v1 conserva exactamente su wire actual mientras se completa el inventario y se migran los consumidores. No se reinterpretan ni reescriben filas históricas; GET v2 de una fila histórica debe bloquearse o marcarse fuera de alcance hasta que el owner funcional resuelva su procedencia. |
| UTC-D07 | Los horarios recurrentes (`día` + `LocalTime`) no son instantes y quedan fuera de esta conversión. Requieren fecha y zona IANA institucional antes de materializar sesiones. |
| UTC-D08 | Roles y ownership no cambian: escritura DOCENTE y validación de titularidad existente; lectura mantiene el matcher/alcance contractual vigente. El actor se obtiene del JWT, nunca del body. |
| UTC-D09 | La compatibilidad `PUT` solo existe en v1. v2 publica PATCH; cualquier fecha de deprecación o retiro de v1 necesita inventario de consumidores y decisión separada. |

## Estado de revisión — 2026-10-09

Ninguna decisión queda aprobada automáticamente.

| ID | Estado |
|---|---|
| UTC-D01..D05, UTC-D07..D09 | `ACCEPTED_AS_INITIAL_DESIGN_SUBJECT_TO_TESTS`: base del RED en `jose-valencia/maint-003b-utc-v2-red`; no es firma contractual ni autoriza implementación |
| UTC-D06 | `CONTRACT_DECISION_REQUIRED`: la redacción actual no dice cómo distinguir una fila histórica de una UTC fiable; con v1 escribiendo, ninguna fecha de corte lo resuelve. Alternativas y preguntas al owner en [UTC_D06_HISTORICAL_PROVENANCE_OPTIONS](UTC_D06_HISTORICAL_PROVENANCE_OPTIONS.md) |
| UTC-D02 (detalle) | abierto: v1 AS-IS responde `ERR_FECHA_HORA_INVALIDA`, la propuesta v2 dice `VALIDATION_ERROR`; confirmar si la divergencia es intencional |
| MAINT-003C (2026-10-09) | revisión independiente: D06 → propuesta definitiva A+C+E1 para el owner DB ([UTC_D06_OWNER_DB_PROPOSAL](UTC_D06_OWNER_DB_PROPOSAL.md)); D02 → perfil estricto y tabla de errores ([UTC_D02_ERROR_CONTRACT_PROPOSAL](UTC_D02_ERROR_CONTRACT_PROPOSAL.md)). Ambas `CONTRACT_DECISION_REQUIRED`; sin firma no se implementa |

Diseño del micro-PR y pruebas preparadas: [UTC_V2_MICRO_PR_DESIGN](UTC_V2_MICRO_PR_DESIGN.md). GET v2 no se activa y v1 no cambia; la guarda `SesionUtcActivationGuardTest` (PR #20) lo hace verificable.

## Forma propuesta

Request POST v2:

```json
{
  "grupo": "11111111-2222-3333-4444-555555555555",
  "nombre": "Clase de prueba",
  "fechaHoraInicio": "2026-07-15T16:00:00+02:00",
  "fechaHoraFin": "2026-07-15T17:00:00+02:00"
}
```

Persistencia esperada: `2026-07-15 14:00:00` y `2026-07-15 15:00:00`. Respuesta GET v2:

```json
{
  "fechaHoraInicio": "2026-07-15T14:00:00Z",
  "fechaHoraFin": "2026-07-15T15:00:00Z"
}
```

Los demás campos y envelopes deben conservar la forma de v1 salvo decisión contractual explícita. POST mantiene inicialmente `201` y el envelope de mensaje vigente; GET por id y por grupo conservan sus envelopes actuales. No se propone inventar un identificador en la respuesta del SP.

## Regla DST

El offset explícito hace inequívoco el instante. En el solapamiento de Berlín, `2026-10-25T02:30:00+02:00` y `2026-10-25T02:30:00+01:00` son instantes diferentes y deben persistirse distintos. Un gap de una zona IANA no puede detectarse a partir de `OffsetDateTime` solamente; el frontend que parte de fecha/hora civil debe resolver el `ZoneId` del navegador, rechazar gaps y exigir desambiguación en overlaps antes de enviar el offset.

## Gates posteriores a la aprobación

1. Versionar OpenAPI v2 y contract tests RED antes de producción.
2. Implementar DTO/controller/mapper v2 usando el codec de PR #20, sin conectar el codec a v1.
3. Probar POST→SQL→GET y PATCH→SQL→GET con `Z`, Bogotá, Berlín y Londres verano/invierno, overlap con ambos offsets, naive inválido y gap resuelto/rechazado por el consumidor.
4. Ejecutar JWT real para rol permitido, `401`, `403` y ownership; `-Pintegration verify` con cero skips focales.
5. Revalidar v1 sin cambio, OpenAPI, ArchUnit, JaCoCo, Sonar, CodeQL y Trivy.
6. Publicar rollback por revert del micro-PR v2. No realizar UPDATE histórico ni DDL.

## Firma requerida

La implementación solo puede iniciar tras aprobar explícitamente `UTC-D01` a `UTC-D09` y registrar la decisión del owner DB/funcional sobre el tratamiento de filas `HISTORICAL_TZ_UNDETERMINED`. Si se rechaza cualquier punto, se actualiza esta propuesta antes de escribir código.
