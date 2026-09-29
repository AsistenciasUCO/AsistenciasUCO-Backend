---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-29
---

# Línea base técnica activa

La vertical patrón es [asistencia en lote + consulta + realtime](GOLDEN_PATH_ASISTENCIA.md), decidida en [ADR-001](../adr/ADR-001-golden-path-asistencia.md). Esta es la secuencia normativa activa; el detalle histórico permanece en los work items y cierres enlazados.

## Estado de fases

| Fase | Alcance | Estado |
|---|---|---|
| LB-000 | Gobernanza y documentación | **CLOSED** — governance complete; build gate restaurado por TECH-001 |
| LB-001 | Golden Path + Contract First | **CLOSED / FROZEN** — DB↔backend↔frontend alineados; OpenAPI/Swagger/contrato congelados |
| LB-001D | Governance hardening | **CLOSED / FROZEN** — seguridad webhook y aislamiento cloud integration cerrados; MV-003 sigue parcial |
| LB-002 | Piloto JDBC → JPA | **CLOSED / FROZEN** — query JPA + command JPA `registrarAsistenciasSesion`, paridad SQL Server real, fallback JDBC preservado |
| LB-003 | Quality Gate Golden Path | **CLOSED / PASS** — local quality gate, SQL Server targeted y CI remoto del PR #14 verdes |
| LB-004 | Stateless / Serverless readiness | **NEXT_PHASE / NOT_STARTED** |
| LB-005 | Realtime distribuido | NOT_STARTED |
| LB-006 | IaC / CD / cloud | NOT_STARTED |
| LB-007 | Replicar patrón | NOT_STARTED |

Ninguna fase autoriza automáticamente la siguiente. No combinar JPA, negocio, contrato, realtime y cloud en una sola tarea sin límites.

## Golden Path actual

```text
POST /api/v1/asistencias/lote
  -> AsistenciaController
  -> RegistrarAsistenciasSesionInputPort / UseCase
  -> AsistenciaRepositoryPort
  -> AsistenciaRepositoryHybridSqlServerAdapter
       -> command registrarAsistenciasSesion: JPA en local; JDBC fallback disponible
       -> demás commands de Asistencia: JDBC
  -> dbo.usp_registrar_asistencias_sesion
  -> RealtimePublisherPort
  -> SSE local

GET /api/v1/grupos/{grupoId}/asistencias
  -> AsistenciaRepositoryPort
  -> query JPA en local/dev; JDBC fallback disponible
```

La DB sigue siendo la autoridad contractual del Golden Path. JPA no cambia el contrato HTTP, Application/Domain ni el Stored Procedure congelado.

## Providers AS-IS

| Perfil | Query asistencia | Command `registrarAsistenciasSesion` |
|---|---|---|
| `local` | JPA | JPA |
| `dev` | JPA | JDBC |
| sin perfil / prod-like | JDBC | JDBC |

Los selectores son independientes y fail-closed. JDBC no fue retirado.

## Evidencia consolidada

### LB-001 — contrato

- DB↔backend Golden Path alineado: [LB-001B.4](../work-items/LB-001B.4-final-backend-contract-closure/CLOSURE.md).
- OpenAPI/Contract First + Swagger UI: [LB-001C](../work-items/LB-001C-openapi-contract-first/CLOSURE.md).

### LB-002 — JPA incremental

- JPA query pilot/activation: `LB-002.1` / `LB-002.1B`.
- JPA command `registrarAsistenciasSesion`: [LB-002.2 final closure](../work-items/LB-002-jpa-incremental/LB-002.2-jpa-command-pilot/LB-002.2-FINAL-CLOSURE.md).
- SQL Server parity: `PARITY_MISMATCHES=0`.
- Runtime local manual: query JPA + command JPA PASS.
- Realtime dos clientes + reconexión/reconvergencia ~25 s; HTTP/DB source of truth.

### LB-003 — Quality Gate Golden Path

