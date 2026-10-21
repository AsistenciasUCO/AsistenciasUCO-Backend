# TEST_PLAN — P0-D

## Alcance

Este work item no cambia código de aplicación, contratos ni esquema DB. No se
crea un test RED Java/Angular. La validación es operacional y observable.

## Comprobaciones

| ID | Comprobación | Evidencia | Criterio |
|---|---|---|---|
| P0D-GIT-01 | `git status`, rama, HEAD y remotos por ruta | inventario | sin estado omitido |
| P0D-GIT-02 | `git worktree list --porcelain` y tipo de `.git` | inventario | distinguir worktree, clon y enlace |
| P0D-GIT-03 | `branch -a --contains` para commits recuperados | inventario/validación | cada commit tiene ref preservada |
| P0D-GIT-04 | diff, untracked y stash antes de retirar | inventario/validación | ningún cambio único se descarta |
| P0D-FE-01 | búsqueda de referencias a `frontend-lb004d-working` | validación | no queda dependencia activa no documentada |
| P0D-FS-01 | tamaño lógico antes/después y destino de cuarentena | validación | espacio reportado con límites |
| P0D-OPS-01 | apertura/rutas canónicas y procesos activos | validación | backend/frontend/DB apuntan a rutas definidas |

## Gate funcional

Si la consolidación modifica código o configuración de ejecución, se ejecutan
los tests afectados y los gates aplicables conforme al runbook. Si solo se
retiran copias independientes o worktrees limpios, no se repite el suite
completo; se conserva la evidencia previa y se ejecutan las comprobaciones
operacionales anteriores.
