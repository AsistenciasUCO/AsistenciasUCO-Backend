---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-04
---

# VALIDATION — HU058 / HU155 / HU167

Validación local del 2026-11-04. Los gates se corrieron uno a la vez.

| Capa | Comando | Resultado |
|---|---|---|
| Backend | `.\mvnw.cmd verify` | `BUILD SUCCESS`: 3086 tests, 0 fallos, 0 errores, 0 omitidos; ArchUnit en verde; "All coverage checks have been met" |
| DB | Gate completo en un contenedor limpio | `TOTAL_EXECUTED=432`, `PASSED=431`, `FAILED=0`, `SKIPPED=1`, `DB GATE PASS`, tras el arreglo de salida de `SES01_AUTO_CLOSED` (ver el VALIDATION de la BD) |
| Frontend | `npm run verify` | `TOTAL: 1488 SUCCESS`; statements 91.68 %, lines 92.48 %; build exit 0 |

Pendiente: reaprovisionar el PDP con `GET /api/v1/admin/estados` (cuando John lo escriba) y la prueba en pantalla MV-014.
