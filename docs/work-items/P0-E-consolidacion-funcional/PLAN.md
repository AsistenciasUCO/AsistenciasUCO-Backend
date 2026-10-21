---
status: active
type: work-item
scope: cross-repository-functional-consolidation-and-local-verification
owner: backend-team
last-reviewed: 2026-10-20
---

# P0-E — Consolidación funcional y restauración del entorno local

## Identidad y objetivo

- Fecha / autor o rol / base Git: 2026-10-20 / agente Codex / Backend `a360dc0` en `codex/p0-sec-001-coordinator-ownership`.
- Objetivo: recuperar de forma trazable los cambios funcionales preservados, alinear únicamente contratos compatibles y validar el entorno local controlado desde las rutas oficiales de Backend, Frontend, DB e Infra.
- Criterios de aceptación: inventario Git actualizado; commits funcionales clasificados por repositorio; tres SQL pendientes descritos y preservados; deriva de Keycloak/MinIO/ClamAV diagnosticada sin exponer secretos; servicios comprobados; recorridos funcionales etiquetados `PASS`, `FAIL`, `BLOCKED` o `NOT_RUN`; comandos reproducibles de arranque y validación; sin merge final ni borrado de cuarentenas.
- Skills y fuentes autoritativas: `uco-baseline`, `uco-arquitectura`, `uco-contratos`, `uco-persistencia`, `uco-seguridad`, `uco-observabilidad`, `uco-realtime`, `uco-testing`; `AGENTS.md`; `SOURCE_OF_TRUTH.md`; OpenAPI canónico; contratos DB y decisión `LB-004B.2` de storage.

## AS-IS y evidencia

| Hecho | Archivo + símbolo / contrato + versión | Evidencia y límites |
|---|---|---|
| Backend oficial existe y tiene cambios locales preservados | `C:\Users\josev\AsistenciasUCO\AsistenciasUCO`, HEAD `a360dc0`, rama `codex/p0-sec-001-coordinator-ownership` | 7 commits ahead de `origin/codex/p0-sec-001-coordinator-ownership`; `.env.example`/scripts y work items locales modificados o nuevos; no se descarta nada. |
| Frontend funcional recuperado está en refs preservadas | `C:\Users\josev\OneDrive\Documentos\Front_Asistecias\AsistenciasUCO-Frontend`, ref `p0d-recovered/codex/p0-d-frontend-f1-recovered` | `fac2f640`, `a33876b` y `95a202a` son alcanzables; la rama oficial de checkout `b4368d1` conserva tres modificaciones locales. Autoridad `origin` vs `legacy-frontend` requiere decisión versionada. |
| Backend LB-004D no está en la rama canónica actual | `jose-valencia/lb-004d-b1-support-binding` | `2824170`, `39a3aa5`, `aea1d6d` y `f45a521` son exclusivos de la línea recuperada frente a `origin/develop`; su promoción requiere contrato DB D1 y RED/validación. |
| DB D1 y D0 están fuera de `origin/develop` | `gestion-asistencia-db`, refs `legacy-db/codex/lb-004d-d1-archivo-soporte` y `codex/p0-d-db-d0-recovered` | `231be58`, `b698c76`, `4e5e218` y `bdb2935` están en refs preservadas; la autoridad entre `origin` y `legacy-db` no se elige por inferencia. |
| Existen tres cambios SQL sin commit | worktree `db-dev-consolidation` | `test/fixtures/dev/dev_pagination_cleanup.sql`, `dev_pagination_fixture.sql` y `dev_pagination_validate.sql`; son fixtures de paginación, no cambios de esquema. Se preservan y no se integran automáticamente. |
| Configuración local declarada | `src/main/resources/application.yml`, `infra/files/compose.yaml`, `infra/keycloak/compose.yaml` | Backend espera MinIO `localhost:9000`, ClamAV `localhost:3310`, Keycloak `127.0.0.1:8081`, SQL Server `localhost:1433`; las credenciales solo están en `.env` local y no se registran. |
| Providers no están disponibles en la comprobación inicial | Docker/puertos locales | Docker Desktop Linux engine y puertos `1433/8081/9000/3310/8080/4200` no estuvieron disponibles bajo el sandbox; se requiere comprobación controlada con permisos adecuados. |

