---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---

# MAINT-003 — VALIDATION

## GitHub Actions sobre fuente `c42129e504394f73768d32dd85f49473e248af3c`

| Workflow | Run | Resultado |
|---|---|---|
| Backend CI (Java25/Maven, JaCoCo, Sonar, Docker) | https://github.com/alejovh12/AsistenciasUCO/actions/runs/37903985248 | SUCCESS |
| Backend Security (CodeQL, Dependency Review) | https://github.com/alejovh12/AsistenciasUCO/actions/runs/37903985263 | SUCCESS |
| Backend Deep Security Scan (Trivy FS/Imagen, ArchUnit) | https://github.com/alejovh12/AsistenciasUCO/actions/runs/37903985240 | SUCCESS de jobs |

Backend CI job log: `HttpUtcInstantCodecTest` **7 tests, 0 failures, 0 errors, 0 skipped**. Total unit `mvn clean verify` **1453 tests, 0 failures, 0 errors, 0 skipped**, `BUILD SUCCESS`; SonarCloud Quality Gate SUCCESS. Importante: este conteo corresponde a la suite Surefire de la rama de PR20; no demuestra que se ejecutaron `-Pintegration` con SQL Server real.

## Límites de certificación

- `HttpUtcInstantCodec` es una clase **sin consumidores**, probada de manera independiente. HTTP v1 sigue devolviendo `LocalDateTime` sin offset.
- **No existe API v2 funcional en este PR.** Toda API v2 continúa CONTRACT_DECISION_REQUIRED. No publicar fechas `Z` para v1 sin migración aprobada.
- SQL Server `CoreViewQueriesJpaParityIT` sobre fixture >=11/multigrupo: NOT_RUN desde este PR. PR #18 tiene CI verde, pero esa integración no ha sido acreditada.
- Catálogo `CatalogJpaParityIT` contra SQL Server real: NOT_RUN desde este PR. PR #19 tiene CI verde, pero no acredita DB real.
- Auditoría de historiales `dbo.Sesion`: NOT_RUN; semántica del histórico por confirmar.
- Roundtrip sesión POST/GET/PATCH/GET SQL UTC, roles reales, navegador Berlin/London/Bogota: NOT_RUN.
- Migraciones y despliegue real: NO MODIFICADOS; pruebas E2E de UI pertenecen a fase frontend posterior.

**Resultado actual:** `PREPARATION_PASS / BACKEND_READY_FOR_FRONTEND = NO`. Solo firmar la entrega tras contrato UTC, tests SQL reales, autorización de owner/usuario y regresión integral.

## Evidencia adicional

Comandos de SQL y Maven, plan de pruebas y prompt de ejecución manual se encuentran en `PLAN.md`, `TEST_PLAN.md`, `BACKEND_TO_FRONTEND_HANDOFF.md` y `PROMPT_CODEX_BACKEND.md`. Nunca reportar `PASS` por ausencia de fallos en tests que no se ejecutaron.

## Ejecución independiente posterior — 2026-10-09

Esta sección reemplaza los `NOT_RUN` anteriores únicamente donde existe evidencia nueva. El detalle íntegro, SHAs, contrato y pendientes está en [BACKEND_READY_FOR_FRONTEND.md](BACKEND_READY_FOR_FRONTEND.md).

| Alcance | Resultado comprobado |
|---|---|
| PR18 unit | Java 25, 1452 tests, 0 failures/errors/skips, JaCoCo 92.53 % line / 80.97 % branch |
| PR18 SQL real | 192 IT, 0 failures/errors/skips; `CoreViewQueriesJpaParityIT` 7/0/0/0 y filtro académico 3/0/0/0 |
| Paginación directa/HTTP | clone 11 identidades/12 contextos/1 multigrupo; páginas 5/5/1, 11 distintos, 3 páginas; nueve filtros 200 con valores válidos; límites inválidos 400 |
| JWT real | Keycloak validado; anónimo 401, DOCENTE 403, ADMINISTRADOR 200. COORDINADOR/ESTUDIANTE reales NOT_RUN |
| PR19 unit | Java 25, 1447 tests, 0 failures/errors/skips, JaCoCo 92.50 % line / 80.70 % branch |
| PR19 SQL real | 191 IT, 0 failures/errors/skips; `CatalogJpaParityIT` 11/0/0/0; fixtures residuales 0 |
| PR20 unit | 1453 tests, codec 7, 0 failures/errors/skips, JaCoCo 90.26 % line / 80.25 % branch |
| Históricos | 3 sesiones caracterizadas; `HISTORICAL_TZ_UNDETERMINED`, sin escritura ni conversión |
| UTC HTTP/SQL | v2 no aprobada/no implementada: POST/GET/PATCH, SQL UTC y DST end-to-end continúan NOT_RUN |
| Calidad remota | 9/9 check-runs SUCCESS para cada SHA final de PR18, PR19 y PR20; CLI locales Sonar/CodeQL/Trivy no instalados |

