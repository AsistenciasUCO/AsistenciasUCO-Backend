# VALIDATION — P0-B

Fecha de corte: 2026-10-18 (America/Bogota).

## Dictamen de corte

`GIT_CANONICAL_REMOTE_CONFIGURED = YES` — los cuatro checkouts tienen `origin` apuntando a repositorios de `AsistenciasUCO`.

`ALL_REPOSITORIES_SYNCED = YES_WITH_PRESERVED_LOCAL_CHANGES` — los cuatro repositorios fueron comprobados por el sincronizador real; Backend y Frontend quedaron dirty y no se tocaron, DB e Infra quedaron up to date.

`P0_STATUS = BLOCKED_BY_EXTERNAL_GATES`

`READY_FOR_P1_MINIO = NO`

## Protección y remotos

- Backend: se creó `refs/backup/p0b-pre-canonical-20261018` en `d0f170b8` y `stash@{0}` con modificaciones y untracked. Luego `origin` pasó al repositorio oficial y `personal` conservó el remoto anterior.
- Frontend: checkout limpio; `origin` oficial y `legacy-frontend` conservado; feature activa preservada.
- DB: checkout limpio en `develop`; `origin` oficial y `legacy-db` conservado; worktrees y stashes existentes preservados.
- Infra: clonado oficialmente en `C:\Users\josev\AsistenciasUCO\AsistenciasUCO-Infra`; `main` y `develop` verificados. `develop` es la rama de integración vigente (9 commits por delante de `main`); el checkout activo permanece en `main`.
- No hubo `reset --hard`, force-push, merge automático a `develop/main`, eliminación de ramas, eliminación de stashes ni cambios SQL manuales.

## Sincronizador real

Comando aplicado mediante `scripts/sync-workspaces.ps1 -Apply` con configuración temporal no versionada para los cuatro componentes:

```text
backend   UP_TO_DATE_DIRTY  develop 253c3c95 -> 253c3c95  active=codex/p0-sec-001-coordinator-ownership
frontend  UP_TO_DATE_DIRTY  develop bd2120f5 -> bd2120f5  active=jose-valencia/maint-004-frontend-utc-v2-integration
db        UP_TO_DATE         develop 32b70ebe -> 32b70ebe  active=develop
infra     UP_TO_DATE         develop 6ae52da6 -> 6ae52da6  active=main
exit code: 0
```

El `-Apply` hizo `fetch origin --prune`; para Backend y Frontend reportó explícitamente que el worktree tenía cambios y no escribió ramas ni archivos. La rama de integración de Infra quedó configurada en `develop` tanto en el script como en el ejemplo de configuración.

El backend se actualizó primero desde `72312d7c` a `253c3c95`; la feature `codex/p0-sec-001-coordinator-ownership` siguió activa y fue reportada `ahead=2/behind=5` frente a `origin/develop`.

## Pruebas de fixtures

```text
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File .\scripts\tests\test-sync-workspaces.ps1
exit code: 0
sync-workspaces tests: passed=8 failed=0
```

Escenarios PASS: actualizado, fast-forward (incluida repetición idempotente), feature activa, worktree con cambios, preservación de untracked, divergencia, remoto incorrecto, repositorio faltante y error de fetch. El arnés usa repositorios bare temporales y limpia su directorio temporal.

## Ownership integrado

La corrección `cad539f5` se aplicó sin conflictos sobre `origin/develop` en la rama `codex/p0-b-canonical-sync-ownership`. La validación dirigida `GestionarAsignaturaUseCaseImplTest` + `AsignaturaJpaCommandPatternTest` terminó 15/15. La prueba de no-escritura cross-program contra SQL Server real es `NOT_RUN`; el riesgo de los stored procedures y la propuesta contractual están separados en `DB_OWNER_PROPOSAL.md`.

## Gate backend sobre la rama oficial nueva

```text
Java: 25
Comando: .\\mvnw.cmd -B -ntp "-Dmaven.repo.local=C:\\Users\\josev\\.m2\\repository" clean verify "-DargLine=@{argLine} -javaagent:C:/Users/josev/.m2/repository/org/mockito/mockito-core/5.20.0/mockito-core-5.20.0.jar"
Resultado: exit code 0 / BUILD SUCCESS
Tests: 1557 run, 0 failures, 0 errors, 0 skipped
JaCoCo: LINE 90 %; BRANCH 78 %; checks met
Duración: 02:44 min
```

El primer intento del gate dirigido quedó bloqueado por permisos del caché Maven local; se reejecutó con el caché de usuario y terminó en `PASS`. El warning de ausencia de provider Jakarta Validation y los logs esperados de pruebas no alteraron el resultado.

## Auditoría diferencial

Se compararon los commits posteriores a los snapshots:

