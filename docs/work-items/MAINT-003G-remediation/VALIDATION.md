---
status: active
type: validation
scope: backend
owner: backend-team
last-reviewed: 2026-10-10
---

# VALIDATION — MAINT-003G remediation

## Executed evidence

| Check | Environment / command | Result |
|---|---|---|
| Targeted Java parity | Java 25, `-Pintegration -Dit.test=AsistenciaQueryJpaParityIT,AsistenciaCommandsSpParityIT failsafe:integration-test failsafe:verify` against `utc_d06_sqltest` | `17` tests, `0` failures, `0` errors, `0` skips; exit `0`. |
| Complete Java integration | Java 25, `-Pintegration failsafe:integration-test failsafe:verify`, explicit `APP_DATABASE_EXPECTED_NAME=gestionasistenciadb` | Aggregated Surefire XML: `191` tests, `3` failures, `0` errors, `3` skips. The prior `1` configuration error and eight remediable failures are cleared. |
| HTTP v2 / real identity provider | Local backend on an isolated DB, local Keycloak realm, temporary users removed afterward | `401` without JWT; `403` for non-DOCENTE; `403` for non-owner DOCENTE; POST→GET→PATCH→GET preserves `2042-07-15T14:00:00.1234567Z` and normalized `2042-07-15T15:30:00.7654321Z`. |
| UTC startup guard | Same isolated backend, `app.sesiones.v2.enabled=true` | Startup logged the UTC-D06 schema compatibility verification and enabled `/api/v2/sesiones`. |
| Added v2 unit coverage | Java 25, `-Dtest=SesionV2RequestParserTest,SesionV2ConsultaControllerTest test` | `4` tests, `0` failures, `0` errors, `0` skips; parser normalizes exact UTC and rejects invalid/unknown fields, while GET response retains confirmed vs indeterminate provenance. |
| Full Java + JaCoCo | Java 25, `-Pintegration verify` against `utc_d06_sqltest` | Surefire `1495/1495` pass; JaCoCo line `8194/(8194+804)=91.06%`, branch `1899/(1899+512)=78.77%`, both above configured gates. Failsafe remains blocked only by `CC-003G-01` (`191`, `3` failures, `0` errors, `3` skips). |

## Remaining non-pass results

`CC-003G-01` remains **CONTRACT_CONFLICT / BLOCKED**. The only three Failsafe failures are:

1. `SqlStoredProcedureContractIT`: public `dbo.usp_sincronizar_usuario` is absent.
2. `SqlStoredProcedureContractIT`: public `dbo.usp_registrar_o_actualizar_plan_estudio` is absent.
3. `UsuarioPasswordHashSqlServerIT`: depends on the first absent public provider.

The three explicit skips are preserved and reported, not treated as PASS:

- two `DocenteRepositorySqlServerIT` fixture-shape scenarios;
- `AcademicQueryJpaParityIT.horarios_conservan_horas_locales_y_nulos`.

No backend fallback, internal-procedure substitution, v1 removal, primary DB change, deployment, merge, or production credential was used. `BACKEND_SQL_INTEGRATION_PASS` cannot be asserted until the owners resolve `CC-003G-01`. The SQL least-privilege and real JWT HTTP evidence support `BACKEND_SECURITY_PASS` for the covered UTC-v2 scope.

## Unavailable local analyzers

`sonar-scanner`, `codeql`, and `trivy` are not installed in this environment; each is `NOT_RUN`, not PASS. Their remote/CI executions remain required before PR approval.
