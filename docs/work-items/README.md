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
- [LB-004C MinIO + ClamAV](LB-004-stateless-serverless-readiness/LB-004C-EXECUTION_REPORT.md): reanudación autorizada 2026-10-18 de la storage foundation independiente de DB; handoffs [DB](LB-004-stateless-serverless-readiness/DB_BINDING_HANDOFF.md) y [frontend](LB-004-stateless-serverless-readiness/FRONTEND_FILES_HANDOFF.md).

## Correcciones de defectos — octubre 2026
- [FIX-002 — reporte Excel de asistencia](FIX-002-reporte-asistencia-titularidad/PLAN.md): trabajo activo para validar rol y scope institucional antes de consultar o generar el XLSX; no altera OpenAPI, SQL ni el formato del reporte.
- [FIX-001](FIX-001-sse-error-content-negotiation/PLAN.md): errores de `/api/v1/realtime/stream` con `Accept: text/event-stream` respondían 500 (403 esperado) o 400 sin cuerpo; ahora el `ApiErrorResponse` se escribe siempre en JSON. Hallado en el E2E SEC-BASELINE-002.
## P0-B2 — implementación y estabilización — 2026-10-20
- [Higiene de logs](P0-B2-log-hygiene/PLAN.md): runner local corregido, rotación y retención aisladas; arranque real pendiente de servicios externos.
- [Consolidación local](P0-B2-local-consolidation/PLAN.md): matriz de copias preservada; retiro de carpetas requiere autorización explícita.

## P0-D — consolidación física — 2026-10-20
- [Consolidación física](P0-D-consolidacion-fisica/PLAN.md): worktrees limpios retirados con Git, refs recuperadas y clones independientes trasladados a cuarentena reversible; el borrado definitivo sigue pendiente de aprobación.

## P0-E — consolidación funcional local — 2026-10-20
- [Consolidación funcional](P0-E-consolidacion-funcional/PLAN.md): checkouts oficiales y proveedores locales validados; la integración cross-repo permanece abierta por autoridad divergente y las tres SQL de paginación quedan preservadas sin integrar.

- [P0-B](P0-B-unificacion-repositorios/PLAN.md): remotos canónicos de organización, sincronización Git segura y auditoría diferencial multirrepositorio; cierre funcional bloqueado por Infra/gates externos no ejecutados.

## HU de coordinación y decanatura — paquetes 1 a 5 — 2026-10-25/26
- [HU081-HU083](HU081-HU082-HU083-plan-estudio-estado-semestres/CLOSURE.md): estado y semestres del plan de estudio. DONE (MV-007 PASS).
- [HU096-HU098](HU096-HU097-HU098-periodos-academicos/CLOSURE.md): períodos académicos solo por el Administrador. En `develop`; PENDIENTE de MV-008.
- [HU064/HU060/HU061](HU064-HU060-HU061-consultas-coordinador/CLOSURE.md): consultas del coordinador sobre su programa. En `develop`; PENDIENTE de MV-008.
- [HU104/HU105/HU109/HU119/HU145](HU104-HU105-HU109-HU119-HU145-lecturas-decano/CLOSURE.md): consultas del decano sobre su facultad. En `develop`; PENDIENTE de MV-008.
- [HU108 (planes y malla)](HU108-malla-plan-decano/CLOSURE.md): malla del plan para el decano. En `develop`; PENDIENTE de MV-008.
- [HU010/HU011 (consulta)](HU010-HU011-consulta-reclamos-estudiante/CLOSURE.md): "Mis Reclamos" del estudiante (`GET /api/v1/estudiante/reclamos`). En `develop`; PENDIENTE de MV-008.
- [HU051 (retiro)](HU051-retirar-estudiante-grupo/CLOSURE.md): el docente titular retira a un estudiante de su grupo con motivo y aviso. PR por integrar; PENDIENTE de MV-008.
- [HU006 (perfil propio)](HU006-perfil-usuario/CLOSURE.md): `GET /api/v1/usuarios/perfil` con datos personales de la DB y vínculos por rol; sección "Expediente y vínculos" en "Mi perfil". El PUT sigue en 501. PR hacia `develop`; PENDIENTE de MV-008.
- [HU094/HU095 (directorio docente del coordinador)](HU094-HU095-directorio-docentes-coordinador/CLOSURE.md): ficha del docente con historial de grupos y resumen de carga; contadores de activos, inactivos y por tipo de vinculación con filtro. PR #51 hacia `develop`; PENDIENTE de MV-008.
- [HU008 (oferta de grupos del estudiante)](HU008-oferta-grupos-estudiante/CLOSURE.md): especificas de sus programas y transversales de su institución con cupos y "Solicitar ingreso"; regla: solo una ESP inscribe en el programa. Commits locales; PENDIENTE de MV-008.
- [HU053/HU054/HU055 (solicitudes de ingreso)](HU053-HU054-HU055-solicitudes-ingreso-grupo/CLOSURE.md): el estudiante solicita, el docente acepta (con cupo) o deja en espera / rechaza con motivo, el estudiante retira. PR por integrar; PENDIENTE de MV-008.
- [HU112/HU128/HU143/HU157 (consultas del decano y del administrador)](HU112-HU128-HU143-HU157-consultas-transversales/PLAN.md): tabla de programas y créditos, docentes de la facultad por programa, búsqueda de asignaturas (decano: su facultad; administrador: su institución) y ficha unificada del estudiante con historial. Rama local `feature/HU112-HU128-HU143-HU157-consultas-transversales`; PENDIENTE de reaprovisionar el PDP con 7 rutas y de MV-012.
