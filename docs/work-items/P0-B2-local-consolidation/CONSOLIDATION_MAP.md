# Matriz de consolidación local — 2026-10-20

## Preservar ahora

- Backend canónico: `C:\Users\josev\AsistenciasUCO\AsistenciasUCO`.
- Frontend F1 candidato: `C:\Users\josev\AsistenciasUCO\AsistenciasUCO\frontend-lb004d-working`.
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

`NO_DELETE_PENDING_EXPLICIT_AUTHORIZATION`.

No se eliminó ninguna carpeta, backup, stash, rama ni remoto. El espacio
reclamable no se declara hasta obtener un inventario con tamaños y una
confirmación explícita de los objetivos exactos.

