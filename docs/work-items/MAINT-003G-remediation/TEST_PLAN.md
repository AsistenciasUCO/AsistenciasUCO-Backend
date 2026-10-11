# TEST_PLAN — MAINT-003G remediation

| ID | Requirement | Scenario | Observable | Expected | Level |
|---|---|---|---|---|---|
| INT-ENV-01 | Real DB diagnostics are explicit. | Failsafe has configured expected DB name. | `SqlServerConnectionIT`. | Isolated DB name matches. | Integration |
| INT-FIX-01 | Attendance parity has deterministic fixture. | Query group/session/null/multiple rows. | JDBC/JPA field parity. | Zero mismatch. | Integration |
| INT-REV-01 | New review remains pending. | Student submits, non-owner attempts resolution. | persisted state. | `P`, with no unauthorized side effect. | Integration |
| V2-HTTP-01 | Protected v2 has real auth semantics. | no JWT, wrong role, wrong owner, owner. | status and persistence. | 401/403/403/2xx. | HTTP/Keycloak/SQL |
| V2-HTTP-02 | UTC is exact. | POST → GET → PATCH → GET with offset inputs. | SQL and JSON instants. | UTC `Z` exact to 7 digits. | HTTP/Keycloak/SQL |
| V2-UNIT-01 | Parser/controller/read/startup errors are observable. | null, unknown field, malformed offset/range, GET null provenance, startup incompatibility, 405. | HTTP/error response or startup failure. | frozen contract response. | Unit/component |

## RED snapshot

Base commit: `30fcd5e7`; existing failures are recorded in `target/failsafe-reports` with 11 failures/1 error/3 skips. Existing RED assertions will not be weakened. New behavioral RED tests require a separately recorded source hash before implementation.
