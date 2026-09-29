---
status: closed
type: evidence
scope: backend
owner: backend-team
last-reviewed: 2026-09-29
---

# LB-003 — Remote CI Evidence

## Pull request certificado

- Repository: `alejovh12/AsistenciasUCO`
- Pull request: `#14` — `complete JPA attendance baseline and quality gate`
- Base: `develop`
- Head branch: `jose-valencia/lb-002.2a-jpa-command-plan`
- Head SHA certificado: `e92afb73221ac3a967e63c673312d3961cfb0688`
- Estado observado: `OPEN`, `mergeable=true`, sin conflictos con `develop`.

## Required checks observados sobre el SHA actual

| Check | Resultado |
|---|---|
| `Backend Quality Gate` | PASS |
| `CodeQL Java Analysis` | PASS |
| `Dependency Review` | PASS |
| `SonarCloud Code Analysis` | PASS — Quality Gate passed |

Checks adicionales observados:

- `Code scanning results / CodeQL`: PASS — no new alerts in code changed by this pull request.

## Evidencia de workflows

- `Backend CI` run `#44`: SUCCESS sobre el head SHA del PR. El job `Backend Quality Gate` completó Java 25, `Maven clean verify`, existencia del reporte JaCoCo, análisis/Quality Gate de SonarQube Cloud, JAR, imagen Docker y subida de artefactos.
- `Backend Security` run `#44`: SUCCESS sobre el head SHA del PR. `CodeQL Java Analysis` PASS y `Dependency Review` PASS.
- SonarCloud sobre PR #14: `Quality Gate passed`, `0 Security Hotspots`, `100.0% Coverage on New Code`, `0.0% Duplication on New Code`. Sonar reporta issues de calidad en new code, pero el Quality Gate configurado los acepta; no se presentan como cero issues.

## Ruleset de develop

Revisión humana autenticada confirmó `Protect develop` activo sobre `refs/heads/develop`:

- Pull Request requerido.
- `deletion` bloqueada.
- `non-fast-forward` bloqueado.
- sin bypass.
- required checks: `Backend Quality Gate`, `CodeQL Java Analysis`, `Dependency Review`, `SonarCloud Code Analysis`.

## Dictamen LB-003

`REMOTE_CI_CURRENT_IMPLEMENTATION: PASS`.

El bloqueo `BLOCKED_PENDING_REMOTE_CI` queda resuelto para LB-003. `MV-004` puede cerrarse como PASS para gobernanza/quality-security gates remotos del checkout certificado.

`TD-023` NO se cierra por completo: queda `OPEN / PARTIAL` exclusivamente por CI DB reproducible/versionada, responsabilidad de una fase de infraestructura posterior (LB-006 salvo nueva decisión normativa). Esa deuda ya no bloquea el cierre de LB-003.
