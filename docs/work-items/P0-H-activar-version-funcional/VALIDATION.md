# VALIDATION — P0-H: activar y validar la última versión funcional

## Dictamen

EN CURSO. La activación local no se declara PASS hasta ejecutar los gates y las comprobaciones manuales. La integración oficial de D1/R2 permanece bloqueada por `CONTRACT_CONFLICT: P0H-DB-001`.

## Comandos ejecutados

| Fecha/entorno/base | Comando | Exit code | Resultado | Evidencia sanitizada |
|---|---|---:|---|---|
| 2026-10-20 / backend actual | inventario de refs/ascendencia Git | 0 | PASS | backend `6893f4d`; candidato `f45a5213`; frontend `95a202a` y `fac2f640`; DB `b698c76` |
| 2026-10-20 / backend actual | snapshot de cambios locales | 0 | PASS | `.p0-h-snapshot-20261020/` preservado; stash Git no disponible por índice solo lectura |
| 2026-10-20 / frontend original | `git status`, refs y ascendencia | 0 | PASS | tres archivos locales preservados; `fac2f640` ancestro de `95a202a` |
| pendiente | `mvnw.cmd -B -ntp verify` | — | NOT_RUN | se ejecutará sobre backend integration |
| pendiente | `npm.cmd run verify` | — | NOT_RUN | se ejecutará sobre frontend integration |
| pendiente | `start-local-stack.ps1` y health checks | — | NOT_RUN | requiere stack/secretos locales disponibles |
| pendiente | E2E docente/estudiante real | — | NOT_RUN | requiere fixture, tokens y rollback autorizados |

## Checks

| Nivel | Check | PASS / FAIL / NO APLICA / BLOQUEADO | Evidencia/motivo |
|---|---|---|---|
| Alcance | plan y preservación | PASS | snapshots y referencias candidateadas registrados |
| Arquitectura | ArchUnit/dependencias | NOT_RUN | pendiente `verify` sobre integración |
| Contratos | HTTP/DB/security | BLOQUEADO | R1/R2 y autoridad DB requieren owner |
| Tests | RED → GREEN | NOT_RUN | se ejecutan tests existentes sin modificar |
| Coverage | líneas ≥80 %, ramas ≥70 % | NOT_RUN | pendiente Maven/Angular |
| Integración | DB/provider real | NOT_RUN | pendiente stack centralizado |
| Build | Maven verify / frontend verify | NOT_RUN | pendiente ramas de integración |
| Seguridad | JWT/RBAC/ownership/secret hygiene | NOT_RUN | no se imprimen secretos; pendiente runtime |
| Documentos | work item y trazabilidad | PASS | PLAN/TEST_PLAN/VALIDATION creados |

## Integridad de RED

No aplica RED nuevo. Los candidatos y sus tests se fijan por SHA; cualquier conflicto de contrato se devuelve al owner correspondiente.

## Bloqueantes y limitaciones

- `CONTRACT_CONFLICT: P0H-DB-001`: DB baseline v1 fija resultset público de cuatro columnas, mientras D1/R2 candidatea R1 con diez y R2 con veinticinco columnas de lectura. Requiere decisión del owner DB.
- `BLOCKED_BY_MISSING_EVIDENCE: P0H-E2E-001`: no se declara E2E mutante hasta confirmar fixture/rollback y providers reales.
- La evidencia histórica de P0-C/P0-E orienta la reproducción, pero no es resultado actual.
- El snapshot Git se conserva porque el índice del backend no permitió `stash`; la recuperación se hace con el patch/manifest, sin borrar backups.

## Evidencias remotas/manuales pendientes

SHA de procesos, puertos, IntelliJ, Angular, Maven, npm, SQL Server/Keycloak/MinIO/ClamAV, resultados de grupos/horarios, reclamos/soportes, login de ambas identidades, commits de integración y PR contractual D1/R2.
