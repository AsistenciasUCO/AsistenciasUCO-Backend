---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-05
---

# VALIDATION — HU159

Validación local del 2026-11-05. Los gates se corrieron uno a la vez.

| Capa | Comando | Resultado |
|---|---|---|
| DB | Gate completo en un contenedor limpio | Corrida 1: `DB GATE INCOMPLETE` por la regla de resultset canónico (el SP nuevo devuelve 3 columnas extra). Se agregó a la excepción, como `usp_actualizar_coordinador`. Corrida 2: `PASSED=440`, `FAILED=0`, `CRITICAL_MISSING=0`, `DB GATE PASS` |
| Backend | `.\mvnw.cmd verify` | Corrida 1: 3123 tests, 1 falla (`JpaRepositoryBeanUniquenessTest`: 54 repositorios por `CorreccionIdentidadJpaRepository`; se actualizó el conteo). Corrida 2: `BUILD SUCCESS`, 3123 tests, 0 fallos, 0 errores; ArchUnit en verde; "All coverage checks have been met" |
| Frontend | `npm run verify` | `TOTAL: 1520 SUCCESS`; statements 91.77 %, lines 92.54 %; build exit 0 |

Pendiente:
- Aprovisionar el PDP con las 3 rutas nuevas, cuando John lo pida.
- Prueba en pantalla MV-016, incluido el cambio de username en el Keycloak del lab y la re-prueba de HU121/HU092.
