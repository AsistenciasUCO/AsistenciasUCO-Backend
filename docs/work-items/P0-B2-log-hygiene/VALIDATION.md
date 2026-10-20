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
- Maven verify: `NOT_RUN` para este cambio de scripts; el baseline vigente del
  backend permanece PASS, pero no certifica el arranque integrado.
- Los `e2e-*.log` de la raíz no se borraron ni movieron; requieren autorización
  explícita y clasificación de regenerabilidad.

