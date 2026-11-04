---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-03
---

# PLAN — SES-04: resolución de reclamos → JUSTIFICADA (Backend)

Rama `fix/ses04-reclamo-justificada` desde `develop@0d4db159`. Origen: [PLAN-CORRECCIONES-POR-ROL](../SEC-BASELINE-019-upgrade-performance/PLAN-CORRECCIONES-POR-ROL.md), decisión 2 y SES-04.

## AS-IS y causa
`PATCH /api/v1/docente/reclamos/{id}` → `ResolverSolicitudRevisionAsistenciaUseCaseImpl` → `usp_resolver_solicitud_revision_asistencia`.
El dominio aceptaba cualquier `accion` no vacía (el SP trataba todo lo distinto de APROBADA como rechazo) y la DB
aprobaba con `asistio = 1`. La lectura del estudiante ya deriva `JUSTIFICADA` de la causal `EX`
(`ConsultarSesionesMateriaEstudianteUseCaseImpl`), así que la corrección de la DB basta para que la vean estudiante y docente.

## Contrato
| Caso | HTTP / código |
|---|---|
| `accion` fuera de `APROBADA`/`RECHAZADA` (dominio o DB `REV_001`) | 400 `ERR_ACCION_REVISION_INVALIDA` |
| Solicitud inexistente (`REV_005`) | 404 `ERR_RECLAMO_NO_EXISTE` |
| Docente no titular (`VAL_007`) | 403 (sin cambio) |
| Ya resuelta con otra decisión (`REV_002`) | 409 `ERR_RECLAMO_YA_RESUELTO` |
| Presencia no justificable (`REV_003`) / sin detalle (`REV_004`) | 409 `ERR_RECLAMO_ASISTENCIA_NO_JUSTIFICABLE` / `ERR_RECLAMO_SIN_DETALLE_ASISTENCIA` |
| Reintento con la misma decisión | 200 sin segunda rectificación |

El éxito solo se responde si el SP confirma (`JpaProcedureExecutor` traduce `estadoResultado = 0` a excepción). El caso
de uso no publica eventos. La matriz HTTP se actualizó; el endpoint no forma parte del OpenAPI golden path.

## Archivos
`ResolverSolicitudRevisionAsistenciaDomain`, `AsistenciaErrorCode`, `DbFailureClassifier`, `DbExceptionTranslator`,
`docs/contracts/HTTP_AS_IS_MATRIX.md`. Tests: `ResolverSolicitudRevisionAsistenciaDomainTest`,
`ResolverSolicitudRevisionAsistenciaUseCaseImplTest` (nuevo), `DbFailureClassifierReclamoRevisionTest` (nuevo),
`ResolverSolicitudRevisionAsistenciaRepositoryMapperTest`.

## Validación
RED `bf47a40b` (compilación) → fix `763c7d8a`: focalizadas 182/182; `./mvnw.cmd -o verify` **3069/3069**, ArchUnit, JaCoCo OK.
**NOT_RUN:** `-Pintegration` y HTTP real (requieren la DB de SES-04 desplegada por el mecanismo oficial), navegador.
