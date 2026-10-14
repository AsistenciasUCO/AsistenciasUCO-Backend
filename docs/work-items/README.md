---
status: active
type: active
scope: backend
owner: backend-team
last-reviewed: 2026-09-20
---

# Work items versionables

Convención: `LB-XXX-descripcion/`, con `PLAN.md`, `TEST_PLAN.md`, `VALIDATION.md`, `CLOSURE.md`. Se puede descomponer una fase en tareas acotadas dentro de su plan, conservando ID de fase y trazabilidad. No crear carpetas vacías.

- Copiar las [plantillas](../../.claude/templates/PLAN.md) y resolver campos con evidencia o NO APLICA justificado. Todo PLAN declara clase de cambio, rutas permitidas/prohibidas y variable principal. Para comparar contratos entre repositorios usar [CONTRACT_MATRIX](../../.claude/templates/CONTRACT_MATRIX.md).
- PLAN guarda AS-IS/TARGET, alcance, fuentes, consumidores, riesgos, rollback y resultado de [DoR](../governance/DEFINITION_OF_READY.md).
- TEST_PLAN fija conducta esperada y RED antes de implementación; registra archivos/hash o commit de los tests aprobados mediante la sección `RED_SNAPSHOT` ([estándar](../testing/TESTING_STANDARD.md#red_snapshot)).
- VALIDATION conserva comandos, resultados, cobertura, skips, dictamen y bloqueos. No versionar logs crudos/secretos.
- CLOSURE vincula [DoD](../baseline/DEFINITION_OF_DONE.md), deuda y manuales; no marca DONE si falla un gate aplicable.

`.workspace/plans`/`.workspace/validation` no son la fuente oficial. Si aparecen, trasladar solo evidencia útil y sanitizada; no copiar estado local ni worktrees.

Los work items activos se determinan desde [LINEA_BASE.md](../baseline/LINEA_BASE.md). La carpeta conserva trabajo activo y cerrado.

Ejemplo de work item técnico cerrado: [TECH-001](TECH-001-restaurar-gate-arquitectura/CLOSURE.md).

## Work item transversal quality
[QUALITY-PR15-recovery](QUALITY-PR15-recovery/PLAN.md) documenta recuperación de checks del PR #15 y no inaugura una baseline funcional. Q0 documental, Q1 diagnóstico, Q2/Q3 implementación con DoR+RED independientes, Q4 certificación remota; LB-004 sigue separada.

## Revisión y reanudación — octubre 2026
- [Auditoría independiente PR #15](QUALITY-PR15-recovery/INDEPENDENT_REVIEW_2026-10-08.md): evidencia remota, riesgos y checklist humano de merge.
- [Plan LB-004 después de PR #15](LB-004-stateless-serverless-readiness/RESUMPTION_CONTRACT_PLAN_2026-10-08.md): análisis contractual DB antes de backend/frontend. Sin implementación autorizada todavía.

## Correcciones de defectos — octubre 2026
- [FIX-001](FIX-001-sse-error-content-negotiation/PLAN.md): errores de `/api/v1/realtime/stream` con `Accept: text/event-stream` respondían 500 (403 esperado) o 400 sin cuerpo; ahora el `ApiErrorResponse` se escribe siempre en JSON. Hallado en el E2E SEC-BASELINE-002.
