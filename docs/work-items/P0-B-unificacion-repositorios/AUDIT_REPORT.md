# AUDIT REPORT — P0-B

Fecha de corte: 2026-10-18 (America/Bogota).

## A. Configuración Git final

| Repositorio | Ruta local | `origin` | Remotos secundarios | Rama activa | Integración / SHA | SHA anterior → actualizado | Estado |
|---|---|---|---|---|---|---|---|
| Backend | `C:\Users\josev\AsistenciasUCO\AsistenciasUCO` | `AsistenciasUCO/AsistenciasUCO-Backend` | `personal=alejovh12/AsistenciasUCO` | `codex/p0-sec-001-coordinator-ownership` / `d0f170b8` | `develop` / `253c3c95` | `72312d7c → 253c3c95` | `UP_TO_DATE`; feature ahead 2/behind 5, preservada |
| Frontend | `C:\Users\josev\OneDrive\Documentos\Front_Asistecias\AsistenciasUCO-Frontend` | `AsistenciasUCO/AsistenciasUCO-Frontend` | `legacy-frontend=shesho22/AsistenciasUCO-Frontend` | `jose-valencia/maint-004-frontend-utc-v2-integration` / `b4368d1` | `develop` / `bd2120f5` | `b0c2225 → bd2120f5` | feature preservada; develop fast-forward |
| DB | `C:\Users\josev\OneDrive\Documentos\AsisteciaUco_db\git\gestion-asistencia-db` | `AsistenciasUCO/AsistenciasUCO-DB` | `legacy-db=johnjduque/gestion-asistencia-db` | `develop` / `32b70ebe` | `develop` / `32b70ebe` | `f2871a9 → 32b70ebe` | fast-forward; worktrees/stashes preservados |
| Infra | no localizado | no configurado | no aplica | no aplica | `main` esperado, no checkout | no aplica | `MISSING_REPOSITORY` |

El backend conserva `refs/backup/p0b-pre-canonical-20261018` y `stash@{0}`. El PR personal #25 permanece abierto/draft en `alejovh12/AsistenciasUCO`; no fue cerrado, retargeteado ni mergeado.

## B. Script de sincronización

- Ubicación: `scripts/sync-workspaces.ps1`.
- Configuración: copiar `scripts/sync-workspaces.config.example.json` a una ruta local no versionada, o proporcionar las cuatro rutas por parámetros.
- Consulta: `powershell -ExecutionPolicy Bypass -File .\scripts\sync-workspaces.ps1 -Status -ConfigPath <config.json>`.
- Aplicación: `powershell -ExecutionPolicy Bypass -File .\scripts\sync-workspaces.ps1 -Apply -ConfigPath <config.json>`.
- `-Apply` hace `fetch origin --prune`, compara SHA/ahead/behind/merge-base, actualiza solo por fast-forward y continúa con componentes independientes.
- Con cambios locales solo hace fetch y reporta `DIRTY_WORKTREE_FETCH_ONLY`; no hace stash, checkout, merge, reset ni push.
- Con divergencia, remoto incorrecto, rama ausente o error de red detiene ese componente y mantiene los demás.
- La prueba de no pérdida es doble: el script exige worktree limpio para escribir y el arnés verifica que el untracked y el SHA permanezcan.

## C. Matriz diferencial de commits

