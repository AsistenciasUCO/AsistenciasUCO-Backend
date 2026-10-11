---
status: DECIDED_PENDING_OWNER_DB_APPROVAL
work_item: MAINT-003H / CC-003G-01
type: contract-decision
date: 2026-10-10
consumers: UsuarioJpaRepository, PlanEstudioJpaRepository
db_work_item: CC-003G-01-public-user-plan-providers (repo gestion-asistencia-db, rama feat/cc-003g-01-public-user-plan-providers)
---

# CC-003G-01 — Decision de contrato de los proveedores publicos

Resuelve la `CONTRACT_CONFLICT` registrada en la auditoria independiente
([INDEPENDENT_REVIEW](INDEPENDENT_REVIEW.md)): el backend invoca `dbo.usp_sincronizar_usuario` y
`dbo.usp_registrar_o_actualizar_plan_estudio`, que **no existian** en la DB publicada
(`feat/utc-d06-post-freeze@3842f70`). La decision elegida es la opcion preferida de la auditoria: **implementar los
proveedores en un work item DB separado de D06**, sin SP simulados ni alias a `usp_sincronizar_usuario_interno`.

## Evidencia usada (AS-IS comprobado)

- Los dos archivos SQL existieron en `d6aa0b5` y fueron eliminados en `daeac11` (historial del repo DB); la version
  eliminada de plan recibia `@codigo`/`@nombre`, que el backend ya no envia, y no tenia autorizacion.
- El estado «TD-043 CLOSED» del backend afirmaba que los providers existian «en la DB final»; contra el arbol DB publicado
  y contra el contenedor aislado desplegado desde el, eso era falso (`SqlStoredProcedureContractIT` fallaba en ambos).
- `POST /api/v1/usuarios` es `authenticated()` ([SecurityConfig](../../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/config/security/SecurityConfig.java))
  y `ProvisionarUsuarioUseCaseImpl` crea la cuenta de identidad despues del alta en DB (un reintento tras fallo parcial es
  esperable).
- `GestionarPlanEstudioUseCaseImpl` resuelve el programa del coordinador (`InstitutionalScopePort`) pero el adapter no
  enviaba el ejecutor al SP: **hallazgo P1 de autorizacion**.

## Contrato adoptado (detalle y matriz de codigos en el repo DB: `docs/work-items/CC-003G-01-public-user-plan-providers/CONTRACT.md`)

| Proveedor | Cambio de firma | Semantica | Autorizacion |
|---|---|---|---|
| `usp_sincronizar_usuario` | ninguna (9 params del backend) | Alta idempotente: reintento exacto = exito sin escribir; documento/correo en uso con otros datos = rechazo; nunca actualiza ni toca el password | Endpoint AUTENTICADO; en DB solo `EXECUTE` del rol runtime |
| `usp_registrar_o_actualizar_plan_estudio` | **+ `@idUsuarioEjecutor`** (5.º param, obligatorio fail-closed) | UPSERT por id; unicidad (programa, inp); plan de otro programa = `SEC_002`; reintento = exito sin escribir | COORDINADOR activo y titular del programa, revalidado en DB |

## Cambios backend derivados (este PR)

- `PlanEstudioCommandPort.registrarOActualizarPlanEstudio(idPlanEstudio, idPrograma, inp, idUsuarioEjecutor)`; el use case
  propaga el coordinador autenticado (`PlanEstudioDomain.usuario()`); `PlanEstudioJpaRepository` envia
  `@idUsuarioEjecutor` y falla antes de la DB si falta.
- `DbFailureClassifier`: `GEN_003` (plan duplicado) -> `CONFLICT` (antes caia en `ERR_DB_UNCLASSIFIED`, 5xx). Ningun otro SP emite `GEN_003`.
- Tests: `SqlStoredProcedureContractIT` actualiza la firma del plan (contrato nuevo aprobado en esta decision, no una
  relajacion); `GestionarPlanEstudioUseCaseImplTest`, `PlanEstudioJpaCommandPatternTest`, `PlanEstudioJdbcBaseline` y
  `AcademicUserCommandsSpParityIT` se adaptan a la nueva firma conservando todas sus aserciones previas; se **agregan**
  `PLA_PAT_004`, una prueba de propagacion del ejecutor, el caso `GEN_003` del traductor y
  `UsuarioPlanEstudioProvidersSqlServerIT` (5 escenarios reales por puertos). No se elimino ni deshabilito ninguna prueba.

## Compatibilidad y rollback

- HTTP/OpenAPI: sin cambios (el ejecutor es interno al backend, ya autenticado). Usuarios v1 y UTC v2: sin cambios.
- Orden de despliegue: **DB primero** (el SP acepta `@idUsuarioEjecutor` y lo exige), luego backend. Un backend nuevo contra
  una DB sin el proveedor falla en `SqlStoredProcedureContractIT`/runtime con `Could not find stored procedure`; un backend
  viejo contra la DB nueva recibe `GEN_002` (ejecutor ausente) en planes: rechazo seguro, nunca escritura sin autorizacion.
- Rollback: revertir el PR backend y dejar el SP DB (rechaza sin ejecutor) o eliminar los dos SP y el script de permisos.

## Decision pendiente de humanos

Aprobacion del owner DB (CONTRACT.md) y de seguridad antes de desplegar; no se desplego nada a produccion ni se hizo merge.
