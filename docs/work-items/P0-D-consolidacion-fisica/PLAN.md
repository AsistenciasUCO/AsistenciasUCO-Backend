---
status: active
type: work-item
scope: cross-repository-workspace
owner: backend-team
last-reviewed: 2026-10-20
---

# P0-D — Consolidación física y retiro reversible de copias

## Objetivo

Consolidar las copias locales verificables de Backend, Frontend, DB e Infra,
preservar commits, cambios sin commit, stashes y respaldos, y retirar del
workspace únicamente worktrees limpios o clones independientes verificados.
El retiro físico será reversible mediante Git worktree metadata o una
cuarentena identificada. El borrado definitivo queda fuera de este work item y
requiere aprobación explícita de las rutas exactas.

## Clase y variable principal

- Change class: `INFRASTRUCTURE` (operación local de workspace, sin cambio de runtime).
- `PRIMARY_VARIABLE`: `LOCAL_COPY_RECOVERABILITY`.

## Requisito y autoridad

- Requisito: ejecución P0-D del usuario, texto pegado el 2026-10-20.
- Autoridad de arquitectura y precedencia: `AGENTS.md` y
  `docs/governance/SOURCE_OF_TRUTH.md`.
- Antecedentes: P0-B2, P0-C y `P0-estabilizacion-multirrepositorio`.
- Los remotos oficiales y la autoridad contractual Frontend/DB no se infieren
  de un nombre de carpeta; se registran AS-IS y cualquier decisión pendiente.

## AS-IS comprobado

- Backend canónico: `C:\Users\josev\AsistenciasUCO\AsistenciasUCO`.
- Frontend: existen worktrees de
  `AsistenciasUCO-Frontend-p0b` y la copia independiente
  `frontend-lb004d-working`; la autoridad entre `origin` fork y `upstream`
  oficial sigue siendo una decisión de owner.
- DB: `gestion-asistencia-db` en el workspace y la ruta documentada en OneDrive
  comparten la autoridad externa pendiente; SQL y backups no se modifican.
- Infra: `AsistenciasUCO-Infra` es un repositorio separado; no se mezcla con
  Backend.

## Alcance autorizado

- Inventariar rutas, tipo de checkout, ramas, HEAD, remotos, commits
  exclusivos, cambios sin commit, untracked, stashes, worktrees y tamaños.
- Transferir commits locales válidos al repositorio canónico mediante refs o
  ramas preservadas, sin sobrescribir cambios existentes.
- Crear commits y ramas de consolidación y preparar/publicar ramas cuando la
  preservación lo requiera, sin merge a ramas compartidas ni force-push.
- Retirar worktrees limpios con `git worktree remove` solo después de verificar
  preservación y ausencia de procesos/cambios pendientes.
- Trasladar clones independientes verificados a una cuarentena reversible fuera
  del workspace principal cuando la ruta de destino esté identificada.
- Corregir referencias locales obsoletas solo en archivos de configuración o
  scripts cuya dependencia de una copia retirada esté demostrada.
- Registrar evidencia, tamaños y espacio liberado.

## Fuera de alcance y rutas prohibidas

- No borrar definitivamente repositorios, backups, stashes, datos persistentes,
  volúmenes Docker ni información no verificada.
- No usar `git reset --hard`, `git checkout --`, `git worktree remove --force`,
  force-push ni merge a ramas compartidas.
- No cambiar contratos HTTP/OpenAPI/SQL, esquema DB, roles, secretos, runtime,
  frontend funcional ni workflows.
- No retirar worktrees activos de otra tarea sin evidencia de que están libres.
- No declarar un remoto Frontend/DB como autoridad contractual por inferencia.

## Criterios de aceptación

1. Cada ruta candidata tiene clasificación, tamaño, HEAD, rama, remoto,
   estado local, stashes, commits preservados y ubicación definitiva.
2. Los commits P0 indicados por el requisito (`fac2f640`, `95a202a`, `bf6de9e`,
   `66cf9e0`, `0694ad4`) son alcanzables desde refs preservadas o quedan
   explícitamente bloqueados con evidencia.
3. La ejecución normal del Frontend no depende de
   `frontend-lb004d-working`; sus cambios válidos están en el repositorio
   frontend canónico o en una rama preservada con ubicación definida.
4. Solo se retiran físicamente rutas limpias y verificadas; cualquier ruta con
   autoridad pendiente, cambios únicos, stash, backup o uso activo se conserva.
5. La validación registra comandos, fecha, exit code, limitaciones y espacio
   liberado. Borrado definitivo queda en lista separada para aprobación.

## Riesgos, rollback y stop conditions

- Riesgo: confundir un worktree limpio con un commit no publicado. Mitigación:
  `branch -a --contains`, refs locales/remotas, diff y snapshot antes de retirar.
- Riesgo: romper una tarea Codex/IDE activa. Mitigación: comprobar procesos,
  artefactos y metadatos de worktree; detener el retiro si hay uso activo.
- Rollback: restaurar el worktree desde su rama/commit o mover la cuarentena a
  su ruta original; no borrar la cuarentena en esta fase.
- Stop: cambios sin commit no recuperados, stash no inspeccionado, remoto/owner
  contradictorio, ruta persistente, proceso activo o necesidad de `--force`.

## Definition of Ready

`READY_FOR_SCOPED_INVENTORY_AND_REVERSIBLE_CLEANUP` para las rutas con
evidencia suficiente. `NOT_READY_FOR_PERMANENT_DELETION` para todas las rutas
hasta la aprobación posterior del usuario.

## Decisión operativa tras el inventario — 2026-10-20

- Backend canónico: `C:\Users\josev\AsistenciasUCO\AsistenciasUCO`.
- Frontend canónico: `C:\Users\josev\OneDrive\Documentos\Front_Asistecias\AsistenciasUCO-Frontend`;
  `origin` apunta a `https://github.com/AsistenciasUCO/AsistenciasUCO-Frontend.git`.
- DB canónica: `C:\Users\josev\OneDrive\Documentos\AsisteciaUco_db\git\gestion-asistencia-db`;
  `origin` apunta a `https://github.com/AsistenciasUCO/AsistenciasUCO-DB.git`.
- Infra canónica: `C:\Users\josev\AsistenciasUCO\AsistenciasUCO-Infra`.
- `frontend-lb004d-working`, `AsistenciasUCO-Frontend-p0b` y el clon DB del
  workspace fueron trasladados a cuarentena externa después de transferir sus
  refs relevantes. La cuarentena no es una copia oficial.
- El worktree DB `db-dev-consolidation` permanece fuera del workspace porque
  tiene tres cambios SQL sin commit; no se modifica ni se elimina.
