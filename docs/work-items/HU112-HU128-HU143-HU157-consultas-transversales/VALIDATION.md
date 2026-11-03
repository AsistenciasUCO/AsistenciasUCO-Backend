---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-03
---

# VALIDATION — HU112 / HU128 / HU143 / HU157

Validación local (Windows 10, Java 25) del 2026-11-02/03.

| Capa | Comando | Resultado |
|---|---|---|
| Backend | `.\mvnw.cmd verify` | `BUILD SUCCESS`: 3040 tests, 0 fallos, 0 errores, 0 omitidos; ArchUnit en verde; "All coverage checks have been met" (JaCoCo LINE ≥ 80 % / BRANCH ≥ 70 %) |
| Backend | `.\mvnw.cmd verify` tras integrar `develop` (APP-01, APP-02 y APP-04), 2026-11-03 | `BUILD SUCCESS`: 3051 tests, 0 fallos, 0 errores, 0 omitidos; JaCoCo en verde (incluye el wiring y la consistencia del catálogo) |
| Backend | `AccessCatalogConsistencyTest` (repetido después de agregar las 7 rutas a `access-catalog.json`) | exit 0 |
| Backend | `ConsultasTransversalesWiringConfigurationTest` (agregado tras el `verify` para cubrir el wiring) | exit 0 |
| Backend | Cobertura JaCoCo de las clases nuevas | 100 % de líneas y ramas en caso de uso, interactor, `ConsultaPaginadaRules`, controladores y DTO; `ConsultasTransversalesJpaRepository` 98.1 % de líneas y 85 % de ramas; `DbCodeTranslator` 88 % / 81.8 % |
| DB | `test/test_consultas_transversales_hu112_hu157.sql` sobre el SQL Server local | 8/8 `TEST_PASS` (T-112-01, T-128-01, T-143-01, T-157-01, T-157-02, T-SEC-03, T-PAG-02, T-INT-01) |
| DB | Gate completo en un contenedor SQL Server 2022 limpio (réplica del job `DB Quality Gate CI/CD`) | 2026-11-03: `TOTAL_EXECUTED=396`, `PASSED=395`, `FAILED=0`, `SKIPPED=1`, `DB GATE PASS` (detalle en el `VALIDATION.md` del work item homónimo de la BD) |
| Frontend | `npm run verify` | `TOTAL: 1410 SUCCESS`; cobertura realtime 95.06 % / 84.52 %; build de producción exit 0. Tras integrar `develop` (APP-04 y APP-06), 2026-11-03: `TOTAL: 1471 SUCCESS`, statements 91.6 %, lines 92.52 %, build exit 0 |
| CI remoto (2026-11-03) | GitHub Actions y SonarCloud de los PR BD #79, backend #77, frontend #36 e Infra #9 | success en todos los checks: BD (Database Deployment & Quality Gate, SonarCloud), backend (Backend Quality Gate, ArchUnit, CodeQL, Trivy, Dependency Review, SonarCloud), frontend (build-test-verify, SonarCloud) e Infra (Validate official Infra contents); mergeados en `develop` |

Limitaciones:

- Integración JDBC contra una DB real (`-Pintegration`) no ejecutada: los SP se probaron con la suite SQL y el repositorio con mocks de `EntityManager`.
- La prueba en pantalla con usuarios reales y el PDP reaprovisionado queda en [MV-012](../../baseline/MANUAL_VALIDATION_LEDGER.md).
