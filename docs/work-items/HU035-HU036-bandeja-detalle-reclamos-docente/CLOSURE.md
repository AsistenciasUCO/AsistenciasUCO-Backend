---
status: closed
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-21
---

# CLOSURE — HU035 + HU036: Bandeja y detalle de solicitudes de revisión del docente

## Resultado

**DONE funcional / CERTIFICADA en las 4 capas**, con la salvedad del gate global `-Pintegration` local (ver Pendientes). Evidencia completa en [VALIDATION.md](VALIDATION.md).

| Capa | Evidencia |
|---|---|
| 1. DB | `test_summary.ps1` con fixture LB-004D: **DB GATE PASS, 254/0**; test nuevo DB-01..DB-08 en verde; hash RED = GREEN |
| 2. Backend | `mvnw test` **1682/0**; `mvnw verify` JaCoCo **LINE 91,10 % / BRANCH 80,26 %**; ArchUnit verde; `SolicitudRevisionDocenteDetalleSqlIT` **4/4**; hashes RED = GREEN |
| 3. Frontend | `test:ci` 847/846; el único fallo es `DeanInfraestructuraComponent`, ajeno y presente en el baseline; build de producción y gate realtime PASS; hashes RED = GREEN |
| 4. E2E | `scratch/test_hu035_hu036.ps1` **17/17** contra backend, Keycloak y SQL Server reales |

## Alcance entregado y decisiones

- HU035: bandeja paginada y filtrada en servidor, contador de pendientes desde el `total` del servidor, filtro reactivo con Signals y errores propagados (sin enmascarar 501/404).
- HU036: SP `usp_consultar_detalle_solicitud_revision_docente` (titularidad en el SP, causal `RazonCausa` + `asistio`, soporte parametrizado), `GET /api/v1/docente/reclamos/{id}` y detalle en la UI.
- Decisiones del owner: D1 = B (causal de la inasistencia), D2 = B (SP y endpoint nuevos), D3 = A (parámetro `ARCHIVO.ESTADO_ADJUNTO`); enmiendas E-01 (`400 INVALID_REQUEST`) y E-02 (sin anotaciones OpenAPI; registro en `HTTP_AS_IS_MATRIX.md`); clasificación del SP nuevo como `READ_QUERY_RESULTSET` en `test_contract_closure.sql`.
- Implementación delegada por capa a subagentes (Sonnet), con auditoría del agente principal antes de cada capa siguiente.

## Deuda

- [TD-059](../../baseline/TECHNICAL_DEBT.md#td-059): `DELETE` sobre `SolicitudRevisionAsistencia` falla con Msg 8624 (FK + índice filtrado de LB-004D). **OPEN / HIGH.**
- [TD-060](../../baseline/TECHNICAL_DEBT.md#td-060): la categoría de radicación no se persiste. **OPEN / MEDIUM.**
- [TD-061](../../baseline/TECHNICAL_DEBT.md#td-061): literales `'ATTACHED'`/`'DRAFT'` en SP de LB-004D. **OPEN / LOW.**
- Inconsistencia documental Constitución (`ApiPageResponse`) vs norma backend (`ApiListResponse`): sin cambio en esta HU.

## Validación manual

Ninguna MV nueva. La validación visual en navegador queda pendiente (NOT_RUN).

## ADR relacionados

Ninguno nuevo. Aplican ADR-003 (JPA-only) y el contrato UTC v2.

## Elementos pendientes y bloqueos

- `verify -Pintegration` completo: 235 ejecutadas, 19 fallos y 9 errores, todos de entorno y no atribuibles a esta HU (triage en VALIDATION): MinIO sin usuario técnico provisionado y fixture MAINT-003G bloqueado por TD-059. Volver a correrlo cuando se provisione MinIO y se resuelva TD-059.
- Recursos Docker creados por error (`asistencias-files-backend`, `asistencias-minio-data`, `asistencias-files-minio-init-1`): pendiente de limpieza manual del owner.
- Commits y PR por capa: pendientes de aprobación del owner.

## Cambios fuera de alcance

- Detectados en el árbol y **excluidos** de esta HU: `SecurityConfig.java` (GET `/admin/sedes` y `/admin/areas`) y `dean-faculty`/`dean-infraestructura` en el frontend, de otro trabajo en curso.
- Sesión aparte "Contador de reclamos pendientes en overview": corrige el mismo defecto que AC-04 en `overview.component.ts`. Sin commit; entregar después de esta HU.

## Condición de parada

No se inicia automáticamente otra HU.
