# VALIDATION — P0-B

Fecha de corte: 2026-10-18 (America/Bogota).

## Dictamen de corte

`GIT_CANONICAL_REMOTE_CONFIGURED = NO` — Backend, Frontend y DB sí; no existe checkout Infra local comprobado.

`ALL_REPOSITORIES_SYNCED = NO` — las ramas existentes se actualizaron por fast-forward cuando fue seguro, pero Infra no está disponible y los gates funcionales externos no se ejecutaron.

`P0_STATUS = BLOCKED`

`READY_FOR_P1_MINIO = NO`

## Protección y remotos

- Backend: se creó `refs/backup/p0b-pre-canonical-20261018` en `d0f170b8` y `stash@{0}` con modificaciones y untracked. Luego `origin` pasó al repositorio oficial y `personal` conservó el remoto anterior.
- Frontend: checkout limpio; `origin` oficial y `legacy-frontend` conservado; feature activa preservada.
- DB: checkout limpio en `develop`; `origin` oficial y `legacy-db` conservado; worktrees y stashes existentes preservados.
- Infra: ruta esperada no existe; no se creó checkout ni se inventó una rama activa.
- No hubo `reset --hard`, force-push, merge automático a `develop/main`, eliminación de ramas, eliminación de stashes ni cambios SQL manuales.

## Sincronizador real

Comando aplicado mediante `scripts/sync-workspaces.ps1 -Apply` con configuración temporal no versionada para los cuatro componentes:

```text
backend   UP_TO_DATE                     develop 253c3c95
frontend  FAST_FORWARDED_INACTIVE_BRANCH develop b0c2225 -> bd2120f5
db        FAST_FORWARDED                 develop f2871a9 -> 32b70ebe
infra     MISSING_REPOSITORY
exit code: 1 (bloqueo explícito de Infra; los otros componentes continuaron)
```

El backend se actualizó primero desde `72312d7c` a `253c3c95`; la feature `codex/p0-sec-001-coordinator-ownership` siguió activa y fue reportada `ahead=2/behind=5` frente a `origin/develop`.

## Pruebas de fixtures

```text
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File .\scripts\tests\test-sync-workspaces.ps1
exit code: 0
sync-workspaces tests: passed=8 failed=0
```

Escenarios PASS: actualizado, fast-forward (incluida repetición idempotente), feature activa, worktree con cambios, preservación de untracked, divergencia, remoto incorrecto, repositorio faltante y error de fetch. El arnés usa repositorios bare temporales y limpia su directorio temporal.

## Ownership integrado

La corrección `cad539f5` se aplicó sin conflictos sobre `origin/develop` en la rama `codex/p0-b-canonical-sync-ownership`. Conserva el work item `P0-SEC-001` y sus tests RED/GREEN. La integración SQL Server y la prueba de no-escritura cross-program son `NOT_RUN`; no se presentan como PASS.

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

## Gates no ejecutados o bloqueados

| Gate | Estado | Motivo |
|---|---|---|
| Backend Java 25 `clean verify` en rama P0-B | `PASS` | 1557/1557; JaCoCo LINE 90 %, BRANCH 78 %; exit 0. |
| SQL Server / Failsafe / stored procedures | `NOT_RUN` | No se dispone de ambiente controlado declarado para esta intervención. |
| Frontend build, Karma/ChromeHeadless, cobertura, E2E | `NOT_RUN` | La copia se sincronizó, pero el entorno de ejecución no fue certificado. |
| Infra Compose/healthchecks | `BLOCKED_BY_MISSING_EVIDENCE` | No hay checkout oficial local. |
| CI, CodeQL, Sonar, Dependency Review del PR oficial | `NOT_RUN` | La rama todavía no se publicó. |
| MinIO, Redis y reclamos | `NOT_RUN / OUT_OF_SCOPE` | Expresamente fuera de P0-B. |

## Próximo paso autorizado

Publicar `codex/p0-b-canonical-sync-ownership` y abrir PR hacia `AsistenciasUCO/AsistenciasUCO-Backend:develop` sin merge. Antes del cierre funcional se requiere localizar/autorizar Infra y ejecutar las validaciones externas aplicables.
