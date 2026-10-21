# PLAN — P0-G: recuperación funcional y centralización de asistencias

## Identidad y objetivo

- Fecha / rol / base Git: 2026-10-20 / recuperación y auditoría / backend `befbdbf5f9fca0ff1845216346b8d0d683cc2d9f`.
- Objetivo: preservar y reconciliar la última combinación funcional comprobable entre backend, frontend, DB e infraestructura sin mezclarla con las líneas oficiales hasta cerrar la autoridad contractual.
- Criterio de aceptación: referencias no destructivas, matriz AS-IS/TARGET, validación reproducible y decisión explícita sobre lo que puede o no pasar a integración.
- Skills y fuentes: `uco-baseline`, `uco-arquitectura`, `uco-contratos`, `uco-testing`, `uco-persistencia`, `uco-seguridad`; `AGENTS.md`, `LINEA_BASE.md`, `P0-E`, `DB_BASELINE_CONTRACT.md`, `PROFESSOR_DECISION.md`.

## AS-IS y evidencia

| Componente | Línea oficial/actual | Candidato recuperado | Evidencia y límite |
|---|---|---|---|
| Backend | `befbdbf5` en `codex/p0-sec-001-coordinator-ownership` | `jose-valencia/lb-004d-b1-support-binding` en `f45a5213` | B1 contiene wiring de W1/W2/R1/R2 y tests; no se mezcla en la rama actual. |
| Frontend | `b4368d1` en `jose-valencia/maint-004-frontend-utc-v2-integration` | `95a202a` con F1 en `06f70c4` y fix docente en `fac2f640` | El checkout actual tiene 3 modificaciones locales y no se cambia de rama. |
| DB | `32b70eb` en `origin/develop` | D1/R2 en `b698c76`, basado en `231be58` | Las cuatro SP están presentes en la instancia local; la autoridad/aceptación de DB sigue pendiente. |
| Infra | `a544c496` en `main` | Sin divergencia propuesta | Preflight de puertos y contenedores correcto; `infra/files/.env` no está presente. |
| P0-F | No existe su work item en este checkout | Referencias preservadas en P0-D/P0-E y refs `p0-f-*` | Falta el artefacto original P0-F; no se inventan sus conclusiones ni se eleva su evidencia a PASS actual. |

## TARGET

Obtener una combinación candidateada y reproducible, con backend B1, frontend F1/teacher fix, DB D1 y la infraestructura actual, manteniendo cada línea separada hasta que el owner de DB apruebe el contrato ONE_TABLE y el resultado de las consultas R1/R2. El cierre funcional requiere después una ejecución real autorizada de estudiante → reclamo → soporte → docente.

## Clase de cambio y alcance de rutas

- Change class: `CONTRACT_ANALYSIS` + preservación de referencias; no se autoriza todavía `BEHAVIOR_CHANGE` ni `PERSISTENCE_MIGRATION`.
- PRIMARY_VARIABLE: `FUNCTIONAL_RUNTIME_REPRODUCIBILITY`.
- ALLOWED_PATHS: este directorio de work item; creación de refs locales `jose-valencia/*-functional-recovery`; comandos de lectura, verificación y preflight.
- FORBIDDEN_PATHS: `src/**`, `schema/**`, `database/**`, contratos públicos, OpenAPI, migraciones, borrado/reset de DB, cambio de ramas de checkouts con trabajo local, y cualquier `codex/*` nuevo.
- CONTRACTS: `API_DESIGN_RULES`, `BACKEND_GOLDEN_PATH_CONTRACT`, OpenAPI canónico y `DB_BASELINE_CONTRACT`.
- PROVIDERS: SQL Server, Keycloak, MinIO y ClamAV; mocks no certifican disponibilidad real.
- EXTERNAL_ENVIRONMENT: servicios locales en puertos 1433, 8081, 9000, 3310, 8080 y 4200.
- SECURITY_IMPACT: no se registran secretos; se valida solo presencia/salud y se reportan ausencias por archivo/tipo.
- OBSERVABILITY_IMPACT: ninguno.
- TEST_LEVEL_REQUIRED: reconciliación de refs, unit/integration gates existentes, preflight real y E2E real autorizado antes de declarar cierre.
- ROLLBACK: eliminar solo una ref recién creada si el owner lo solicita; no reescribir ni eliminar refs históricas.
- CONSUMERS: backend B1, frontend F1/teacher, SP D1, Keycloak y servicios de archivos.
- STOP_CONDITIONS: `CONTRACT_CONFLICT` en DB; `BLOCKED_BY_MISSING_EVIDENCE` para P0-F original; cambios locales ajenos; ausencia de fixture/permiso para E2E mutante.