Backup `COPY_ONLY` con checksum fue verificado antes de restaurar `gestionasistenciadb_fase1`. La base fuente quedó en 3 identidades, 3 contextos, 24 parámetros y 3 sesiones antes/después. No se ejecutó merge, push, DDL ni migración.

### Ledger de comandos y exit codes

Fecha de ejecución: 2026-10-09, zona `America/Bogota`. Los valores de conexión y tokens se inyectaron desde variables locales y no se imprimieron.

| Worktree/entorno | Comando o acción equivalente | Exit | Observación |
|---|---|---:|---|
| PR18 `39e34c49` | `.\mvnw.cmd -B -ntp clean verify` | 0 | 1452 unit, Java 25 |
| PR18 `39e34c49` + clone SQL | `.\mvnw.cmd -B -ntp -Pintegration verify` | 0 | 192 IT, 0 skips |
| PR19 `3985b0ac` | `.\mvnw.cmd -B -ntp clean verify` | 0 | 1447 unit, Java 25 |
| PR19 `3985b0ac` + clone SQL | `.\mvnw.cmd -B -ntp clean -Pintegration verify` | 0 | 191 IT, 0 skips; corrida válida fuera del sandbox |
| PR20 `c83442d5` | `.\mvnw.cmd -B -ntp clean verify` | 0 | 1453 unit; codec 7 |
| SQL Server clone | `pagination_readonly_diagnostics.sql` | 0 | páginas, nueve filtros y multigrupo, solo lectura |
| SQL Server clone | `utc_schema_probe.sql` + `utc_hypothesis_diagnostics.sql` | 0 final | primer borrador de hipótesis falló al compilar (Msg 130); corregido antes de obtener evidencia |
| SQL Server source + clone | `post_test_invariants.sql` | 0 | source intacta; cleanup catálogo 0 residuales |
| Keycloak local | `validate-keycloak.ps1` | 0 final | primer intento sandbox no tuvo socket; repetición autorizada: 0 fallos críticos, warnings legacy |
| Keycloak local | `seed-e2e-users.ps1` | 0 | tokens de ejemplo válidos; password no modificada por política local |
| Backend PR18 + clone + Keycloak | requests HTTP locales sanitizados | 0 | 401/403/200, páginas, filtros y 400 inválidos |
| Diffs | `git diff --check` | PR18 0; PR19 0; PR20 1 | PR20: whitespace final en skill, no fallo runtime |

Intentos PR19 con exit 1 antes de tests: ACL de `target` en worktree gestionado y pipe de attach de Mockito bloqueado por sandbox. No se usaron como evidencia; la repetición limpia posterior es la reportada.

**Resultado actualizado:** `PAGINATION_AND_CACHE_REAL_SQL_PASS / UTC_CONTRACT_BLOCKED / BACKEND_READY_FOR_FRONTEND = NO`.

## Segunda pasada — 2026-10-09 (Claude Code)

