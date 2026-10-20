# VALIDATION — LOCAL-STABILIZATION-LB004D-2026-10-20

Estado: `IN_PROGRESS`; cierre funcional aún bloqueado por el conflicto contractual frontend y por la validación remota pendiente.

Este archivo se completa con comandos, fecha/hora local, exit code, alcance, cobertura, skips y dictamen. No almacenar tokens, contraseñas, payloads de archivos ni logs crudos.

## Resumen

| Área | Estado | Evidencia | Limitación |
|---|---|---|---|
| Git / refs | `PASS` parcial | Fetch/read-only completado; worktrees auxiliares: backend mergeado con `origin/develop`, DB mergeado con `origin/develop`; PR heads originales preservados | Falta push/revisión remota de los commits auxiliares |
| Fixture DB CI | `PASS` local | Fixture idempotente namespaced; test dirigido: 17/17; quality gate: Expected 245, Executed 248, Passed 247, Failed 0, Skipped 1 permitido, CriticalMissing 0, SQLCMD 0, SQL errors 0, unauthorized skips 0; cleanup PASS | La ejecución fue sobre SQL Server Developer local; CI remoto pendiente |
| Backend hygiene / verify | `PASS` | Java 25; `mvnw test` exit 0: 1651 tests, 0 failures/errors/skips; `mvnw verify` exit 0; JaCoCo LINE 91.05%, BRANCH 79.98%; `PYC_TRACKED=0` | Se observaron WARN/logs de tests, sin gate rojo |
| Frontend build | `PASS` | `npm ci` exit 0; `npm run build -- --configuration=production` exit 0 | No implica PASS de Karma/E2E |
| Frontend verify | `FAIL / BLOCKED` | `npm run verify` exit 1 por specs tipados preexistentes y `ChromeHeadlessCI` sin load; no se modificaron tests | Requiere decisión del owner de contratos/tests y navegador CI/local |
| Docker/providers | `PASS` parcial / `REVIEW_REQUIRED` | Keycloak readiness/realm 200; MinIO primario healthy loopback; ClamAV healthy; EICAR `FOUND`; archivo benigno `OK`; SQL running; legacy MinIO detenido sin borrar volumen | E2E backend real aún no certificado; legacy ClamAV histórico exit 137 |
| Angular E2E real | `NOT_RUN` | No se declara PASS sin browser, login, fixture contractual y binding DB docente | `REVIEW_BINDING` sigue `BLOCKED_BY_DB_CONTRACT` |

## Comandos

### 2026-10-20 — comandos ejecutados

- `git fetch --all --prune` en los tres repositorios: refs remotas actualizadas; no hubo merge remoto ni force-push.
- `.\mvnw.cmd test` y `.\mvnw.cmd verify` en `be-stab`, Java 25: exit 0.
- Fixture + `test_lb004d_support_binding.sql` en `sql_server_asistencias`: exit 0; cleanup exit 0.
- Fixture + `test_summary.ps1` en `sql_server_asistencias`: exit 0; contadores registrados arriba; cleanup exit 0.
- `npm ci --no-audit --no-fund` en `fe-stab`: exit 0.
- `npm run verify` en `fe-stab`: exit 1; fallo reproducido y no convertido en PASS.
- `npm run build -- --configuration=production` en `fe-stab`: exit 0.
- Docker health/readiness y probes ClamAV: resultados registrados arriba.

No se guardaron credenciales, tokens ni logs crudos.

## Gate y dictamen

No cerrar mientras haya un gate aplicable `NOT_RUN`, `FAIL`, `REVIEW_REQUIRED` o mientras el contrato DB de binding permanezca bloqueado para el E2E docente. El cierre funcional requiere además `CLOSURE.md`, deuda y ledger manual conforme a la DoD única.
