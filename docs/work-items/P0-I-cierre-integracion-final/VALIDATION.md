# VALIDATION — P0-I (2026-10-20)

## Dictamen
`LOCAL_PASS` para unit/integración de backend y frontend. `NOT_RUN` para los E2E HTTP de estudiante (identidades `files.e2e.estudiante1/2` con `invalid_grant`). `BLOCKED`: merge a `develop` por D1 sin aprobación del owner DB; CI/Sonar remotos sin ejecutar sobre los SHA finales.

| Comando / evidencia | Resultado |
|---|---|
| `.\mvnw.cmd -B -ntp verify` (backend) | exit 0, 1651 tests, 0 fallos/omitidos, gates JaCoCo cumplidos |
| `npm run verify` (frontend) | exit 0, 819 tests SUCCESS, cobertura realtime y build de producción OK (líneas 89,05 %, ramas 70,8 %) |
| `SecurityErrorResponseWriterTest` + suites de seguridad | PASS (incluidos en el verify) |
| Fixture `E2E-FIX-A/B` (`fixture_apply`/`fixture_cleanup`) en SQL local | PASS: idempotente, `SES-SQL` PASS; conteos Sesion/Asistencia/Detalle/Solicitud/Archivo 16/6354/6354/7/7 antes y después |
| `support_binding_e2e.py idempotency`, `student_sessions_e2e.py` por HTTP | NOT_RUN (credenciales estudiante); se ejecutan tras realinear identidades |
| Estado remoto PR #31 (SHA `02338e0`) | CodeQL: failure; Backend Quality Gate: success pero **sin Sonar** (solo corre con base `develop`); sin análisis Sonar del PR #31 |

## Bloqueos
- `CONTRACT_CONFLICT P0H-DB-001` / D1 sin aprobación del owner DB (ADR-LB004D-001 `proposed`). Bloquea el merge de DB, backend y frontend dependientes. D1.1 propuesta en `DB_D1_1_IDEMPOTENCY_PROPOSAL.md`.
- Sin `gh`/sesión GitHub: no se retargeteó el PR #31 ni se abrieron los PR de frontend y DB.
- `UTC_GAP`: horarios recurrentes sin zona institucional contractual; `SolicitudRevisionAsistencia.fecha` usa la zona del servidor SQL.
