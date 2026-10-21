# VALIDATION — P0-D

Fecha: 2026-10-20 (America/Bogota).

## Dictamen

`REVERSIBLE_CLEANUP_EXECUTED; PERMANENT_DELETE_PENDING_APPROVAL`.

Se retiraron del workspace los worktrees limpios mediante `git worktree
remove` sin `--force`. Las copias independientes con cambios/refs se movieron
a cuarentena externa después de transferir sus commits. No se borraron
repositorios, backups, stashes, ramas, remotos, SQL, datos persistentes ni
volúmenes Docker.

## Rutas canónicas y remotos comprobados

| Componente | Ruta canónica | Remoto comprobado | Estado |
|---|---|---|---|
| Backend | `C:\Users\josev\AsistenciasUCO\AsistenciasUCO` | `origin = https://github.com/alejovh12/AsistenciasUCO.git`; `upstream = https://github.com/AsistenciasUCO/AsistenciasUCO-Backend.git` | Preservado, rama `codex/p0-sec-001-coordinator-ownership`, HEAD `7f4438e` |
| Frontend | `C:\Users\josev\OneDrive\Documentos\Front_Asistecias\AsistenciasUCO-Frontend` | `origin = https://github.com/AsistenciasUCO/AsistenciasUCO-Frontend.git`; `legacy-frontend = https://github.com/shesho22/AsistenciasUCO-Frontend.git` | Preservado, rama `jose-valencia/maint-004-frontend-utc-v2-integration`, HEAD `b4368d1`, 3 cambios locales |
| DB | `C:\Users\josev\OneDrive\Documentos\AsisteciaUco_db\git\gestion-asistencia-db` | `origin = https://github.com/AsistenciasUCO/AsistenciasUCO-DB.git`; `legacy-db = https://github.com/johnjduque/gestion-asistencia-db.git` | Preservado, `develop`, HEAD `32b70eb`, `_codex-pr37/` sin rastrear |
| Infra | `C:\Users\josev\AsistenciasUCO\AsistenciasUCO-Infra` | `origin = https://github.com/AsistenciasUCO/AsistenciasUCO-Infra.git` | Preservado, `main`, HEAD `a544c49` |

## Inventario y acción ejecutada

Tamaños: bytes lógicos medidos antes de la operación; incluyen artefactos
generados cuando estaban presentes. `QUARANTINE` es reversible y no cuenta
como espacio físico liberado.

| Ruta original | Tamaño | Cambios únicos | Cambios preservados | Acción ejecutada | Estado final |
|---|---:|---|---|---|---|
| `C:\Users\josev\AsistenciasUCO\AsistenciasUCO` | 5,189,891,558 B | Cambios locales P0-C/P0-B, 1 stash y worktrees internos | `bf6de9e`, `66cf9e0`, `0694ad4`, `7f4438e`; cambios locales intactos | Preservar | CANÓNICO |
| `C:\Users\josev\AsistenciasUCO\AsistenciasUCO-Frontend-p0b` | 4,474,465 B | 3 modificaciones; hashes iguales a la copia OneDrive | Rama activa y todas las refs bajo `p0d-recovered/*` en Frontend canónico | Mover a cuarentena | QUARANTINE |
| `C:\Users\josev\AsistenciasUCO\AsistenciasUCO-Frontend-p0d` | 2,224,832 B | Ninguno; HEAD `e33ca3b` | Ref `p0d-recovered/codex/p0-d-frontend-stabilization` | `git worktree remove` | RETIRADO |
| `C:\Users\josev\AsistenciasUCO\AsistenciasUCO-Frontend-p0d-develop` | 1,495,116,456 B | Ninguno; HEAD `0366fc5` | Ref `p0d-recovered/codex/p0-d-frontend-develop-stabilization` | `git worktree remove` | RETIRADO |
| `C:\Users\josev\AsistenciasUCO\AsistenciasUCO-Infra` | 1,564,590 B | Ninguno | Repo/HEAD `a544c49` | Preservar | CANÓNICO |
| `C:\Users\josev\AsistenciasUCO\AsistenciasUCO-Infra-p0d` | 3,309,363 B | Ninguno; HEAD `05530ec` | Rama local `codex/p0-d-infra-validation` | `git worktree remove` | RETIRADO |
| `C:\Users\josev\AsistenciasUCO\be-stab` | 433,603,970 B | Ninguno; HEAD `0d8fa9f` | Rama `codex/stabilize-lb004d-backend` | `git worktree remove` | RETIRADO |
| `C:\Users\josev\AsistenciasUCO\db-stab` | 2,601,968 B | Ninguno; HEAD `bdb2935` | Ref remota legacy DB `codex/lb-004d-d1-archivo-soporte` | `git worktree remove` | RETIRADO |
| `C:\Users\josev\AsistenciasUCO\fe-stab` | 677,219,829 B | Ninguno; HEAD `06f70c4` | Ref `p0d-recovered/codex/stabilize-lb004d-frontend` | Retiro Git verificado al final; sin `--force` | RETIRADO |
| `C:\Users\josev\AsistenciasUCO\lb004d-backend` | 202,563,414 B | Ninguno; HEAD `f45a521` | Rama/remoto `jose-valencia/lb-004d-b1-support-binding` | `git worktree remove` | RETIRADO |
| `C:\Users\josev\AsistenciasUCO\lb004d-db` | 2,586,042 B | Ninguno; HEAD `b698c76` | Ref remota legacy DB D1 | `git worktree remove` | RETIRADO |
| `C:\Users\josev\AsistenciasUCO\lb004d-frontend` | 1,568,762,662 B | Ninguno; HEAD `06f70c4` | Ref `p0d-recovered/jose-valencia/lb-004d-f1-support-binding` | `git worktree remove` | RETIRADO |
| `C:\Users\josev\AsistenciasUCO\reconcile-backend-2026-10-18` | 436,852,561 B | Ninguno; HEAD `53fc48d` | Rama/remoto `codex/reconcile-backend-2026-10-18` | `git worktree remove` | RETIRADO |
| `C:\Users\josev\AsistenciasUCO\reconcile-db-2026-10-18` | 2,428,333 B | Ninguno; HEAD `034e6e8` | Ref remota legacy DB reconcile | `git worktree remove` | RETIRADO |
| `C:\Users\josev\AsistenciasUCO\reconcile-frontend-2026-10-18` | 686,467,073 B | Ninguno; HEAD `0366fc5` | Ref `p0d-recovered/codex/reconcile-frontend-2026-10-18` | Retiro Git verificado al final; sin `--force` | RETIRADO |
| `C:\Users\josev\AsistenciasUCO\gestion-asistencia-db` | 10,135,271 B | D0 `4e5e218` no estaba en la DB OneDrive | `codex/p0-d-db-d0-recovered`; demás heads en refs/remotos | Mover a cuarentena | QUARANTINE |
| `C:\Users\josev\AsistenciasUCO\AsistenciasUCO\frontend-lb004d-working` | 1,833,921,996 B | `a33876b` y `95a202a`; logs regenerables sin rastrear | `p0d-recovered/codex/p0-d-frontend-f1-recovered` en Frontend canónico | Detener procesos Angular acotados y mover a cuarentena | QUARANTINE |
| `C:\Users\josev\AsistenciasUCO\AsistenciasUCO\w` | 6,660,829 B | 2 documentos no rastreados | Commit `ddc4654` en `codex/local-lb004d-stabilization` | Commit local y `git worktree remove` | RETIRADO |
| `C:\Users\josev\AsistenciasUCO\db-backups` | 86,159,360 B | Backups no Git | 7 archivos preservados | No tocar | CONSERVAR |

