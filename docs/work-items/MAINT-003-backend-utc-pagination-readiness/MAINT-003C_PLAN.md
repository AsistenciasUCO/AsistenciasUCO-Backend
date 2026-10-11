---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---

# MAINT-003C — revisión independiente y propuesta UTC-D06/D02 para el owner DB

## Clasificación

| Campo | Valor |
|---|---|
| Clase de cambio | `CONTRACT_ANALYSIS` + `DOCUMENTATION_ONLY` (esta rama) y `TEST_RED_ONLY` (rama RED separada) |
| Variable principal | decisión del owner DB/funcional sobre procedencia temporal de `dbo.Sesion` |
| Autorización | solicitud del usuario (Fase 1 backend, 2026-10-09): revisión independiente, propuesta definitiva para owner DB, RED y análisis de compatibilidad en rama independiente; **sin** cambios de esquema ni activación de UTC v2 sin decisión aprobada |
| DoR implementación UTC v2 | `NOT_READY` — falta firma owner DB/funcional (UTC-D06) y contratos (UTC-D02 detalle) |

## Ramas y separación

| Rama | Base | Contenido permitido | Prohibido |
|---|---|---|---|
| `jose-valencia/maint-003c-utc-d06-owner-proposal` | `codex/fase1-backend-validation` `27e6ed0` | `docs/work-items/MAINT-003-backend-utc-pagination-readiness/**` | `src/**`, `pom.xml`, OpenAPI canónico, SQL ejecutable |
| `jose-valencia/maint-003c-utc-v2-red-hardening` | `jose-valencia/maint-003b-utc-v2-red` `e85feff` | `src/test/**` (RED) | `src/main/**`, OpenAPI, SQL, `pom.xml` |

Commits separados por naturaleza: propuesta DB (texto SQL **no ejecutable**, solo para el owner), propuesta de contrato HTTP de errores, pruebas RED y acta. Ninguna rama abre PR: los workflows solo corren en PR/push a `develop`/`master`.

## Alcance

1. Verificar checks del último commit del PR #18 y la coherencia de la clasificación de alcance de `MAINT-001/PLAN.md`.
2. Auditar UTC-D06 A+C+E y el contrato de errores UTC-D02.
3. Propuesta definitiva para el owner DB: esquema de procedencia, coexistencia v1/v2, clasificación histórica, impacto en SP y vistas, reglas de actualización y rollback.
4. RED derivados de D02 (perfil de wire y contrato de errores).
5. Nueva acta `BACKEND_READY_FOR_FRONTEND`.

## No alcance

DDL, migraciones, `UPDATE` de horas, cambios a SP/vistas, activación de `/api/v2/**`, cambio del OpenAPI canónico, cambio de v1, merge o force-push. La implementación UTC v2 (POST/GET/PATCH reales con SQL Server, JWT, DST) queda para la microfase posterior a la firma.

## Rollback

Documentación: revert de los commits de esta rama. RED: borrar la rama `maint-003c-utc-v2-red-hardening` (sin PR). Base de datos: sin escrituras; nada que revertir.
