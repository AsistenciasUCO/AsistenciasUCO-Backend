---
status: active
type: work-item
scope: cross-repository-workspace
owner: backend-team
last-reviewed: 2026-10-20
---

# P0-B2 — Consolidación local segura de repositorios

## Objetivo

Dejar una matriz verificable de las copias locales y sus refs antes de
retirar cualquier carpeta. La copia backend canónica es
`C:\Users\josev\AsistenciasUCO\AsistenciasUCO`; Frontend y DB mantienen sus
repositorios oficiales; Infra se conserva separada. La consolidación no
implica borrar worktrees, backups, stashes o clones con cambios únicos.

## AS-IS comprobado

La auditoría `P0-estabilizacion-multirrepositorio/CONSOLIDATION_AUDIT_2026-10-20.md`
registró múltiples worktrees y clones de Backend, Frontend, DB e Infra, con
ramas `lb004d`, `p0d`, `stab` y `reconcile`. La rama activa del backend raíz
contiene cambios locales del usuario; el clon frontend de trabajo dentro de
la raíz está limpio en `fac2f640` y contiene la corrección docente.

## TARGET

| Componente | Copia de trabajo a preservar | Decisión actual |
|---|---|---|
| Backend | `C:\Users\josev\AsistenciasUCO\AsistenciasUCO` | Canónica; no se resetea ni se reemplaza. |
| Frontend | Copia oficial/working copy del repositorio frontend y el clon F1 hasta cierre de revisión | Preservar ambos mientras se verifica owner, PR y refs. |
| DB | `C:\Users\josev\OneDrive\Documentos\AsisteciaUco_db\git\gestion-asistencia-db` | Canónica DB; no se modifica SQL ni se eliminan backups. |
| Infra | `C:\Users\josev\AsistenciasUCO\AsistenciasUCO-Infra` | Se preserva separada; no es backend. |

## Clase y variable principal

- Change class: `CONTRACT_ANALYSIS`.
- `PRIMARY_VARIABLE`: `LOCAL_COPY_AUTHORITY_AND_RECOVERABILITY`.
- Allowed paths: esta carpeta de work item y documentos de inventario
  sanitizados.
- Forbidden paths: `Remove-Item`/borrado de carpetas, borrar backups o
  stashes, reset, force-push, borrar ramas remotas, merge final, cambiar
  remotos por suposición y copiar secretos.

## Alcance ejecutable sin nueva autorización

- Enumerar rutas, ramas, SHAs, worktrees, stashes y remotos con evidencia
  reproducible.
- Comparar refs y marcar candidatos de retiro solo cuando no tengan cambios
  únicos o exista un snapshot recuperable.
- Publicar una propuesta de retiro para confirmación humana.

## Stop conditions

- owner de Frontend/DB no confirmado;
- divergencia no explicada entre remoto legacy y oficial;
- cambios no publicados, stashes, backups o artefactos únicos;
- cualquier acción que elimine datos o haga irreversible la recuperación.

## Definition of Ready

`READY_FOR_INVENTORY_ONLY`; `NOT_READY_FOR_DELETION`. La autorización de la
misión permite ramas/commits/PRs, pero exige confirmación explícita antes de
borrar carpetas o información potencialmente única.

