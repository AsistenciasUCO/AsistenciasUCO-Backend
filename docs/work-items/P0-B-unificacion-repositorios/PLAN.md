# PLAN — P0-B: Unificación de repositorios y sincronización segura

## Identidad y objetivo

- Fecha / rol: 2026-10-18 / mantenimiento de workspace, auditoría y backend.
- Base Git backend oficial: `origin/develop` en `253c3c95e4308f105d1357c023db7b7f48578929`.
- Objetivo: dejar `origin` apuntando a los repositorios de `AsistenciasUCO`, conservar los remotos históricos, proteger el estado local y versionar un sincronizador PowerShell no destructivo.
- Criterios de aceptación: remotos comprobados por URL y ascendencia; cambios locales preservados; `-Status` y `-Apply` idempotentes; fast-forward solamente cuando sea seguro; feature branches sin merge automático; matriz de auditoría; pruebas reproducibles; PR hacia el repositorio oficial sin merge.
- Fuentes: `AGENTS.md`, `SOURCE_OF_TRUTH.md`, `DEFINITION_OF_READY.md`, `DEFINITION_OF_DONE.md`, `VALIDATION_RUNBOOK.md`, `TESTING_STANDARD.md`, protocolo de alineación contractual y el work item hijo `P0-SEC-001-coordinador-asignatura-ownership`.

## AS-IS y evidencia

| Ámbito | Estado inicial comprobado | Límite |
|---|---|---|
| Backend | `origin=https://github.com/alejovh12/AsistenciasUCO.git`; worktree `codex/p0-sec-001-coordinator-ownership`; `d0f170b8`; cambios modificados y sin seguimiento; PR personal #25 abierto/draft, base `72312d7c`. | El checkout compartía worktrees; no se podía hacer checkout destructivo. |
| Frontend | `origin=https://github.com/shesho22/AsistenciasUCO-Frontend.git`; feature limpia `b4368d1e`; `develop` personal estaba detrás. | Copia fuera del workspace escribible; se requirió operación Git elevada. |
| DB | `origin=https://github.com/johnjduque/gestion-asistencia-db.git`; `develop` limpio `f2871a9`; 15 commits detrás del remoto personal; worktrees y stashes presentes. | El esquema/SQL pertenece al owner DB; esta tarea no edita migraciones. |
| Infra | Se localizó y clonó `AsistenciasUCO/AsistenciasUCO-Infra` en `C:\Users\josev\AsistenciasUCO\AsistenciasUCO-Infra` sin sobrescribir una ruta existente. `main` y `develop` existen; `develop` está 9 commits por delante de `main`. | `develop` queda como integración; el checkout activo se conserva en `main` y no se actualiza automáticamente. |
| Oficial backend | `AsistenciasUCO/AsistenciasUCO-Backend` avanzó cinco commits después de `72312d7c`, hasta `253c3c95`; el PR personal no es transferible entre repositorios. | La equivalencia funcional se verificó por diff; el PR personal se conserva. |

## Continuación autorizada y corte real

La decisión de organización del 2026-10-18 autoriza continuar el cierre en repositorios oficiales. El sincronizador se ejecutó contra las cuatro rutas reales con `-Status` y `-Apply`, usando `develop` como rama de integración de Infra. El último `-Apply` terminó con exit code 0: Backend y Frontend quedaron `UP_TO_DATE_DIRTY` y conservaron sus cambios; DB e Infra quedaron `UP_TO_DATE`. No hubo checkout, merge, reset, stash, push ni eliminación de ramas.

El cierre funcional sigue condicionado por dos bloqueos externos comprobados: SQL Server local no escucha en `localhost:1433` y la revisión del PR oficial mantiene fallos de configuración de Sonar/Dependency Review. P0 no se declara DONE por esos bloqueos.

## TARGET

Cada checkout local existente usa `origin` oficial; el remoto anterior queda como `personal`, `legacy-frontend` o `legacy-db`. La rama de integración se actualiza solo por fast-forward cuando el worktree está limpio y la rama no diverge. El operador conserva su rama activa y recibe un reporte si debe reconciliarla.

## Clase de cambio y alcance de rutas

