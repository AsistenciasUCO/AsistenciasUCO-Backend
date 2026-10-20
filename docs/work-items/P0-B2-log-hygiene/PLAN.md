---
status: active
type: work-item
scope: backend-tooling
owner: backend-team
last-reviewed: 2026-10-20
---

# P0-B2 — Higiene de logs locales y arranque reproducible

## Objetivo

Corregir el runner local de Spring Boot y concentrar sus salidas en `logs/`,
que ya está ignorado por Git. Cada ejecución debe rotar los archivos activos,
aplicar retención y límite de tamaño, y dejar el proceso con el mismo código
de salida de Maven. No se modifica el contrato HTTP, el esquema DB, la
configuración productiva ni el contenido de los secretos.

## REQUIREMENT → CONTRACT

- `scripts/run-local.ps1` debe poder ejecutarse desde la raíz del backend.
- La carpeta de logs debe ser única, local e ignorada: `logs/`.
- Los nombres de log no deben contener tokens, contraseñas ni valores de
  configuración secreta.
- La ejecución debe conservar el código de salida del proceso hijo.
- La rotación se realiza al inicio de cada ejecución; la retención se aplica
  por antigüedad, cantidad y tamaño configurables.
- Los archivos históricos `e2e-*.log` de la raíz no se borran en este work
  item; su eliminación requiere autorización explícita del usuario después de
  confirmar que son regenerables.

## AS-IS comprobado

- `scripts/run-local.ps1` apunta a `scripts/backend/run-local.ps1`, ruta que no
  existe en el checkout.
- `.gitignore` ignora `*.log` y `logs/`, pero no existe un runner funcional
  que imponga una ubicación central ni una política de retención.
- Hay logs históricos `e2e-*.log` en la raíz con fecha 2026-10-11; son
  evidencia local y no se clasifican como eliminables automáticamente.

## TARGET / alcance

- Corregir el runner PowerShell y añadir su prueba de comportamiento.
- Añadir el helper de rotación/retención sin tocar código Java.
- Validar la ayuda del runner y, si el entorno lo permite, un arranque y
  reinicio de Spring Boot con inspección de los logs generados.

## Fuera de alcance

- Borrar logs históricos de la raíz, backups, datos persistentes o worktrees.
- Cambiar `pom.xml`, `application*.yml`, OpenAPI, SQL, Keycloak, Docker o
  providers cloud.
- Declarar PASS de arranque integrado si SQL Server/Keycloak no están
  disponibles.

## Rutas autorizadas

- `scripts/run-local.ps1`
- `scripts/lib/local-logging.ps1`
- `scripts/tests/test-local-logging.ps1`
- `docs/work-items/P0-B2-log-hygiene/**`
- `.gitignore` únicamente para la carpeta local de logs si la prueba demuestra
  que el patrón actual no la cubre.

## Evidencia, consumidores y rollback

- Consumidores: desarrolladores que ejecutan Spring Boot localmente y los
  harnesses que leen una ruta de reporte explícita.
- Evidencia: pruebas PowerShell con archivos temporales, `mvnw.cmd` y revisión
  de `logs/` sin valores sensibles.
- Rollback: revertir los commits propios del work item; no se requiere reset,
  merge ni eliminación de artefactos.

## Definition of Ready

`READY_WITH_SCOPED_EXTERNAL_BLOCKERS`: el defecto del runner, los archivos
permitidos, el comportamiento observable y el rollback están definidos. El
arranque real queda condicionado por los servicios externos disponibles.