| Worktree / SHA | Comando o acción | Exit | Resultado |
|---|---|---:|---|
| clon + fuente | `post_test_invariants.sql`, `pagination_readonly_diagnostics.sql` (solo lectura) | 0 | clon 11/12/1 multigrupo, páginas 5/5/1; fuente 3/3/24/3 intacta antes y después |
| PR18 `3b1ddb4` | focales `-Pintegration` (2 IT + 2 unit) | 0 | IT 10/0/0/0, unit 13/0/0/0 |
| PR18 `3b1ddb4` | IT de tres páginas contra la fuente | 1 esperado | falla por `>=11` requerido: no pasa en vacío |
| PR18 `3b1ddb4` | `clean verify` / `-Pintegration verify` | 0 / 0 | 1452/0/0/0; 192 IT/0/0/0 |
| PR18 `d0468e3` | `clean verify` / `-Pintegration verify` | 0 / 0 | 1458/0/0/0; 192 IT/0/0/0; JaCoCo combinado 92.53 % / 80.97 % |
| Keycloak local | `seed-e2e-users.ps1` (4 roles) | 0 | tokens verificados por rol, idUsuario y audience |
| backend jar `3b1ddb4` + clon | matriz HTTP JWT real | 0 | 42 PASS / 0 FAIL |
| PR18 OpenAPI | RED `e7803ca` → GREEN `d0468e3` | 1 → 0 | 7 fallos esperados → 23/0/0/0 |
| PR20 `b512bb4` | guarda + sesión + codec | 0 | 23/0/0/0; control negativo (controller `/api/v2` sembrado) → falla, retirado |
| RED `e85feff` | tests UTC v2 | 1 esperado | 11 fallos por ausencia de v2, 0 errores; v1 congelado y guarda GREEN |
| owner DB `f2871a9` + fuente | lectura de SP, semillas, `CatalogoParametro`, `AuditoriaEvento` | 0 | insumos de UTC-D06; sin escrituras |

Incidencias honestas: un borrador de la matriz HTTP falló por una variable PowerShell que pisaba la URL base y otro por usar `nombre=Estudiante01` (inválido por diseño); el primer borrador de la guarda esperaba `VALIDATION_ERROR` y el AS-IS real es `ERR_FECHA_HORA_INVALIDA`. Todos se corrigieron antes de registrar resultados; ninguno se cuenta como PASS.

CI remoto al cerrar esta pasada: en `9ed690f` y `b512bb4`, Dependency Review y Trivy repo `success`; Backend Quality Gate, ArchUnit, CodeQL y Trivy image `in_progress`. No se declara PASS remoto hasta su conclusión.

**Resultado:** `PAGINATION_CONTRACT_PUBLISHED_AND_JWT_4_ROLES_PASS / UTC_D06_DECISION_REQUIRED / BACKEND_READY_FOR_FRONTEND = NO`.

## MAINT-003C — 2026-10-09 (Claude Code)

Fecha/zona: 2026-10-09, `America/Bogota`. Credenciales leídas de `.env` local sin imprimirse; consultas con `ApplicationIntent=ReadOnly`.

| Entorno / SHA | Comando o acción | Exit | Resultado |
|---|---|---:|---|
| GitHub API | `commits/7e2827d1/check-runs` | 0 | 9/9 `success` (`head_sha` 7e2827d1) |
| GitHub API | `commits/3985b0ac/check-runs`, `commits/b512bb40/check-runs` | 0 | 9/9 `success` cada uno |
| SonarCloud | `qualitygates/project_status?pullRequest=18`, `project_pull_requests/list` | 0 | `OK`; PR18 analizado en `7e2827d1`, PR19 `3985b0ac`, PR20 `b512bb40` |
| git | `diff --stat bfc4fd3..7e2827d1` | 0 | 18 archivos; producción solo validador HTTP + adapter JPA |
| SQL Server fuente | `d06_impact.sql` (columnas, dependencias, triggers, CHECK, CDC, parámetros SP, conteos, `CatalogoParametro TIEMPO`) | 0 | ver acta MAINT-003C; un primer borrador falló por columna inexistente (`DetalleAsistencia.sesion`) antes de producir resultados |
| SQL Server fuente | `d06_defs.sql` (`OBJECT_DEFINITION` de 4 vistas, uso de `fechaHora`/`AT TIME ZONE`) | 0 | vistas con lista explícita; solo `usp_generar_sesiones_grupo` convierte |
| repo owner DB | `git fetch --all`; `git log --since=2026-10-01` | 0 | sin commits nuevos; sin decisión D06 |
| JDK 25 jshell | parseo `ISO_OFFSET_DATE_TIME` de 15 entradas | 0 | acepta `+02`, `+02:00:30`, `-00:00`, sin segundos, 9 decimales, minúsculas |
| RED `19b5a95` | `mvnw test -Dtest=HttpUtcInstantCodec*Test,SesionV2HttpContractRedTest,SesionUtcActivationGuardTest,OpenApiSesionesV2ContractRedTest` | 1 esperado | guarda 3/0/0/0; codec 7/0/0/0; estricto 8 tests/4 RED («nothing was thrown»); HTTP 13/13 RED («ningún @RestController mapea /api/v2/sesiones»); OpenAPI 4/3 RED |
| RED `19b5a95` | `mvnw clean test -Dmaven.test.failure.ignore=true -Djacoco.skip=true` | 0 | 1481 tests, 20 failures = exactamente los RED anteriores, 0 errores, 0 skips |

