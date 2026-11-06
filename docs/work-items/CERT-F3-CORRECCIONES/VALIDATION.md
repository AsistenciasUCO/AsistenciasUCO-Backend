---
status: active
type: work-item
scope: database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-11-06
---

# VALIDATION — CERT-F3-CORRECCIONES

Corrida final (2026-11-06, tarde), con todas las correcciones y las 5 decisiones de John:

| Capa | Comando | Resultado |
|---|---|---|
| BD | `deploy_schema.ps1` + `test/prepare_lb004d_fixture.ps1` + `test_summary.ps1` en un contenedor SQL Server 2022 limpio (mismo orden que el CI) | `TOTAL_EXECUTED=442`, `PASSED=441`, `FAILED=0`, `SKIPPED=1` (permitido), `CRITICAL_MISSING=0`, `DB GATE PASS` |
| Backend | `.\mvnw.cmd verify` | `BUILD SUCCESS`: 3139 tests, 0 fallos, 0 errores; "All coverage checks have been met" |
| Frontend | `npm run verify` | 1534 de 1534 SUCCESS; statements 91.8 %, lines 92.61 %; realtime line 95.06 %, branch 84.52 %; build de producción OK |
| PDP del lab | `provision_asistencias.py verify-decisions` | 973 decisiones, 0 fallos, `clean: true` |

Re-prueba en pantalla (BD local, lab Security Baseline):
- PASAN: HU145, la estructura y Programas y Créditos con 8 semestres, la tarjeta del coordinador con facultad y conteos, el plan del coordinador con 3 créditos y 1 asignatura, y Grupos Académicos sin "Aula".
- HU159 del decano queda PARCIAL por entorno: el realm del lab no declara `idUsuario`, y se corrige con el commit `67a006c3` de develop. El detalle está en el reporte de certificación.

### Corrida anterior (2026-11-06, madrugada)

| Capa | Comando | Resultado |
|---|---|---|
| BD | Gate completo en un contenedor limpio | `TOTAL_EXECUTED=442`, `PASSED=441`, `FAILED=0`, `CRITICAL_MISSING=0`, `DB GATE PASS` |
| Backend | `.\mvnw.cmd verify` | `BUILD SUCCESS`: 3131 tests, 0 fallos, 0 errores; "All coverage checks have been met" |
| Frontend | `npm run verify` | `TOTAL: 1527 SUCCESS`; statements 91.85 %, lines 92.65 %; realtime line 95.06 %, branch 84.52 %; build de producción exit 0 |

Re-prueba en pantalla (BD local, lab Security Baseline): HU136, HU051, HU053, HU054, HU055, HU006 y HU008 pasan. El detalle está en el reporte de certificación.
