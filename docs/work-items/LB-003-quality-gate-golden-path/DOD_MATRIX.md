---
status: closed
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-09-29
---

# DoD Matrix — LB-003 Quality Gate Golden Path

Clasificación de entrada exigida por la tarea: `PASS_PREVIOUS_EVIDENCE`, `RUN_REQUIRED`, `GAP` o
`NO_APLICA`. `PENDING`, `NOT_RUN` y `NOT_OBSERVABLE` nunca equivalen a PASS.

| Área DoD | Clasificación de entrada | Evidencia concreta / acción | Resultado LB-003 |
|---|---|---|---|
| Alcance y contrato | RUN_REQUIRED | Contrato congelado por [LB-001C](../LB-001C-openapi-contract-first/CLOSURE.md); gates OpenAPI actuales 16/16; no se modificó contrato ni código | PASS |
| RED -> GREEN | NO_APLICA | No hay cambio funcional ni de contrato; [TEST_PLAN](TEST_PLAN.md) reutiliza tests existentes y RED cosmético está prohibido | NO_APLICA |
| Build | RUN_REQUIRED | Java 25 + `clean verify`: BUILD SUCCESS; 1426 tests, 0 failures, 0 errors, 0 skips; JAR presente | PASS |
| Arquitectura | RUN_REQUIRED | ArchUnit dentro de `verify`: 19 clases / 85 tests, 0 F/E/S; aislamiento JPA y puertos neutrales también certificados en [LB-002.2C](../LB-002-jpa-incremental/LB-002.2-jpa-command-pilot/CLOSURE.md) | PASS |
| Coverage | RUN_REQUIRED | JaCoCo BUNDLE de la corrida `clean verify`: LINE 7630/8337 = 91.52 %; BRANCH 1746/2151 = 81.17 % | PASS |
| Persistencia / providers | RUN_REQUIRED | targeted SQL Server de siete clases sobre `sql_server_asistencias` / `gestionasistenciadb`: 59/59, 0 F/E/S, paridad 0 mismatches | PASS |
| Seguridad | RUN_REQUIRED | matriz 401/403/ownership/éxito de [TEST_PLAN](TEST_PLAN.md); tests normales + targeted SQL + MV-006 | PASS |
| Operación | RUN_REQUIRED | correlation/logs/evento PASS por tests; SSE/readback PASS por targeted + [MV-006](../../baseline/MANUAL_VALIDATION_LEDGER.md). Auditoría SQL real del request: `NOT_OBSERVABLE`, clasificada no bloqueante por TD-010 | PASS (con límite de auditoría explícito) |
| CI — Ruleset / required checks | RUN_REQUIRED | ruleset `Protect develop` activo sobre `refs/heads/develop`: PR requerido, deletion/non-fast-forward bloqueados, sin bypass; 4 required checks | PASS |
| CI — remote runs sobre implementación actual | RUN_REQUIRED | PR #14, commit `e92afb73221ac3a967e63c673312d3961cfb0688`: `Backend Quality Gate`, `CodeQL Java Analysis`, `Dependency Review` y `SonarCloud Code Analysis` SUCCESS; check adicional `CodeQL` SUCCESS. SonarCloud: Quality Gate passed, 0 Security Hotspots, 100 % coverage on new code, 0 % duplication on new code | PASS |
| Documentación | RUN_REQUIRED | TEST_PLAN, DOD_MATRIX, [VALIDATION](VALIDATION.md), [REPORT](REPORT.md), [CLOSURE](CLOSURE.md); TD-008 cerrado y TD-043 preservado | PASS |
| Higiene de evidencia | RUN_REQUIRED | cero secretos copiados; skips revisados; cambios de cierre solo documentales | PASS |
| Cierre | RUN_REQUIRED | gates locales + SQL + CI remoto + revisión humana satisfechos | PASS |

## Evidencia previa que no se repite

| Evidencia | Resultado reutilizado | Límite |
|---|---|---|
| LB-001 Contract First | contrato OpenAPI/HTTP/SSE congelado y conformance previa PASS | el gate OpenAPI actual vuelve a correr dentro de `verify` |
| LB-002.2C | 1426 tests; LINE 90.63 %; BRANCH 81.17 %; ArchUnit/OpenAPI PASS | cifra histórica, no sustituye la corrida LB-003 |
| LB-002.2D | targeted SQL Server 59/59, 0 F/E/S; paridad 0 mismatches; write/read/realtime | se reconfirma una vez sobre la DB actual |
| LB-002.2E / MV-006 | runtime JPA + frontend + SQL real; POST/readback; dos clientes SSE; reconexión ~25 s; HTTP fuente de verdad | PASS reportado externo; no hay artefacto/log local y no se repite manualmente |
| MV-001 | E2E frontend/Keycloak/SQL/SSE PASS reportado externo | sin artefacto reproducible local |
| TD-043 | `OPEN / DEFERRED / NON-GOLDEN`; perfil global `NOT_GREEN_TD043` | no se modifica para LB-003 |

## Gates transversales al cierre

| Gate | Estado final | Regla de cierre |
|---|---|---|
| TD-008 | CLOSED — LB-003 | targeted actual 59/59, SQL Server 16.0, fixtures, cero skips y MV-006 trazables |
| Auditoría / TD-010 | `NOT_OBSERVABLE` en integración Golden Path; 10/10 tests unitarios de interceptor/repositorio PASS | no bloquea LB-003; TD-010 mantiene su alcance de release DB |
| TD-023 / MV-004 | TD-023 `OPEN / PARTIAL`; MV-004 PASS | gobernanza + 4 checks remotos del PR #14 satisfechos para LB-003; permanece pendiente solo CI DB reproducible/versionada para fase de infraestructura posterior |
| TD-043 | OPEN / DEFERRED | fuera del Golden Path; no invalida targeted verde |