- Backend: `72312d7c..253c3c95`, cinco commits oficiales, 39 archivos/1799 adiciones y 36 eliminaciones.
- Frontend: `b4368d1e..bd2120f5`, siete commits oficiales, 85 archivos/7440 adiciones y 3481 eliminaciones.
- DB: `f2871a9..32b70ebe`, 25 commits oficiales, 72 archivos/6735 adiciones y 1382 eliminaciones.

La matriz por commit, estándares y hallazgos está en `AUDIT_REPORT.md`. La revisión fue estática/diferencial; no equivale a prueba de provider real.

## Frontend validado

```text
Comando: npm run verify
Resultado: exit code 0
ChromeHeadless 155: 341 SUCCESS / 341
Realtime coverage: LINE 95.38% (227/238), BRANCH 83.95% (68/81)
Production build: PASS
```

Durante el cierre se corrigieron dos defectos reales del componente de sesiones (NG0600 al resetear señales y pérdida de selección tras crear) y se alineó el fixture de `DeanFaculty` con las solicitudes vigentes. Los cambios están publicados en [AsistenciasUCO/AsistenciasUCO-Frontend#1](https://github.com/AsistenciasUCO/AsistenciasUCO-Frontend/pull/1).

## CI, Sonar y seguridad del PR backend

En el head de código `ca9e72f4` del PR backend #1, la evidencia remota fue:

- Backend CI `39176860222`: Maven/JaCoCo PASS; SonarQube Cloud FAIL por autorización del proyecto/configuración externa.
- Backend Security `39176860225`: CodeQL PASS; Dependency Review FAIL porque el Dependency Graph del repositorio no está habilitado.
- Backend Deep Security Scan `39176860286`: Trivy de repositorio, Trivy de imagen y ArchUnit PASS.

No se deshabilitó ningún check ni se registró ningún secreto. Los dos fallos son configuración/permiso del repositorio externo, no fallos de código identificados.

El head actual `de25ffc7` solo añade documentación/configuración del sincronizador y la propuesta para DB; la consulta de ejecuciones PR-triggered para ese SHA devolvió `workflow_runs: []` al corte. Por tanto, los resultados anteriores se conservan como evidencia del head de código y no se presentan como una ejecución nueva del commit documental.

## SQL Server y stored procedures

`Test-NetConnection localhost -Port 1433` devolvió `False`; Docker no pudo consultar el engine (`Access is denied`). Por ello `scripts/test-local.ps1`, Failsafe de integración y `test_summary.ps1` quedaron `NOT_RUN`. No se eliminaron datos, no se recrearon contenedores y no se ejecutaron migraciones.

## Gates no ejecutados o bloqueados

| Gate | Estado | Motivo |
|---|---|---|
| Backend Java 25 `clean verify` en rama P0-B | `PASS` | 1557/1557; JaCoCo LINE 90 %, BRANCH 78 %; exit 0. |
| SQL Server / Failsafe / stored procedures | `NOT_RUN` | Puerto 1433 cerrado y Docker inaccesible; ejecución no segura sin ambiente controlado. |
| Frontend build, Karma/ChromeHeadless, cobertura | `PASS` | `npm run verify`; 341/341, realtime LINE 95.38 %, BRANCH 83.95 %, build PASS. |
| Frontend E2E contra backend real | `NOT_RUN` | No se habilitó un entorno integrado; no se inventa resultado. |
| Infra ramas/sincronizador | `PASS` | Checkout oficial y `main`/`develop` verificados; `develop` configurada como integración. |
| CI / CodeQL / Sonar / Dependency Review PR backend | `BLOCKED_BY_EXTERNAL_CONFIGURATION` | CodeQL, Trivy y ArchUnit PASS; Sonar y Dependency Review FAIL por configuración externa. |
| MinIO, Redis y reclamos | `NOT_RUN / OUT_OF_SCOPE` | Expresamente fuera de P0-B. |

## Próximo paso autorizado

La rama `codex/p0-b-canonical-sync-ownership` quedó publicada en `origin`. El PR [AsistenciasUCO/AsistenciasUCO-Backend#1](https://github.com/AsistenciasUCO/AsistenciasUCO-Backend/pull/1) sigue abierto/draft hacia `develop`, sin merge. El PR [AsistenciasUCO/AsistenciasUCO-Frontend#1](https://github.com/AsistenciasUCO/AsistenciasUCO-Frontend/pull/1) está abierto hacia `develop`. Los dos quedan sujetos a revisión y a la corrección de la configuración externa de CI.

Antes del cierre funcional se requiere disponer de SQL Server controlado, resolver la configuración externa de Sonar/Dependency Review y completar la revisión de los PR; Infra ya fue localizado y verificado.
