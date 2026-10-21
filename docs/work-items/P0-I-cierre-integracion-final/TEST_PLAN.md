# TEST_PLAN — P0-I

| Caso | Nivel | Estado |
|---|---|---|
| 1 clic / doble clic / 5 clics emiten 1 POST | Karma `student-courses.component.spec` (FE-05, P0-DUP) | PASS |
| Reintento tras timeout reutiliza correlación y refresca sesiones | Karma | PASS |
| Contenido distinto o éxito previo → correlación nueva; error libera el bloqueo | Karma + `radicacion-intento.util.spec` | PASS |
| `crearReclamo` envía `X-Correlation-Id` solo si hay `intentoId` | Karma `attendance-claim.service.spec` | PASS |
| Fecha civil cerca de medianoche (America/Bogota) | Karma | PASS |
| Respuesta perdida / concurrencia 5x misma correlación / otra correlación / sin soporte / dos reclamos legítimos / rollback / MinIO sin huérfanos | E2E `support_binding_e2e.py idempotency` | NOT_RUN: identidades ESTUDIANTE1/2 desalineadas en Keycloak local (`invalid_grant`) |
| 15 checks de sesiones con fixture aislado | E2E `student_sessions_e2e.py` | fixture SQL PASS (aplica/idempotente/limpia, conteos restaurados); HTTP NOT_RUN por la misma causa |
| `SecurityErrorResponseWriter` JSON seguro | JUnit | PASS |
