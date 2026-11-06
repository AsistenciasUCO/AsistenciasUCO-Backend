---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-05
---

# VALIDATION — HU136

Validación local del 2026-11-05. Los gates se corrieron uno a la vez.

| Capa | Comando | Resultado |
|---|---|---|
| DB | Gate completo en un contenedor limpio (también tras el ajuste de docentes sin vínculo) | `TOTAL_EXECUTED=442`, `PASSED=441`, `FAILED=0`, `CRITICAL_MISSING=0`, `DB GATE PASS` |
| Backend | `.\mvnw.cmd verify` | `BUILD SUCCESS`: 3127 tests, 0 fallos, 0 errores; ArchUnit en verde; "All coverage checks have been met" |
| Frontend | `npm run verify` | `TOTAL: 1524 SUCCESS`; statements 91.84 %, lines 92.6 %; build exit 0 |

Pendiente:
- Aprovisionar el PDP con `GET /api/v1/admin/docentes`.
- `--prune` de las 3 rutas de instituciones retiradas, solo con aprobación de John.
- Prueba en pantalla MV-017.
