---
status: active
type: active
scope: backend
owner: backend-team
last-reviewed: 2026-09-28
---

# LB-002.2B — BASELINE (`clean verify` previo al RED)

Evidencia de la etapa **BASELINE** ([TEST_PLAN §Revisión 2.2A](TEST_PLAN.md)). Ejecutada **antes** de crear cualquier test o archivo Java de 2.2B.

```text
BASELINE_CLEAN_VERIFY: PASS
```

| Campo | Valor |
|---|---|
| Git | rama `jose-valencia/lb-002.2a-jpa-command-plan`, HEAD `0bfc02a58a8abc894841cc1e9b31c002a716554e`; único cambio local = documentos de trabajo (`??`), sin cambios en `src/` |
| Comando | `.\mvnw.cmd clean verify` |
| Entorno | Sin SQL Server, sin Azure, sin variables `SPRING_DATASOURCE_*`/`AZURE_*`/`APP_*` cargadas (comprobado con `Get-ChildItem Env:`) |
| Inicio / fin | 2026-09-28 00:49:11 -05:00 / 2026-09-28 00:51:53 -05:00 (Maven `Total time: 02:38 min`) |
| Exit code | **0** — `BUILD SUCCESS` |
| Tests (Surefire) | **1291** run · **0** failures · **0** errors · **0** skipped |
| JaCoCo LINE | **90.53 %** (7465 / 8246) — gate ≥ 80 %: `All coverage checks have been met.` |
| JaCoCo BRANCH | **80.81 %** (1706 / 2111) — gate ≥ 70 % |
| ArchUnit | 20 clases `architecture.*` en el verify, 0 failures, 0 skipped (incl. `JpaIsolationRulesTest` 6/6, `AdapterCompositionRootRulesTest` 8/8, `CleanArchitectureRulesTest` 20/20, `InfrastructureStructureRulesTest` 5/5) |
| Gates OpenAPI | `OpenApiGoldenPathConformanceTest` 9/9, `OpenApiGoldenPathValidationTest` 2/2, `SwaggerUiDisabledRuntimeTest` 1/1, `SwaggerUiEnabledRuntimeTest` 4/4 — 0 failures |
| Clasificación | No aplica `BASELINE_REGRESSION` ni `ENVIRONMENT_BLOCKER` |

Notas:

- Los contadores JaCoCo se leyeron del `BUNDLE` en `target/site/jacoco/jacoco.xml` (sin exclusiones en `pom.xml`). El margen de BRANCH registrado en [PLAN R-06](PLAN.md) (70.50 %) procedía de la fase 1B (1067 tests); en este HEAD medido el margen es amplio (80.81 %). R-06 sigue vigente como vigilancia para 2.2C/2.2D: repetir la medición en cada `clean verify`.
- Los 1291 tests son el universo hermético del verify normal; los ITs (`*IT`) no participan (perfil `integration`). Esto **no** dice nada sobre TD-043 ni sobre `-Pintegration verify`.
- Log completo del run: no se versiona (contiene rutas locales); la evidencia es este resumen y los reportes regenerables en `target/`.