## TARGET

Una rama de consolidación por repositorio, trazable a commits recuperados y contratos aprobados, que ejecute los flujos existentes sin depender de cuarentenas. La funcionalidad de soporte seguirá bloqueada cuando falte el binding DB aprobado; el entorno local usará los puertos y nombres oficiales, sin duplicar contenedores ni rotar usuarios institucionales.

## Clase de cambio y alcance de rutas

- Change class: `INFRASTRUCTURE` (consolidación de código existente y verificación operativa; cualquier cambio de comportamiento debe abrir/activar su microfase contractual).
- PRIMARY_VARIABLE: `FUNCTIONAL_RUNTIME_REPRODUCIBILITY`.
- ALLOWED_PATHS: `docs/work-items/P0-E-consolidacion-funcional/`; comprobaciones y reconfiguración local reversible de servicios ya autorizados usando los Compose canónicos, sin borrar volúmenes; scripts locales de arranque/stop/check sin secretos, limitados a detectar puertos, reutilizar servicios y arrancar procesos desde las rutas oficiales. La integración funcional de ramas y cualquier cambio de contrato sigue requiriendo `READY` específico, RED y aprobación de owner.
- FORBIDDEN_PATHS: borrar o modificar cuarentenas/backups/volúmenes; reset destructivo; force-push; merge a ramas oficiales; secretos y `.env`; reinicialización indiscriminada de Keycloak; cambios de esquema SQL sin contrato DB; cambios HTTP/OpenAPI/RBAC/ownership/realtime fuera de un work item hijo; datos reales.
- CONTRACTS: OpenAPI canónico y `BACKEND_GOLDEN_PATH_CONTRACT`; `DB_BASELINE_CONTRACT`/contratos DB liberados; runtime Keycloak; decisión `PROFESSOR_DECISION.md` de MinIO/ClamAV; contratos frontend existentes como consumidores, no como autoridad HTTP.
- PROVIDERS: SQL Server, Keycloak/PostgreSQL, MinIO, ClamAV, Spring Boot y Angular desde sus rutas oficiales.
- EXTERNAL_ENVIRONMENT: ambiente local controlado; fixtures E2E versionados; ningún resultado se marca `PASS` sin provider real cuando sea obligatorio.
- SECURITY_IMPACT: autenticación, issuer/audience, `idUsuario`, roles, ownership, credenciales de prueba y almacenamiento privado; solo evidencia sanitizada.
- OBSERVABILITY_IMPACT: conservar logging JSONL, correlation/trace/span IDs y rutas configuradas; comprobar escritura sin registrar secretos.
- TEST_LEVEL_REQUIRED: unit/contract existentes, `verify`, integración SQL Server/MinIO/ClamAV/Keycloak y E2E funcional cuando estén disponibles; RED congelado antes de cualquier cambio de comportamiento.
- ROLLBACK: ramas/commits reversibles; conservar cuarentenas; revertir solo commits propios; detener servicios iniciados por esta tarea sin tocar contenedores/volúmenes ajenos.
- CONSUMERS: Frontend oficial, Backend oficial, DB oficial, Infra oficial, CI y operadores locales.
- STOP_CONDITIONS: `CONTRACT_CONFLICT`, `TEST_CONTRACT_CONFLICT`, `BLOCKED_BY_MISSING_EVIDENCE`, secreto expuesto, provider no controlado, credenciales faltantes, puerto/proceso ajeno, necesidad de descartar cambios o integración no demostrable.

Lo no listado como Allowed no se modifica. La fase actual autoriza reconciliación de lectura, preflight y corrección local reversible de deriva de puertos/identidad técnica en servicios ya aprobados; la implementación funcional sigue `NOT_READY` hasta resolver los bloqueos indicados.

## Alcance

