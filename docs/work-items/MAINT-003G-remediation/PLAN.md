---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-10
---

# PLAN — MAINT-003G remediation

## Objective and evidence

Remediate and certify UTC v2 only where the contract is proven. Source backend is `30fcd5e7`; independent audit is `3b633e7`. Failsafe evidence is 191 tests: 11 failures, 1 error and 3 skips. Surefire is 1492/1492 pass.

## Scope

- Change class: `BEHAVIOR_CHANGE` plus `INFRASTRUCTURE` test support.
- PRIMARY_VARIABLE: `BACKEND_SQL_INTEGRATION_CERTIFICATION`.
- ALLOWED_PATHS: UTC v2 controller/application/infrastructure code and tests, integration test support/configuration, and this work item evidence.
- FORBIDDEN_PATHS: v1 endpoint removal or semantic change, OpenAPI contract changes, Domain/Application infrastructure dependencies, production secrets/configuration and database schema from this repository.
- Required: Java 25, real isolated SQL Server, real Keycloak for HTTP certification, existing RED tests preserved.

## Findings and disposition

| Finding | Disposition |
|---|---|
| `APP_DATABASE_EXPECTED_NAME` absent | Integration invocation/configuration defect; provide the explicit expected isolated database value without relaxing the diagnostic. |
| Six `AsistenciaQueryJpaParityIT` failures | Fixture defect: the isolated DB has zero attendance rows. Seed deterministic data in the DB test environment, not the primary DB. |
| RAD/RES state assertions | Fixture/state isolation investigation required; preserve assertions and correct setup only if the DB contract proves `P` for a new request. |
| Two absent public providers and dependent hash failure | `CC-003G-01` contract conflict with the DB owner; no backend fallback or invented SP. |
| Runtime grant | DB-side corrective scope; backend must later prove its actual runtime principal. |

## Ready and stop conditions

**READY (scoped)** for test environment configuration, deterministic fixtures, UTC v2 tests, and DB least-privilege certification. **NOT_READY** for changing either absent provider or replacing it with an internal procedure. Stop on `CC-003G-01`, a frozen HTTP/DB contract contradiction, or unavailable real Keycloak; record `NOT_RUN`, never PASS.
