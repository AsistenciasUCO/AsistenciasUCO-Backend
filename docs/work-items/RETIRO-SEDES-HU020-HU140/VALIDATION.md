---
status: active
type: work-item
scope: backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-06
---

# VALIDATION — RETIRO-SEDES-HU020-HU140

Validación local del 2026-11-06. Los gates se corrieron uno a la vez, partiendo de `develop` con HU136 ya mergeado.

| Capa | Comando | Resultado |
|---|---|---|
| Backend | `.\mvnw.cmd verify` | `BUILD SUCCESS`: 3128 tests, 0 fallos, 0 errores; "All coverage checks have been met" |
| Backend (scripts del baseline) | `python -m unittest test_e2e_safety` en `infra/security-baseline/scripts` | 44 tests, OK |
| Frontend | `npm run verify` | `TOTAL: 1521 SUCCESS`; statements 91.84 %, lines 92.62 %; build de producción exit 0 |
| BD | — | Sin cambios |

Pendiente:
- Prueba en pantalla MV-018.
- `--prune` en el PDP de las 8 rutas retiradas, solo con aprobación de John.
