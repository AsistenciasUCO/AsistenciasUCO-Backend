---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-19
---

# LB-004D — D0: análisis contractual del vínculo soporte ↔ reclamo (consumidor backend)

## Identidad y objetivo

- Fecha: 2026-10-19. Base: backend `develop` `e6c4df6` (PR #27); DB `develop` `7d9c050` (PR #37).
- Objetivo de LB-004D: estudiante sube soporte → MinIO + ClamAV → SQL vincula el archivo al reclamo
  → el docente titular consulta y descarga. Esta entrega cubre **solo D0**.
- Skills: `uco-files`, `uco-contratos`, `uco-seguridad`, `uco-persistencia`.

```text
DB_P0D_MERGED: YES (johnjduque/gestion-asistencia-db develop 7d9c050, merge PR #37)
LB004D_CONTRACT_APPROVED: NO (propuesta enviada; aprobación del owner DB pendiente)
REVIEW_BINDING: BLOCKED_BY_DB_CONTRACT (sin cambio)
DOCENTE_FILE_ACCESS: DENY_BY_DEFAULT (sin cambio)
CHANGE_CLASS: CONTRACT_ANALYSIS
DOR (B1/F1): NOT_READY
```

## Propuesta contractual (autoridad: repositorio DB)

La propuesta vive en el repositorio dueño del contrato, rama
`codex/lb-004d-d0-support-binding-contract`, carpeta
`docs/work-items/LB-004D-soporte-revision-binding/`:

- `ADR-LB004D-001.md` — alternativa A: metadata SQL durable desde `DRAFT`.
- `CONTRACT_PROPOSAL.md` — `ArchivoSoporte`, `ArchivoSoporteTransicion`, W1 registrar borrador,
  W2 `usp_radicar_solicitud_revision_asistencia_v2`, R1 lectura autorizada, R2 reclamos del
  docente, W3/R3 reconciliación, preguntas Q1–Q10.
- `INTERACTIONS.md` — secuencias, fallas y compensación.
- `TEST_MATRIX.md` — DB-01…26, BE-01…12, FE-01…07, E2E-01…15.

Responde a los requisitos R1–R3 y W1–W3 de [DB_BINDING_HANDOFF](DB_BINDING_HANDOFF.md) sin
modificar contratos SQL existentes.

## Impacto previsto en backend (B1, no autorizado todavía)

| Pieza | Cambio previsto |
|---|---|
| Application | Un único puerto nuevo `SoporteArchivoMetadataPort` (registrar borrador, consultar autorizado, reconciliación si Q6). Los reclamos del docente usan el puerto de repositorio de asistencia existente. |
| `SubirArchivoUseCaseImpl` | Tras `FileStoragePort.store`, registrar el `DRAFT`; si el rechazo es definitivo → `delete` de compensación; si el resultado es desconocido → reintento idempotente, sin borrar los bytes. |
| `DescargarArchivoUseCaseImpl` | Autorizar con R1 **antes** de `FileStoragePort.read`; 0 filas → 404; verificar SHA-256 contra el valor de SQL. |
| Radicación | `AsistenciaJpaRepository` pasa de v1 a v2 con `soporteArchivoId`; deja de transmitir `soporteNombre/soporteUrl`. |
| `GET /docente/reclamos` | Deja de responder `FeatureUnavailable`; requiere contrato HTTP en OpenAPI antes de implementar (`API_DESIGN_RULES`). |
| Content security | Rechazar nombres sanitizados de más de 250 caracteres (hallazgo H-05). |
| Prohibido | JDBC, bucket u `objectKey` fuera de Infrastructure, acceso por rol administrativo. |

## Hallazgos que afectan al backend

- H-01/H-02: los SP v1 de radicar y resolver no verifican que el estudiante o el docente
  pertenezcan al ejecutor; hoy lo mitiga el backend al derivarlos del JWT. No retirar esa
  derivación.
- H-03: `uv_auth_solicitud_revision_asistencia` expone reclamos a compañeros de grupo y a roles
  administrativos; el backend no debe adoptarla para soportes.
- H-06: `usp_resolver_…` devuelve mensajes de catálogo ajenos al error (VAL_002/VAL_007).

## Siguiente paso

Aprobación de John sobre el ADR y respuestas Q1–Q10 → D1 (DB, RED → GREEN) → B1 → F1 → E2E.
