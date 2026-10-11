# PR (Draft) — MAINT-003H: backend de CC-003G-01 (ejecutor del plan de estudio + evidencia)

**Base sugerida:** `jose-valencia/maint-003f-utc-v2-implementation` (apilado sobre `e45c36a`). **Head:** `jose-valencia/maint-003h-provider-contract-impl`.
**No hacer merge ni desplegar** hasta: (1) aprobacion del owner DB del PR `feat/cc-003g-01-public-user-plan-providers`, (2) checks remotos
(Backend Quality Gate / SonarCloud / CodeQL) en verde sobre el SHA exacto, (3) aprobacion de seguridad.

## Que cambia
- `PlanEstudioCommandPort` / `PlanEstudioJpaRepository` / `GestionarPlanEstudioUseCaseImpl`: el coordinador autenticado viaja como
  `@idUsuarioEjecutor` a `usp_registrar_o_actualizar_plan_estudio` (hallazgo P1); falla antes de la DB si falta.
- `DbFailureClassifier`: `GEN_003` -> `CONFLICT` (plan duplicado ya no cae en error tecnico no clasificado).
- Contrato `SqlStoredProcedureContractIT` del plan actualizado a la firma aprobada (5.º parametro); el resto de expectativas intactas.
- Pruebas nuevas: `PLA_PAT_004`, propagacion del ejecutor, caso `GEN_003`, `UsuarioPlanEstudioProvidersSqlServerIT` (5 escenarios reales).
- Docs: decision de contrato, los 3 failures y 3 skips uno por uno, validacion con SHA exactos, TD-043 reabierta con la correccion de su cierre
  indebido, TD-044 con el fixture.

## Evidencia (ver VALIDATION.md y FAILURES_AND_SKIPS.md)
Java 25: Surefire 1499/0/0/0; Failsafe 196 tests, 0 failures, 0 errors, 0 skips (original 191/3/0/3); JaCoCo 91,31 % / 78,87 %; gate SQL 219/218/0/1;
regresion UTC v2 HTTP real con JWT de Keycloak y login SQL de minimo privilegio 20/20 (21/21 con JVM en UTC).

## Riesgos
Orden de despliegue: DB primero. Un backend viejo contra la DB nueva recibe `GEN_002` en planes (rechazo seguro). Sin cambios HTTP/OpenAPI.
Observacion fuera de alcance: la lectura v1 de sesiones depende de la zona horaria de la JVM (+5 h con zona Bogota; exacta en UTC; identica en baseline) — ver VALIDATION.md.
