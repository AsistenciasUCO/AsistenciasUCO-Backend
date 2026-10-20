# VALIDATION — P0-B2 log hygiene

Fecha de inicio: 2026-10-20 (America/Bogota).

## Evidencia ejecutada

| Comprobación | Resultado | Evidencia |
|---|---|---|
| Harness de logging | `PASS` | `powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File .\scripts\tests\test-local-logging.ps1`; exit code 0; `passed=5 failed=0`. Probó creación, rotación, retención, aislamiento de archivos y propagación del exit code 7 del proceso hijo. |
| Ayuda del runner | `PASS` | `powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File .\scripts\run-local.ps1 -Help`; exit code 0. No cargó `.env` ni arrancó servicios. |
| Diff whitespace | `PASS` | `git diff --check`; sin errores de whitespace en los archivos del cambio. |

## Pendiente / limitaciones

- Spring Boot start/restart: `NOT_RUN`; requiere decidir un entorno controlado
  con SQL Server/Keycloak y no se ejecuta contra datos persistentes por defecto.
- Maven verify: `BLOCKED_BY_TOOLING` en la repetición de este corte. Compiló
  con Java 25, pero Surefire no produjo resumen final y dejó un dumpstream por
  error de canal de attach de la JVM; el proceso se interrumpió. El baseline
  previo del backend permanece `PASS` (1581/1581 en worktree limpio), pero no
  se usa esta repetición fallida como PASS.
- Los `e2e-*.log` de la raíz no se borraron ni movieron; requieren autorización
  explícita y clasificación de regenerabilidad.

