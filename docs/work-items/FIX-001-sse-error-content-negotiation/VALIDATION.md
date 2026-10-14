---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-14
---

# VALIDATION — FIX-001: errores de `/api/v1/realtime/stream` con `Accept: text/event-stream`

## Dictamen

**PASS local** (suite normal + gates JaCoCo). Pendiente: CI remoto del PR y E2E real SEC-BASELINE-002, que no se presumen ejecutados. Tester, implementador y validador fueron el mismo agente: la independencia es solo conceptual y requiere revisión del auditor o del PR.

## Comandos ejecutados

| Fecha/entorno/base | Comando | Exit code | Resultado | Evidencia sanitizada |
|---|---|---|---|---|
| 2026-10-14, Windows 11, JDK 25, base `develop` `4082736`, **sin fix** | `./mvnw.cmd -B -ntp -q test -Dtest=RealtimeStreamErrorHttpTest -Djacoco.skip=true` | ≠0 | RED: `Tests run: 4, Failures: 2, Errors: 1` | F1-01 `ServletException …ForbiddenException`; F1-02/F1-03 `Content type not set`; log `Failure in @ExceptionHandler … HttpMediaTypeNotAcceptableException: No acceptable representation` |
| 2026-10-14, mismo entorno, **con fix** | `./mvnw.cmd -B -ntp -q test -Dtest=RealtimeStreamErrorHttpTest,GlobalExceptionHandler*,*ErrorHttpTest,SesionV2HttpContractRedTest,RealtimeEventsControllerTest -Djacoco.skip=true` | 0 | GREEN: 6 suites, 0 fallos | No aparece `Failure in @ExceptionHandler` |
| 2026-10-14, mismo entorno, **con fix** | `./mvnw.cmd -B -ntp verify` | 0 | `BUILD SUCCESS` (3:40 min); `Tests run: 1538, Failures: 0, Errors: 0, Skipped: 0`; `All coverage checks have been met.` | `RealtimeStreamErrorHttpTest`: 4/4 PASS |
| 2026-10-14, mismo entorno, **con fix + F1-05** (revisión previa al PR) | `./mvnw.cmd -B -ntp verify` | 0 | `BUILD SUCCESS` (2:36 min); `Tests run: 1540, Failures: 0, Errors: 0, Skipped: 0`; `All coverage checks have been met.` | `RealtimeStreamErrorHttpTest` 4/4, `GlobalExceptionHandlerFrameworkErrorsTest` 7/7, `GlobalExceptionHandlerTest` 17/17; ArchUnit `architecture.*` 82/82 (20 clases); sin `Failure in @ExceptionHandler` en el log |

## Checks

| Nivel | Check | PASS / FAIL / NO APLICA / BLOQUEADO | Evidencia/motivo |
|---|---|---|---|
| Alcance | Plan/criterios | PASS | Solo cambian `GlobalExceptionHandler` (+9/−1), un test nuevo, dos tests 405 añadidos a `GlobalExceptionHandlerFrameworkErrorsTest`, este work item y el índice de work items |
| Arquitectura | ArchUnit/dependencias | PASS | Suites `architecture.*` dentro de `verify`; `MediaType` es de Spring Web en un primary adapter (ya permitido) |
| Contratos | HTTP/DB/security/eventos | PASS | 403 → `Forbidden` y 400 → `BadRequest` (`ApiErrorResponse` JSON) según OpenAPI; éxito `text/event-stream` intacto (F1-04); OpenAPI sin cambios |
| Tests | RED → GREEN, negativos | PASS | F1-01..F1-03 RED → GREEN; F1-04 guardia de éxito; F1-05 guardia 405/`Allow` |
| Coverage | Líneas ≥80 %, ramas ≥70 % | PASS | Gate `jacoco:check` cumplido. Totales de `target/site/jacoco/jacoco.csv`: LINE 8191/9037 = 90,64 %; BRANCH 1967/2442 = 80,55 % (último `verify`). `GlobalExceptionHandler`: LINE 131/131, BRANCH 30/34 |
| Integración | DB real, fixtures, skips | NO APLICA | El cambio es de la capa MVC; no toca persistencia ni providers. `-Pintegration` no ejecutado |
| Build | mvn verify | PASS | Exit 0 |
| Seguridad | Secretos, RBAC, scanners | PASS (local) | Sin cambios en RBAC/ownership/cadena de seguridad; la denegación sigue ocurriendo antes de abrir el stream (`activeSubscribersCount()==0`); sin secretos. CodeQL/Trivy/Sonar quedan para el CI remoto |
| Documentos | Enlaces, deuda, manuales, ADR | PASS | PLAN/TEST_PLAN/VALIDATION; sin deuda nueva; validación manual E2E pendiente; ADR no aplica |

## Integridad de RED

SHA-256 de `RealtimeStreamErrorHttpTest.java` en RED y después del `verify` GREEN: `d1ac4604f9dc277c3c5ec7c92edd37aa3c96230332be43b9a07aecff271459fa` (idéntico). El test no se modificó para el GREEN.

## Bloqueantes y limitaciones

- MockMvc standalone no ejecuta Tomcat ni la cadena JWT; el 500 del contenedor se infiere de la `ServletException` no resuelta, coherente con la evidencia E2E original.
- Los porcentajes del CSV son totales del reporte; la decisión vinculante es el `jacoco:check` del `pom.xml`, que pasó.

## Evidencias remotas/manuales pendientes

- CI remoto del PR (backend-ci, security, Sonar) por SHA, cuando se abra el PR contra `develop`.
- E2E real SEC-BASELINE-002: `EventSource`/curl con `Accept: text/event-stream` y grupo ajeno → esperar 403 `FORBIDDEN` JSON; sin `grupoId` → 400 con cuerpo.
