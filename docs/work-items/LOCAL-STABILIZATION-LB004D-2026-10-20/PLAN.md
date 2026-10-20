# PLAN — LOCAL-STABILIZATION-LB004D-2026-10-20: estabilización local y preparación E2E

## Identidad y objetivo

- Fecha / rol / base Git: 2026-10-20 / agente backend / `codex/local-lb004d-stabilization` sobre `origin/develop` `5c06087d948bd097387aaf162f4abdfd931a6fc4`.
- Objetivo: dejar una ruta reproducible y segura para validar localmente la capacidad LB-004D, el fixture CI de DB y la preparación E2E Angular, preservando datos, secretos, ramas y PRs existentes.
- PRIMARY_VARIABLE: `LOCAL_LB004D_E2E_READINESS`.
- Criterios de aceptación: ramas canónicas auditadas y sincronizadas sin merge a `develop`; fixture CI LB-004D idempotente y solo de pruebas; `.pyc` no versionado; stacks MinIO/ClamAV/Keycloak/SQL documentados con salud y límites; validaciones ejecutadas o marcadas `NOT_RUN` con causa; ningún volumen ni dato existente eliminado.
- Skills y fuentes: `uco-baseline`, `uco-arquitectura`, `uco-contratos`, `uco-testing`, `uco-seguridad`, `uco-files`, `uco-persistencia`; `AGENTS.md`, `LINEA_BASE.md`, `SOURCE_OF_TRUTH.md`, contratos LB-004, PRs #29/#39/#2 y ramas remotas observadas el 2026-10-20.

## AS-IS y evidencia

| Hecho | Archivo / objeto | Evidencia y límites |
|---|---|---|
| Backend local tiene cambios de usuario no relacionados | checkout principal `codex/p0-sec-001-coordinator-ownership` | Se preservan: `.env.example`, scripts Keycloak/E2E y work item `MAINT-003L`; no se edita este checkout. |
| B1 está abierto y no sincronizado con el `develop` remoto actual | PR #29 / rama `jose-valencia/lb-004d-b1-support-binding` | Head `54d191d`; `origin/develop...origin/B1 = 3 5`; PR declara integración DB pendiente y Angular E2E `NOT_RUN`. |
| D1 está abierto y no sincronizado con el `develop` remoto actual | PR #39 / rama `codex/lb-004d-d1-archivo-soporte` | Head `b698c76`; rama D1 es limpia; fixture requerido por `test_lb004d_support_binding.sql` no se invoca desde CI. |
| F1 está abierto con base antigua y checks de frontend preexistentes | PR #2 / rama `jose-valencia/lb-004d-f1-support-binding` | Head `06f70c4`; `mergeable:false`; el cuerpo reporta errores de spec previos, que no se deben maquillar. |
| SQL local no tiene binding de archivos/revisiones | SQL Server `gestionasistenciadb`, tablas `ArchivoSoporte` y `SolicitudRevisionAsistencia` | Consulta read-only observó 0 filas; esto no autoriza borrar objetos MinIO ni diseñar schema. |
| Hay dos familias MinIO/ClamAV | Docker Desktop | Stack nuevo usa loopback y volúmenes `asistencias-*`; stack legacy usa puertos no limitados a loopback y volúmenes `asistenciasuco_*`; ambos inventarios se preservan. |
| El fixture CI LB-004D exige actores/grupos adicionales | `test/test_lb004d_support_binding.sql` en DB D1 | Busca estudiantes en dos grupos, dos docentes y usuarios con prefijo/roles; los seeds productivos no cubren todo el fixture. |

## TARGET

Una preparación local reproducible, reversible y fail-closed: cada repositorio conserva su rama/PR funcional; las correcciones de CI viven en un worktree/branch auxiliar; el fixture de integración se aplica explícitamente a una base efímera de CI; MinIO es privado y backend-only; los objetos y volúmenes existentes quedan intactos; la certificación externa no se presume.

## Clase de cambio y alcance de rutas

- Change class: `INFRASTRUCTURE` + `REFACTOR` de higiene de artefactos + fixture de pruebas; no `CONTRACT_CHANGE`, no `PERSISTENCE_MIGRATION`.
- ALLOWED_PATHS:
  - Este work item en `docs/work-items/LOCAL-STABILIZATION-LB004D-2026-10-20/`.
  - DB D1 auxiliar: `.github/workflows/ci.yml`, `test/prepare_lb004d_fixture.ps1`, `test/fixtures/lb004d_ci_fixture.sql` y documentación/validación asociada.
  - Backend B1 auxiliar: `.gitignore` y el `.pyc` versionado bajo `scripts/e2e/files/__pycache__/`.
  - Frontend F1 auxiliar: solo cambios necesarios para ejecutar/diagnosticar los checks existentes; no se cambia el contrato de soporte ni se silencian fallos preexistentes.
  - Scripts de auditoría local nuevos dentro de este work item o rutas E2E ya existentes, sin secretos.