- Change class: tooling operacional de repositorio + evidencia transversal + corrección frontend acotada para cerrar tests pendientes; no cambia contratos backend, SQL, MinIO, Redis ni reclamos.
- PRIMARY_VARIABLE: `GIT_CANONICAL_REMOTE_CONFIGURED`.
- ALLOWED_PATHS: `scripts/sync-workspaces.ps1`, `scripts/sync-workspaces.config.example.json`, `scripts/tests/test-sync-workspaces.ps1`, `docs/work-items/P0-B-unificacion-repositorios/**`, actualización del índice `docs/work-items/README.md`; en Frontend, únicamente los tres archivos del PR oficial `AsistenciasUCO/AsistenciasUCO-Frontend#1`.
- FORBIDDEN_PATHS: migraciones SQL, `schema/`, `.env*`, secretos, MinIO, Redis, reclamos, merges, force-push, reset destructivo, cambios de contrato HTTP/OpenAPI y código productivo no cubierto por `P0-SEC-001`.
- CONTRACTS: no se cambia contrato HTTP, dominio, persistencia ni realtime; se referencia el contrato existente para auditar consumidores.
- PROVIDERS: Git remoto oficial, ramas de integración, worktrees locales y script PowerShell.
- EXTERNAL_ENVIRONMENT: GitHub oficial/legacy, Frontend oficial, DB oficial e Infra oficial; cada validación se reporta con salida real o `NOT_RUN`.
- SECURITY_IMPACT: no registrar credenciales; no enviar datos de worktree a GitHub; preservar `origin` personal como referencia histórica.
- OBSERVABILITY_IMPACT: el script imprime SHA, estado y razones, nunca contenidos de archivos o secretos.
- TEST_LEVEL_REQUIRED: pruebas PowerShell con repositorios Git temporales; backend unit/ArchUnit/verify para la corrección P0-SEC-001; integraciones externas separadas como `NOT_RUN` cuando no haya fixture.
- ROLLBACK: restaurar remotos desde la configuración registrada; recuperar trabajo desde `refs/backup/p0b-pre-canonical-20261018` y `stash@{0}`; revertir solo commits propios. No borrar ramas ni stashes.
- CONSUMERS: desarrolladores de los cuatro repositorios, CI/PR oficial y operadores locales.
- STOP_CONDITIONS: remoto incorrecto, worktree dirty, divergencia, conflicto contractual, Infra ausente, secreto expuesto o cualquier operación que requiera descartar cambios.

## Alcance

- Inspeccionar y proteger estado Git antes de cambiar remotos.
- Configurar remotos oficiales en los checkouts existentes y conservar los históricos.
- Publicar un sincronizador seguro con `-Status` y `-Apply`.
- Auditar por diff los commits oficiales posteriores a cada snapshot.
- Integrar la corrección de ownership existente de `P0-SEC-001` sobre `origin/develop` en una rama nueva.
- Ejecutar los gates disponibles y separar `PASS`, `FAIL`, `SKIP`, `NOT_RUN` y `BLOCKED`.
- Ejecutar `npm run verify` en Frontend y publicar el PR oficial separado cuando los tests confirmen una corrección real.
- Registrar el riesgo de stored procedures de ownership en `DB_OWNER_PROPOSAL.md` sin modificar SQL.

## No alcance

- No cerrar ni eliminar el PR personal #25.
- No cambiar SQL ni administrar el esquema DB.
- No implementar MinIO, Redis, reclamos, PEP mayor ni nuevas funcionalidades.
- No actualizar una feature branch con merge automático.
- No declarar E2E, SQL Server, ChromeHeadless, Docker o CI remoto como PASS sin ejecución verificable.

## Archivos afectados

- EXISTENTES: `docs/work-items/README.md`.
- NUEVOS: `scripts/sync-workspaces.ps1`, `scripts/sync-workspaces.config.example.json`, `scripts/tests/test-sync-workspaces.ps1` y los informes de esta carpeta.
- RETIRAR: ninguno; los documentos P0 antiguos no se copian porque describían remotos personales como estado vigente.

## Contratos y consumidores afectados

No hay cambio de contrato público. La auditoría contrasta backend oficial, frontend oficial y DB oficial por sus snapshots. `P0-SEC-001` conserva rutas/payloads HTTP y añade una defensa Application; la defensa DB de update/toggle sigue siendo responsabilidad del owner DB.

## Riesgos y dependencias

- SQL Server local y Docker no están disponibles para una prueba segura en este entorno.
- Sonar y Dependency Review del PR oficial requieren configuración de repositorio/credenciales externas; no se resuelven cambiando código ni deshabilitando checks.
- Frontend y DB viven fuera del workspace y requieren permisos de operación local.
- DB tiene worktrees/stashes que deben conservarse.
- La rama activa del PR #25 queda basada en la línea personal; la rama nueva oficial necesita revisión y reconciliación posterior.
- La validación real SQL Server, frontend y CI remota no es sustituible por mocks ni por el script Git.

## Test plan

Ver `TEST_PLAN.md`. El sincronizador se prueba como herramienta externa con fixtures Git locales; el cambio de ownership conserva su RED/GREEN en `P0-SEC-001`.

## Definition of Ready

`READY_WITH_SCOPED_EXTERNAL_BLOCKERS` para tooling y reconciliación local: objetivo, rutas, snapshots, consumidores y rollback están definidos. `NOT_READY` para declarar P0 funcionalmente cerrado mientras SQL Server no esté disponible y los checks externos del PR no estén verdes/revisados.
