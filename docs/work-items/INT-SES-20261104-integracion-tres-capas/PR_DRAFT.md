# PR — docs(integracion): evidencia E2E SES-01/02/04/05 y APP-04 en las tres capas

**Base:** `develop` · **Rama:** `docs/integracion-ses-20261104` · **Commit:** `d62b597c` · Solo documentación, sin cambios de código.

## Qué cambia

Nuevo work item `docs/work-items/INT-SES-20261104-integracion-tres-capas/`:

- `VALIDATION.md` — sincronización de los tres `develop` (BE `1959d431`, FE `f0cfe5d`, DB `11d7d50`), diagnóstico de datos académicos (solo lectura), resultados de `verify` (BE 3144/3144, FE 1504/1504) y E2E en navegador por rol (DOCENTE, ESTUDIANTE, COORDINADOR) con la clasificación final de cada corrección.
- `DB_DEPLOY_PLAN.md` — alineación de `sql_server_asistencias` con develop DB: paquete A (PRs #76–#80, fusionados por el owner DB) **aplicado** con backup `COPY_ONLY`; paquete B (SES-01/04/02, PRs #81–#83 sin revisión del owner DB) **pendiente de su aprobación**, con procedimiento de rollback.
- `scripts/align-live-sql-develop.ps1` — preflight/apply/verify leyendo una revisión git exacta, una sola transacción con `ROLLBACK` ante error, comprobación de codificación UTF-8; rechaza el paquete B sin `-ApprovedBy`. No contiene secretos (lee el `.env` del repo DB en tiempo de ejecución).
- `SES03_PROPUESTA.md` — contrato DB/HTTP propuesto para solicitudes de sesión extraordinaria (no implementado) y las decisiones funcionales pendientes.

## Por qué

Dejar trazable qué se integró, qué se validó de verdad entre las tres capas y qué sigue bloqueado por aprobación o decisión, sin presentar `NOT_RUN` como `PASS`.

## Para el revisor

- **SES-04 aprobar/rechazar y la barrera SQL de SES-01** siguen pendientes del paquete B: requiere la aprobación formal de John Duque (addendum del freeze).
- **PDP del laboratorio**: faltan 8 recursos del `access-catalog.json` (F3, HU112–HU157); `provision_asistencias.py pdp` debe ejecutarlo el responsable. Hasta entonces el DOCENTE recibe 403 en `GET /api/v1/docente/oferta-creacion-grupos`.
- **Registro de prueba creado en la DB dev**: grupo 9105 «E2E-SES02 integracion 20261104» (6 sesiones), conservado como evidencia de SES-02.
- **Hallazgos de datos sin corregir**: 7 asistencias `SJC` en la sesión futura 3 del grupo 001; filas heredadas `EX` con `asistio=1`.
- PR relacionado del Frontend: `shesho22/AsistenciasUCO-Frontend` rama `fix/ses05-ex-no-presencial`.
- MessageUcoLab no se inició.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