- FORBIDDEN_PATHS: checkout principal sucio; `.env` con valores reales; SQL productivo/schema contractual; `develop` remoto; merges, force-push, rebase destructivo, cierre/merge de PR; volúmenes/containers de datos existentes; `infra` oficial salvo lectura.
- CONTRACTS: contratos HTTP/OpenAPI, DB y ownership LB-004 permanecen congelados; no se agrega endpoint, columna ni permiso académico.
- PROVIDERS: MinIO/ClamAV, SQL Server y Keycloak solo se verifican con configuración existente; cualquier proveedor no disponible queda `NOT_RUN`.
- EXTERNAL_ENVIRONMENT: Docker Desktop/WSL2, repositorios GitHub y clones locales de backend/DB/frontend; cambios remotos solo push a branch auxiliar si la validación local y el alcance lo autorizan.
- SECURITY_IMPACT: positivo en higiene de `.pyc`, detección de exposición de puertos/credenciales y preservación de least privilege; no se rotan secretos sin owner/confirmación del consumidor activo.
- OBSERVABILITY_IMPACT: solo evidencia sanitizada; no se guardan tokens, contraseñas, payloads ni logs crudos con secretos.
- TEST_LEVEL_REQUIRED: unit/fixture script, integración SQL Server controlada, backend `verify`, frontend `verify` cuando el entorno lo permita, Docker health/readiness y E2E real `NOT_RUN` si faltan credenciales/fixture contractual.
- ROLLBACK: revertir únicamente commits de branches auxiliares o borrar worktrees temporales después de preservar evidencia; no tocar volúmenes ni usar reset destructivo.
- CONSUMERS: CI DB, tests LB-004D, backend B1, frontend F1 y operadores locales; ningún consumidor público cambia.
- STOP_CONDITIONS: `CONTRACT_CONFLICT`, `TEST_CONTRACT_CONFLICT`, `BLOCKED_BY_MISSING_EVIDENCE`, secreto encontrado, fixture que requiera cambiar schema/seed productivo, pérdida o ambigüedad de datos, gate rojo no atribuible a este cambio.

## Alcance

1. Inventariar y reconciliar refs Git de los tres repositorios sin modificar ramas LB-004D originales.
2. Proveer fixture CI LB-004D explícito, aislado e idempotente, fuera de los seeds productivos.
3. Retirar el `.pyc` versionado del branch auxiliar y reforzar ignore.
4. Ejecutar validaciones proporcionales y documentar skips, límites y evidencia.
5. Auditar Docker/MinIO/ClamAV/Keycloak/SQL/Redis con decisión conservadora; detener solamente contenedores duplicados que este work item haya iniciado y que no sean el stack principal.

## No alcance

- Implementar attach, binding docente, `GET /docente/reclamos`, migración JPA, schema SQL o cambios OpenAPI.
- Mezclar o cerrar PRs, hacer merge a `develop`, borrar ramas, volúmenes, objetos, usuarios o credenciales.
- Reparar silenciosamente errores preexistentes del frontend o convertir una integración ausente en `PASS`.

## Archivos afectados

### Existentes

- `docs/work-items/LB-004-stateless-serverless-readiness/*` como autoridad congelada.
- DB: `.github/workflows/ci.yml`, `test/test_lb004d_support_binding.sql` y scripts de fixture existentes.
- Backend: `.gitignore` y `scripts/e2e/files/__pycache__/files_storage_e2e.cpython-314.pyc` en el branch B1 auxiliar.

### Nuevos

- Este work item y fixture/script CI LB-004D, solo en branches auxiliares.

### Retirar

- Solo el `.pyc` versionado indicado, mediante `git rm` en la rama auxiliar; no otros artefactos.

## Contratos y consumidores afectados

DOMAIN / HTTP / PERSISTENCE / SECURITY / REALTIME: `NO CHANGE`. El fixture consume el contrato DB existente para pruebas; no altera su shape. El contrato de storage conserva bucket privado, backend-only, ClamAV requerido y `REVIEW_BINDING: BLOCKED_BY_DB_CONTRACT`.

## Riesgos y dependencias

- Los remotos avanzaron respecto de los bases de PR; se requiere merge normal en branches auxiliares y resolución explícita de conflictos.
- CI puede necesitar SQL Server efímero y `sqlcmd`; si no hay entorno, queda `NOT_RUN` con comando reproducible.
- ClamAV sale `137` en el inventario local; no se interpreta como pérdida de datos ni se cambia la política fail-closed sin evidencia.
- Las diferencias MinIO no se pueden reconciliar automáticamente porque SQL actual no muestra bindings.

## Test plan

Ver `TEST_PLAN.md`. La secuencia es REQUIREMENT → CONTRACT → TEST_PLAN → RED/GREEN del fixture → VALIDATE; los tests existentes no se editan para forzar PASS.

## Rollback y stop conditions

No se hace rollback sobre datos o ramas protegidas. Un conflicto de contrato o evidencia detiene solo el alcance afectado y se registra con `BLOCKED_BY_MISSING_EVIDENCE` o `CONTRACT_CONFLICT`.

## Deuda conocida y validación manual

- `TD-004` sigue `PARTIAL`; el E2E docente no se cierra mientras el contrato DB de binding esté bloqueado.
- Validación manual Docker/Keycloak/Angular se registra como `PASS`, `FAIL` o `NOT_RUN`, nunca como inferencia.

## Definition of Ready

`READY` para diagnóstico, fixture de pruebas y saneamiento de artefacto; `NOT_READY` para attach/ownership docente, cambios de schema, contrato HTTP o cierre de LB-004. Fuentes, PRs, refs y límites fueron identificados; cada cambio de código debe quedar en branch auxiliar y conservar evidencia.