- `clean verify`: **1426 tests**, 0 failures/errors/skips.
- JaCoCo: **LINE 91.52 % / BRANCH 81.17 %**.
- ArchUnit: **85/85 PASS**.
- OpenAPI: **16/16 PASS**.
- SQL Server Golden Path targeted: **59/59 PASS**, 0 skips, 0 mismatches.
- Seguridad, errores seguros, correlation, write/read y realtime: PASS.
- PR #14 / SHA `e92afb73221ac3a967e63c673312d3961cfb0688`: required checks `Backend Quality Gate`, `CodeQL Java Analysis`, `Dependency Review`, `SonarCloud Code Analysis` PASS; check adicional CodeQL PASS.
- SonarCloud: Quality Gate passed, 0 Security Hotspots, 100 % coverage on new code, 0 % duplication on new code. Los 37 new issues reportados por Sonar quedan como backlog de calidad; no se ocultan.

Ver [LB-003 CLOSURE](../work-items/LB-003-quality-gate-golden-path/CLOSURE.md) y [REMOTE_CI_EVIDENCE](../work-items/LB-003-quality-gate-golden-path/REMOTE_CI_EVIDENCE.md).

## Deuda relevante para la secuencia

| ID | Estado | Impacto sobre fases |
|---|---|---|
| TD-001 | CLOSED — LB-002.2 | no bloquea |
| TD-003 | OPEN | bloquea LB-005 (realtime distribuido) |
| TD-004 | OPEN | bloquea LB-004 (storage local / ownership) |
| TD-008 | CLOSED — LB-003 | no bloquea |
| TD-010 | OPEN | no bloquea LB-003; revisar release DB |
| TD-011 | OPEN | relevante para LB-005 si exige durabilidad |
| TD-022 | OPEN | bloquea LB-006 según exposición Actuator/Prometheus |
| TD-023 | **OPEN / PARTIAL** | LB-003 remote gates satisfechos; pendiente solo CI DB reproducible/versionada para LB-006 salvo nueva decisión |
| TD-027 | OPEN | bloquea LB-006 según capacidad modificada |
| TD-043 | **OPEN / DEFERRED / NON-GOLDEN** | no bloquea Golden Path; sí las tres features afectadas |
| TD-049 | OPEN / OUT_OF_GOLDEN_PATH | no bloquea Golden Path |
| TD-050 | OPEN / NON_BLOCKING | latencia de reconciliación realtime |

El ledger completo y la historia por ID permanecen en [TECHNICAL_DEBT.md](TECHNICAL_DEBT.md).

## CI y gobernanza remota

Ruleset `Protect develop` observado activo:

- PR requerido.
- deletion / non-fast-forward bloqueados.
- sin bypass.
- required checks: Backend Quality Gate, CodeQL Java Analysis, Dependency Review, SonarCloud Code Analysis.

Para LB-003, esos checks pasaron sobre el PR #14 y el SHA actual certificado. [MV-004](MANUAL_VALIDATION_LEDGER.md) queda PASS para esta validación remota.

TD-023 permanece abierta únicamente por la ausencia de CI DB reproducible/versionada cross-repo; esa parte se difiere a LB-006 salvo nueva decisión normativa.

## Instrucción activa

**LB-003 está CLOSED / PASS.**

La siguiente fase normativa es:

```text
LB-004 — Stateless / Serverless readiness
STATUS: NEXT_PHASE / NOT_STARTED
```

Objetivo de LB-004: demostrar que el Golden Path puede operar sin depender de filesystem o heap de una única réplica; externalizar storage/config/secrets cuando aplique y evaluar cache solo con necesidad medida. TD-004 es el bloqueo principal conocido.

LB-004 no se inicia automáticamente. Requiere DoR, PLAN, alcance explícito y autorización humana.

## Referencias activas

- [Golden Path](GOLDEN_PATH_ASISTENCIA.md)
- [Definition of Ready](../governance/DEFINITION_OF_READY.md)
- [Definition of Done](DEFINITION_OF_DONE.md)
- [Technical Debt](TECHNICAL_DEBT.md)
- [Manual Validation Ledger](MANUAL_VALIDATION_LEDGER.md)
- [LB-002.2 Final Closure](../work-items/LB-002-jpa-incremental/LB-002.2-jpa-command-pilot/LB-002.2-FINAL-CLOSURE.md)
- [LB-003 Closure](../work-items/LB-003-quality-gate-golden-path/CLOSURE.md)
