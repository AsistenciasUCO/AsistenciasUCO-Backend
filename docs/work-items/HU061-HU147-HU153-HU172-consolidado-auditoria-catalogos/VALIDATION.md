---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-04
---

# VALIDATION — HU061 / HU147 / HU153 / HU172 y P5

Validación local (Windows 10, Java 25), 2026-11-04. Los gates se corrieron uno a la vez.

| Capa | Comando | Resultado |
|---|---|---|
| Backend | `.\mvnw.cmd verify` | `BUILD SUCCESS`: 3176 tests, 0 fallos, 0 errores, 0 omitidos; ArchUnit en verde; "All coverage checks have been met" |
| Backend | Pruebas dirigidas del work item (casos de uso, repositorios, controladores, clasificador, ArchUnit, unicidad de repositorios = 54, `AccessCatalogConsistencyTest`) | todas en verde |
| DB | Gate completo en un contenedor SQL Server 2022 limpio (réplica del job `DB Quality Gate CI/CD`) | `TOTAL_EXECUTED=421`, `PASSED=420`, `FAILED=0`, `SKIPPED=1`, `DB GATE PASS` (detalle en el `VALIDATION.md` del work item homónimo de la BD) |
| Frontend | `npm run verify` | `TOTAL: 1515 SUCCESS`; statements 91.7 %, lines 92.51 %; build de producción exit 0 |

Notas:

- La primera corrida de `verify` falló en `ApplicationShouldNotCarryTechnicalCorrelationTest` y `ApplicationShouldNotUseInfrastructureTechnicalApisTest`, porque el evento de auditoría tenía un campo `idCorrelacion` en la capa de aplicación. Se renombró a `idTraza` (es la traza de la petición auditada, no la correlación técnica de la petición actual) en el backend, el frontend y el contrato.
- La primera corrida de `npm run verify` tuvo un fallo de orden aleatorio en una spec nueva (`afterEach` sin TestBed); se corrigió.

Limitaciones: falta la prueba en pantalla con usuarios reales y el PDP reaprovisionado con las 6 rutas nuevas ([MV-013](../../baseline/MANUAL_VALIDATION_LEDGER.md)). El CI remoto corre al subir las ramas.
