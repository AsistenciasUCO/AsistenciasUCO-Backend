# Matriz de consolidación local — 2026-10-20

## Preservar ahora

- Backend canónico: `C:\Users\josev\AsistenciasUCO\AsistenciasUCO`.
- Frontend canónico tras P0-D: `C:\Users\josev\OneDrive\Documentos\Front_Asistecias\AsistenciasUCO-Frontend`.
- DB canónica: `C:\Users\josev\OneDrive\Documentos\AsisteciaUco_db\git\gestion-asistencia-db`.
- Infra canónica: `C:\Users\josev\AsistenciasUCO\AsistenciasUCO-Infra`.
- `C:\Users\josev\AsistenciasUCO\db-backups` y los worktrees/stashes
  existentes, hasta cerrar la revisión del owner DB.

## Candidatos no retirados

La auditoría reporta copias `*-p0b`, `*-p0d`, `be-stab`, `db-stab`, `fe-stab`,
`lb004d-*` y `reconcile-*`. Se mantienen porque aún no se ha demostrado para
cada una que su contenido esté en un remoto/commit recuperable y porque
algunas contienen cambios de fases distintas.

## Decisión

`P0-D-EXECUTED-REVERSIBLE-CLEANUP`.

El inventario y la limpieza reversible se ejecutaron en
[P0-D](../P0-D-consolidacion-fisica/VALIDATION.md). Se retiraron worktrees
limpios mediante Git sin `--force`; tres clones independientes se trasladaron
a `C:\Users\josev\AsistenciasUCO-P0D-quarantine-2026-10-20` después de
preservar sus refs. No se borraron definitivamente carpetas, backups, stashes,
ramas, remotos, SQL ni datos persistentes.

La ruta Frontend oficial documentada es la copia OneDrive y la DB oficial
documentada es la copia OneDrive; los remotos fork/legacy se conservan como
aliases observados y no se declara una decisión contractual nueva. El retiro
definitivo continúa pendiente de aprobación de las rutas exactas.