No se ejecutó: `-Pintegration`, JaCoCo, Sonar/CodeQL/Trivy locales ni HTTP real para MAINT-003C (no hay código de producción nuevo; la rama RED falla por diseño). No se ejecutó DDL, `UPDATE`, merge ni force-push.

**Resultado:** `REVIEW_COMPLETE / OWNER_PROPOSAL_READY / RED_D02_CAUSAL / UTC_V2_NOT_IMPLEMENTED / BACKEND_READY_FOR_FRONTEND = NO`.

## MAINT-003F / UTC-D06 — 2026-10-10

Fecha/zona: 2026-10-10, `America/Bogota`. Los secretos de conexión se inyectaron localmente y no se registran.

| Entorno / SHA | Comando o acción | Exit | Resultado |
|---|---|---:|---|
| backend `2199321871aaa024aae2506d1d807c5a34f7fed3` | Java 25 `mvnw.cmd -B -ntp clean verify` | 0 | 1492 tests, 0 failures/errors/skips; JaCoCo LINE 88.80 %, BRANCH 78.31 %; ArchUnit y gates pasan |
| backend + SQL Server aislado | `mvnw.cmd -B -ntp clean verify -Pintegration` | 1 | 191 tests: 11 failures, 1 error, 3 skips; faltan SP/fixtures y configuración de nombre esperado; no se cuenta como PASS |
| DB `feat/utc-d06-post-freeze` `6946a73` | `test/test_utc_d06_procedencia_temporal.sql` en `utc_d06_sqltest` | 0 | Todos los checks UTC-D06 pasaron: esquema, históricos, v1/v2, ownership, permisos, clasificación, DST, no-escritura inválida y concurrencia |
| DB `feat/utc-d06-post-freeze` `6946a73` | `test_summary.ps1 -ContainerName utc_d06_sqltest` | 0 | 179 ejecutados, 178 PASS, 0 FAIL, 1 skip explícito; 31 SP públicos y 56 vistas presentes |
| DB aislada | inventario post-D06 de `dbo.Sesion` | 0 | 5 filas antes/después, mismos ids y timestamps; 5 `procedenciaTemporal=NULL`; 3 objetos v2 presentes |
| container | `docker inspect` + `rg` backend/DB | 0 | SQL Server 2022, puerto local 14333, sin volumen, restart `no`; no se hallaron referencias permanentes en repositorios |

La prueba HTTP v2 contra un IdP/JWT real queda `NOT_RUN`; los tests JWT unitarios/contractuales sí forman parte del build unitario verde. La integración Java bloqueada y la ausencia de una prueba HTTP real impiden cerrar `BACKEND_READY_FOR_FRONTEND`.

No se ejecutaron merge, despliegue en base principal, activación productiva de v2 ni eliminación de `utc_d06_sqltest`.

## Reconciliación contra develop — 2026-10-10

La evidencia anterior es una bitácora histórica de pruebas ejecutadas el 2026-10-09. Los PR #18, #19 y #20 ya fueron fusionados a develop. Las diferencias de PR #21 se reconciliaron preservando las versiones actuales del código; detalles en [PR21_CONFLICT_RESOLUTION.md](PR21_CONFLICT_RESOLUTION.md). Nuevos checks y `-Pintegration` siguen pendientes de ejecución/verificación sobre el nuevo HEAD.
