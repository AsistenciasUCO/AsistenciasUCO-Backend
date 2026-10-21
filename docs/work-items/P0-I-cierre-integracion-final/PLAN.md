# PLAN — P0-I: cierre de integración (reclamos duplicados, UTC, fixture, CodeQL, develop)

## Identidad
- Fecha / rol / base: 2026-10-20 / estabilización final / backend `jose-valencia/p0-h-backend-functional-integration` (`02338e0`), frontend `6bf5787`, DB `b698c76`.
- Objetivo: corregir causas de reclamos repetidos, comprobar UTC del portal, aislar el fixture de sesiones, resolver CodeQL y dejar los PR listos hacia `develop`. No rediseña ni reemplaza lo validado manualmente el 2026-10-20.
- Change class: BUG_FIX + TEST_INFRA + CONTRACT_ANALYSIS. PRIMARY_VARIABLE: `IDEMPOTENT_CLAIM_SUBMISSION`.
- ALLOWED: frontend (radicación/UTC), backend `SecurityErrorResponseWriter` y tests, scripts `scripts/e2e/files/**`, docs. FORBIDDEN: DDL/constraints en DB sin owner DB, borrado de reclamos históricos, `master`, force-push, Redis/`@Cacheable`.
- Rollback: revertir los commits P0-I; el fixture de sesiones se elimina con `fixture_cleanup()` (idempotente).

## Diagnóstico de reclamos repetidos (evidencia, no hipótesis)
| Hecho | Fuente |
|---|---|
| Hay 7 `SolicitudRevisionAsistencia`: 3 manuales (asistencias distintas, 1 por asistencia) y 4 con `E2E-LB004D-<run>`. | consulta SQL de solo lectura 2026-10-20 |
| Los 4 E2E provienen de 2 corridas de `support_binding_e2e.py main` (844462d5d7, 7cadc72ae5), cada una crea `-con-soporte` y `-sin-soporte` sobre la misma asistencia y **no limpiaba** al terminar. | `AuditoriaEvento`: 202 a 16:57:43/45 y 16:57:55/57 (-05:00), actor E2E |
| Las radicaciones manuales fueron 3 POST 202 separados (14:57, 17:50:29, 17:51:41) sobre asistencias distintas. Ninguna ráfaga de POST idénticos. | `AuditoriaEvento` |
| No existe doble envío por UI en los datos observados. Aun así, el cliente no reutilizaba la correlación en reintentos. | código frontend |

Conclusión: causa raíz = **acumulación de datos de pruebas E2E sin limpieza** (no un doble clic). Riesgos reales cerrados: (1) el frontend generaba un `X-Correlation-Id` nuevo por intento; tras una respuesta perdida, el reintento con soporte recibía 404 (soporte ya `ATTACHED`) y el usuario re‑adjuntaba y duplicaba; (2) el botón no mostraba estado de carga.

## Cambios
- Frontend: `RadicacionIntento` + `huellaRadicacion` (misma huella = misma correlación; éxito o contenido nuevo = correlación nueva), `crearReclamo(solicitud, intentoId)` envía `X-Correlation-Id`, `[loading]` en el botón, refresco de sesiones tras resultado incierto (red/408/5xx). Fecha de adjunción y fecha de asignación usan el día civil de la zona seleccionada (no `toISOString()` UTC).
- Backend: `SecurityErrorResponseWriter` serializa con Jackson (`JsonMapper.writeValue(OutputStream)`) en lugar de armar JSON a mano; 10 tests incluyendo entradas maliciosas.
- E2E: `support_binding_e2e.py` limpia por defecto filas y objetos MinIO de su marker (`--keep` para el flujo de reinicio), nueva fase `idempotency`; `student_sessions_e2e.py` crea y elimina su fixture aislado `E2E-FIX-A/B`.
- DB: propuesta D1.1 (`DB_D1_1_IDEMPOTENCY_PROPOSAL.md`) sin aplicar.
