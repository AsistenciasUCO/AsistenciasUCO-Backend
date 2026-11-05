---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-05
---

# VALIDATION — HU005 / HU016

Validación local del 2026-11-05. Los gates se corrieron uno a la vez.

| Capa | Comando | Resultado |
|---|---|---|
| DB | Gate completo en un contenedor SQL Server 2022 limpio | `FREEZE_EXIT=0`, `DEPLOY_EXIT=0`, `TEST_PASS:T-005-01`, `TOTAL_EXECUTED=432`, `PASSED=431`, `FAILED=0`, `SKIPPED=1` (permitido), `DB GATE PASS` |
| Backend | `.\mvnw.cmd verify` | `BUILD SUCCESS`: 3101 tests, 0 fallos, 0 errores, 0 omitidos; ArchUnit en verde; "All coverage checks have been met" |
| Frontend | `npm run verify` | `TOTAL: 1503 SUCCESS`; statements 91.72 %, lines 92.5 %; build exit 0. La primera corrida falló en 2 specs: el efecto que preselecciona la categoría escribía una señal sin `allowSignalWrites` (Angular 18). Se corrigió y se repitió completo |

Pendiente: aprovisionar en el PDP `GET /api/v1/categorias-justificacion` (cuando John lo escriba) y la prueba en pantalla MV-015.