- Reconciliar HEAD, ramas, remotos observados, stashes, commits exclusivos y cambios locales de Backend, Frontend, DB e Infra.
- Verificar pertenencia real de los commits funcionales y comparar equivalencias por contenido/cherry-pick/squash.
- Describir los tres SQL pendientes y preservar sus modificaciones en el worktree DB.
- Diagnosticar Keycloak/invalid_grant, SQL Server, MinIO y ClamAV usando checks sanitizados.
- Preparar, sin fusionar, ramas/commits de consolidación y PRs solo después de contrato, RED y DoR.
- Ejecutar validaciones técnicas y recorridos E2E disponibles; registrar límites reales.
- Documentar una secuencia única de arranque/detención reutilizando scripts oficiales.

## No alcance

- Elegir unilateralmente `origin` o `legacy-*` como autoridad contractual.
- Cambiar esquema, SP, datos, usuarios institucionales, realm, roles o contratos por inferencia.
- Integrar automáticamente LB-004D D1/F1/B1 ni declarar soporte docente como funcional sin binding DB y E2E.
- Eliminar cuarentenas, respaldos, contenedores, volúmenes o procesos no iniciados por esta tarea.
- Declarar PASS a partir de resultados históricos, mocks o documentación.

## Archivos afectados

- EXISTENTES comprobados: scripts/configuración local y work items de los cuatro repositorios; tres fixtures SQL modificados en `db-dev-consolidation`; tres archivos frontend modificados localmente.
- NUEVOS: `docs/work-items/P0-E-consolidacion-funcional/{PLAN,TEST_PLAN,VALIDATION,CLOSURE}.md` y `scripts/start-local-stack.ps1`, script operativo sin credenciales que solo orquesta rutas oficiales y procesos locales.
- RETIRAR: ninguno.

## Contratos y consumidores afectados

No se autoriza cambio contractual en este parent work item. Se auditan y referencian DOMAIN/HTTP/PERSISTENCE/SECURITY/REALTIME; las diferencias funcionales LB-004D se mantienen bloqueadas o se derivan a hijos con owner/contrato/RED. El Frontend se usa como consumidor comprobable, no como fuente primaria del HTTP.

## Riesgos y dependencias

- La red remota no estuvo disponible para actualizar refs; “último remoto” queda pendiente de `fetch`/`ls-remote` controlado.
- El checkout externo no permite `git status` en el sandbox aunque `git ls-files -m` sí identifica cambios; la evidencia final requiere ejecución con permisos adecuados.
- Frontend y DB tienen remotos con autoridad divergente.
- Keycloak `invalid_grant` puede ser credencial, client, realm, usuario persistido o política; no se reinicializa por diagnóstico.
- La storage foundation tiene decisión aprobada, pero el binding DB de revisión sigue `BLOCKED_BY_DB_CONTRACT`.

## Test plan

Ver `TEST_PLAN.md`. Secuencia obligatoria: preflight de repositorios/configuración → RED/contrato para cualquier cambio → providers locales reales → `GREEN` técnico → smoke/E2E → `VALIDATION` con exit code y límites.

## Rollback y stop conditions

Ante cualquier conflicto de autoridad o falta de evidencia se registra el ID, fuente, alcance, evidencia requerida y owner; se detiene solo la parte dependiente. No se usa reset, force-push, merge final ni eliminación de estado.

## Deuda conocida y validación manual

TD-004/`REVIEW_BINDING`, TD-043/TD-044 y deuda de autoridad frontend/DB permanecen aplicables según la línea base. Los recorridos de estudiante/docente/coordinador/administrador, autenticación real, storage real y persistencia son `NOT_RUN` hasta disponer de ambiente controlado; se registran en `MANUAL_VALIDATION_LEDGER.md` solo con evidencia ejecutada.

## Definition of Ready

**READY para reconciliación de lectura, preflight, validación controlada y script operativo local. NOT_READY para integración funcional/cambios de contrato.** Ya están disponibles refs remotas, DB/Keycloak/MinIO/ClamAV, fixtures y cuentas controladas. Sigue faltando decisión de autoridad Frontend/DB, aprobación de owner y RED congelado por cada cambio de comportamiento. Responsable de resolver: owner de cada repositorio/provider y backend-team para el wiring.