Los worktrees internos limpios de Backend (`.codex/worktrees/*`,
`.workspace/*`, `.claude/worktrees/*`, `.w/sec-mvc`) también fueron retirados
con Git; sus ramas/commits quedaron en el repositorio Backend. Los worktrees
prunable sin checkout se limpiaron con `git worktree prune` únicamente en la
DB OneDrive después de comprobar que sus carpetas ya no existían.

## Commits recuperados

- Backend: `bf6de9e`, `66cf9e0`, `0694ad4`, `7f4438e` en la rama canónica;
  `ddc4654` en `codex/local-lb004d-stabilization`.
- Frontend: `fac2f640`, `a33876b`, `95a202a` en
  `p0d-recovered/codex/p0-d-frontend-f1-recovered`; la corrección docente
  `fac2f640` además está en `legacy-frontend`.
- DB: `4e5e218` en `codex/p0-d-db-d0-recovered`; `b698c76`, `034e6e8` y
  `bdb2935` son alcanzables desde refs legacy remotas.

## Dependencias de copias retiradas

La búsqueda en código, scripts, configuración IDE y archivos operativos no
encontró consumidores activos de `frontend-lb004d-working` ni de los worktrees
retirados. Las coincidencias restantes son referencias históricas en
auditorías P0-B/P0-C y el propio work item P0-D; no son rutas de ejecución.

## Espacio y límites

- Worktrees retirados: `8,198,233,890 B` lógicos medidos antes del retiro
  (aprox. `7,818.45 MiB`, `8.20 GB` decimal).
- Copias movidas a cuarentena: `1,848,192,574 B`; ese espacio sigue ocupado
  fuera del workspace y no se cuenta como liberado.
- `C:` reporta `71,063,838,720 B` libres al cierre. No se tomó una fotografía
  previa del espacio libre, por lo que no se afirma una delta física de disco;
  el valor reclamado es el tamaño lógico de los worktrees retirados.
- Se trasladaron 18 logs históricos raíz `e2e-*.log` de 2026-10-11, total
  `178,528 B`, a `C:\Users\josev\AsistenciasUCO-P0D-quarantine-2026-10-20\historical-e2e-logs`.
  El log versionado `docs/work-items/LB-004-stateless-serverless-readiness/evidence/LB-004B.2-red-test-compile-output.log`
  se conservó.
- No se tocaron `db-backups`, Docker, volúmenes, SQL Server, MinIO, ClamAV,
  Keycloak ni datos persistentes.

## Validación ejecutada

| Comprobación | Resultado |
|---|---|
| Estados Git y remotos de rutas canónicas | PASS; registrados arriba |
| `git worktree list --porcelain` final | PASS; solo raíces canónicas y `db-dev-consolidation` con cambios SQL |
| Commits recuperados alcanzables | PASS; listado arriba |
| Hashes de las 3 modificaciones frontend p0b vs copia OneDrive | PASS; SHA-256 iguales |
| Referencias activas a clones retirados | PASS; solo histórico/documentación |
| Procesos Angular de la copia retirada | PASS; `ng serve`/`npm test:ci` detenidos de forma acotada; puerto 4200 libre |
| Tests de aplicación | NO_RUN; no hubo cambio de código de aplicación |
| Integración SQL/DB | NO_RUN; no se modificó SQL y el worktree `db-dev-consolidation` conserva cambios |
