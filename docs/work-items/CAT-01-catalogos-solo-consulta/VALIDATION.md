---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-04
---

# VALIDATION — CAT-01: catálogos base de solo consulta

Validación local del 2026-11-04. Los gates se corrieron uno a la vez.

| Capa | Comando | Resultado |
|---|---|---|
| Backend | `.\mvnw.cmd verify` | `BUILD SUCCESS`: 3079 tests (menos que antes porque se quitaron los de escritura), 0 fallos; ArchUnit en verde; "All coverage checks have been met" |
| Backend | `AccessCatalogConsistencyTest`, `JpaRepositoryBeanUniquenessTest` (53) | en verde dentro de `verify` |
| DB | Gate completo en un contenedor limpio | `TOTAL_EXECUTED=432`, `PASSED=431`, `FAILED=0`, `SKIPPED=1`, `DB GATE PASS` |
| Frontend | `npm run verify` | `TOTAL: 1480 SUCCESS` (menos specs porque se quitaron las de escritura); statements 91.69 %, lines 92.5 %; build exit 0 |

Pendiente:
- Reaprovisionar el PDP para retirar las 8 rutas de escritura. Solo se hace cuando John lo escriba.
- Prueba en pantalla: MV-013 del ledger.