| Repositorio | Commit | Autor | Archivos / tema | Estándar | Hallazgo | Corrección | Estado |
|---|---|---|---|---|---|---|---|
| Backend | `685efe0` | Sergio | PEP baseline, `SecurityPepEnforcementFilter`, config | Clean Architecture / seguridad | Provider PEP vive en Infrastructure; integración runtime no ejecutada | Ninguna en P0-B; evaluar starter institucional en work item posterior | `PASS_STATIC / NOT_RUN_RUNTIME` |
| Backend | `37a5d48` | Sergio | Merge del baseline PEP | Dependencias / wiring | No se observó segundo filtro en la rama oficial | Ninguna | `PASS_STATIC` |
| Backend | `0686acc` | Sergio | Javaagent y wiring de matrícula | Testing / Composition Root | Cambio de test/wiring posterior a la base; requiere verify nuevo | Conservado; validado en verify de la rama | `PENDING_VERIFY` |
| Backend | `d55cd7c` | Sergio | HU050 plan/test/validation y matrícula | Contract-first / testing | Evidencia documental, no certificación SQL Server | Ninguna; SQL Server queda `NOT_RUN` | `DOCUMENTED` |
| Backend | `253c3c9` | Sergio | HU116 facultad, controller, JPA, tests | Clean Architecture / HTTP | Diff coherente estáticamente; DB/Frontend real no ejecutados aquí | Ninguna en P0-B | `PASS_STATIC / NOT_RUN_RUNTIME` |
| Backend | `cad539f5` | Jose Alejandro Valencia Henao | Ownership Application de asignatura | Seguridad / 401-403 / scope | Update/toggle no propagaban actor en la base personal | Integrado sobre `origin/develop` en rama P0-B; defensa SQL queda al owner DB | `IMPLEMENTED / SQL_NOT_RUN` |
| Frontend | `d43002a` | Sergio | Switch de seguridad, claims/scopes | Seguridad / guards | Consumidor alineado estáticamente; E2E no ejecutado | Ninguna | `STATIC_ONLY` |
| Frontend | `d5abd82` | Sergio | Merge UTC v2 + seguridad | Contrato temporal | Cambios posteriores a snapshot; requiere build/test real | Ninguna | `NOT_RUN_TESTS` |
| Frontend | `a3bbebc` | Sergio | HU116 administración de facultades | HTTP / modelos / servicios | Consumer existe para endpoint oficial; conformance runtime no probado | Ninguna | `STATIC_ONLY` |
| Frontend | `bd2120f` | Sergio | UX-006 rutas/guards y vistas | Rutas / permisos / UX | Diff amplio; ChromeHeadless y E2E no disponibles en este corte | Ninguna | `NOT_RUN_TESTS` |
| Frontend | `f32db2c` | Sergio | formularios, matrícula y selector de sesión | Compatibilidad HTTP | Debe validarse contra backend/DB actuales | Ninguna | `NOT_RUN_TESTS` |
| Frontend | `969d17c`, `216b2a8`, `30c73e7` | Sergio | Sonar/CI frontend | CI / calidad | No se infiere PASS de los YAML | Ninguna | `REMOTE_CI_NOT_RUN` |
| DB | `6946a73` | Jose Alejandro Valencia Henao | UTC-D06, SP/vistas y readonly | Contrato DB / UTC | Requiere backend y SQL Server desplegado compatible | Ninguna; owner DB conserva contrato | `STATIC_ONLY` |
| DB | `3842f70`, `f64abb6`, `3f2ccf8`, `3f6afec` | Jose Alejandro Valencia Henao | permisos, providers públicos y fixtures | Persistencia / seguridad | Contratos versionados; no se ejecutaron stored procedures | Ninguna | `NOT_RUN_SQL` |
| DB | `db452bb`, `9f3889f`, `336472c` | Jose Alejandro Valencia Henao | seeds, vistas y fixtures de paginación | Datos / pruebas | Evidencia de fixture; no se tocó desde P0-B | Ninguna | `NOT_RUN_SQL` |
| DB | `af37ccc`, `d9741b1`, `b5565de`, `e9f3f27`, `e248619` | Jose Alejandro Valencia Henao | MAINT-003K y consolidación dev | Golden Path / calidad | Cambios reales deben probarse con DB controlada | Ninguna | `NOT_RUN_SQL` |
| DB | `0faa5ac`, `e7ddfeb`, `0c4dde2`, `771a9ed`, `833b3d3`, `5bc0ef3`, `c26823c` | johnjduque | governance, Docker dev y CI | higiene / seguridad | No se administran contenedores ni esquema desde backend | Ninguna | `STATIC_ONLY` |
| DB | `32b70eb` | Sergio | HU116 `usp_crear_facultad` | Contrato DB / transacción | Debe comprobarse contra SQL Server; no se modifica por este work item | Ninguna | `NOT_RUN_SQL` |

## D. Coordinador y seguridad

La rama nueva mantiene slices verticales, `InputPort`/Interactor/UseCase en Application y wiring concreto en Composition Root. El ownership integrado valida programa, plan y asignatura antes del command y propaga el sujeto JWT en update/toggle. El riesgo residual es que los providers SQL de update/toggle no exponen todavía una defensa equivalente para callers distintos del backend; no se cambia SQL sin decisión del owner DB.

La rama oficial ya contiene un único filtro PEP en `infrastructure.config.security`, activable por propiedad. No se añadió starter `security-pep-integration-spring-boot-starter:0.1.3` ni un segundo filtro en P0-B. La disponibilidad/contrato operacional del PEP queda `NOT_RUN`.

## E. Gates y pruebas

| Ámbito | Resultado | Evidencia / limitación |
|---|---|---|
| Git sync fixtures | `PASS` | arnés 8/8, exit 0; incluye repetición idempotente; ver `VALIDATION.md` |
| Backend ownership target | `PASS` | Tests dirigidos 15/15 y `clean verify` 1557/1557 en la rama oficial nueva |
| Backend `clean verify` | `PASS` | Java 25, exit 0; JaCoCo LINE 90 %, BRANCH 78 % |
| Frontend build/tests | `NOT_RUN` | copia externa sincronizada, sin ejecutar Node/ChromeHeadless en P0-B |
| DB checks/SP/SQL Server | `NOT_RUN` | no se ejecuta esquema ni fixtures externos |
| Infra Compose | `BLOCKED_BY_MISSING_EVIDENCE` | checkout oficial no localizado |
| PR oficial | `OPEN/DRAFT` | [AsistenciasUCO/AsistenciasUCO-Backend#1](https://github.com/AsistenciasUCO/AsistenciasUCO-Backend/pull/1); no merge |
| CI/Sonar oficial | `NOT_RUN` | requiere ejecución remota del PR |

## F. Publicación

La rama de trabajo `codex/p0-b-canonical-sync-ownership` está publicada en la organización con HEAD `561802a`. El PR borrador [AsistenciasUCO/AsistenciasUCO-Backend#1](https://github.com/AsistenciasUCO/AsistenciasUCO-Backend/pull/1) apunta a `develop`; no se hizo merge. El PR personal #25 sigue intacto como referencia histórica y no se reutilizó para el PR oficial.
