# TEST PLAN — P0-B: Unificación y sincronización segura

## Requisito → comportamiento observable

| ID | Requisito | Escenario | Observable esperado |
|---|---|---|---|
| P0B-GIT-001 | Validar repositorio y `origin` | Ruta inexistente, carpeta no Git o URL distinta | Estado explícito; no fetch ni escritura de ramas. |
| P0B-GIT-002 | Consultar sin mutar | `-Status` con rama detrás | SHA, ahead/behind y merge-base; rama sin cambios. |
| P0B-GIT-003 | Actualizar seguro | Rama de integración limpia y remote ahead | Fast-forward; SHA anterior/nuevo; exit 0. |
| P0B-GIT-004 | Preservar feature | Feature activa limpia y `develop` inactiva | Se actualiza solo la línea base; feature sigue activa y no recibe merge. |
| P0B-GIT-005 | No sobrescribir trabajo | Archivo modificado y archivo untracked | Solo fetch; SHA y archivos permanecen; exit no cero. |
| P0B-GIT-006 | Detener divergencia | Local y remote con commits exclusivos | `DIVERGED`; no merge ni reset. |
| P0B-GIT-007 | Continuar componentes | Un repositorio falla fetch o falta | Se reporta el componente y se procesan los demás. |
| P0B-GIT-008 | Idempotencia | Repetir `-Apply` tras un fast-forward | Segundo run `UP_TO_DATE`, sin nuevo cambio. |

## RED_SNAPSHOT

El sincronizador es tooling PowerShell y no modifica el comportamiento del backend; no aplica un RED Java. El arnés se diseñó antes de validar la implementación y falla de forma causal si falta el script. No se presenta esa ausencia retrospectiva como una ejecución RED productiva.

## Casos ejecutados

`scripts/tests/test-sync-workspaces.ps1` crea repositorios bare temporales, usa `origin` local y elimina exclusivamente su directorio temporal al finalizar. Cubre: actualizado, fast-forward, repetición idempotente, feature activa, worktree dirty, untracked, divergencia, remoto incorrecto, repositorio faltante y error de fetch.

## Gates separados

- Backend: tests de ownership, ArchUnit, cobertura y `verify` en la rama nueva.
- Frontend: `npm run verify`, incluyendo build, Karma/ChromeHeadless, cobertura global y el gate específico de realtime; no sustituir por lectura de `VALIDATION.md`.
- DB: checks SQL y stored procedures solo contra ambiente controlado; no ejecutar migraciones desde este work item.
- Infra: comprobar `main`/`develop` y la rama de integración sin modificar el checkout activo.

## Evidencia adicional ejecutada

- Sincronizador real: `-Status` y `-Apply` sobre Backend, Frontend, DB e Infra; `-Apply` exit 0, con dirty worktrees preservados y sin escritura destructiva.
- Ownership de aplicación: `GestionarAsignaturaUseCaseImplTest` + `AsignaturaJpaCommandPatternTest`, 15/15.
- Frontend: `npm run verify`, 341/341 tests, cobertura realtime LINE 95.38% y BRANCH 83.95%, build de producción PASS.
- SQL Server: `NOT_RUN` por precondición de ambiente no satisfecha; el bloqueo no se convierte en PASS por inspección estática.