Lo no listado como Allowed no se modifica.

## Alcance

- Recuperar evidencia y refs de la combinación funcional candidata.
- Separar AS-IS oficial, candidato recuperado y TARGET.
- Validar el backend actual, el frontend actual, metadatos de SP en la instancia local y el stack local.
- Dejar trazada la matriz de contratos y consumidores para revisión de owners.

## No alcance

- No fusionar B1/F1/D1, no cambiar SQL ni contratos públicos, no crear PR de integración y no ejecutar la mutación completa de reclamos/soportes.
- No afirmar que la evidencia histórica 807/807 del frontend ni los E2E históricos equivalen a una ejecución actual de esta combinación.

## Archivos afectados

### Existentes

- `docs/governance/API_DESIGN_RULES.md`
- `docs/contracts/BACKEND_GOLDEN_PATH_CONTRACT.md`
- `docs/contracts/openapi/openapi-golden-path.yaml`
- `docs/contracts/external/db/DB_BASELINE_CONTRACT.md`
- `docs/work-items/P0-E-consolidacion-funcional/VALIDATION.md`
- `docs/work-items/LB-004-stateless-serverless-readiness/PROFESSOR_DECISION.md`

### Nuevos

- `docs/work-items/P0-G-recuperacion-funcional/PLAN.md`
- `docs/work-items/P0-G-recuperacion-funcional/TEST_PLAN.md`
- `docs/work-items/P0-G-recuperacion-funcional/CONTRACT_MATRIX.md`
- `docs/work-items/P0-G-recuperacion-funcional/VALIDATION.md`

### Retirar

- Ninguno.

## Contratos y consumidores afectados

| Ámbito | Consumidor | Compatibilidad | Decisión |
|---|---|---|---|
| HTTP | B1 backend ↔ frontend F1 | Candidata, no activa | Requiere revisión de contrato y ejecución E2E autorizada. |
| Persistence mutation | W1/W2 | 4 columnas canónicas | Compatible con el baseline observado; aprobación DB pendiente. |
| Persistence query | R1/R2 | 10/25 columnas de consulta | `CONTRACT_CONFLICT` solo si se exige la regla de mutación a consultas; requiere decisión explícita del owner. |
| Security/storage | archivo privado + ClamAV/MinIO | Backend-only, sin presigned URLs | Respeta `PROFESSOR_DECISION`; falta cerrar binding DB. |

## Riesgos y dependencias

- `origin/develop` de DB no contiene D1; seleccionar `legacy-db` como autoridad sin decisión sería una suposición.
- El checkout frontend actual tiene cambios locales; cambiarlo o instalar sobre él podría sobrescribir trabajo del usuario.
- `infra/files/.env` falta en el repo; los contenedores existentes están activos, pero no se debe inferir que el arranque limpio será reproducible.
- La prueba mutante del flujo de reclamo puede contaminar datos y necesita fixture, permisos y rollback autorizados.

## Test plan

Ver `TEST_PLAN.md`. La secuencia aplicada es requisito → contrato → evidencia RED/estado → validación; no se modifica código productivo mientras el work item esté `NOT_READY`.

## Rollback y stop conditions

- `CONTRACT_CONFLICT: P0-G-DB-001`: autoridad y forma de R1/R2 frente al baseline de mutaciones.
- `BLOCKED_BY_MISSING_EVIDENCE: P0-G-EVIDENCE-001`: falta el documento original P0-F en el checkout; se continúa solo con evidencia versionada disponible.
- Cualquier cambio de SQL/HTTP o necesidad de cambiar checkout requiere un work item hijo con contrato congelado y aprobación correspondiente.

## Deuda conocida y validación manual

- `TD-P0-G-001`: reconciliación entre los remotos `origin` y `legacy-*` no resuelta por autoridad.
- `MV-P0-G-001`: ejecución real del flujo completo de reclamo y soporte pendiente.

## Definition of Ready

`NOT_READY` para integración funcional. Las refs y la evidencia de preflight están disponibles; faltan aprobación DB D1/ONE_TABLE, decisión sobre R1/R2, fixture/rollback E2E y el documento P0-F original o su sustituto aprobado.
