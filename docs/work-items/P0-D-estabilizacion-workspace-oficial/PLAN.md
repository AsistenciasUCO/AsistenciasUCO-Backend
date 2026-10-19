---
status: active
type: normative
scope: transversal-backend-frontend-db-infra
owner: backend-team / owners of the four official repositories
last-reviewed: 2026-10-18
---

# PLAN — P0-D: Estabilización definitiva y preparación del workspace oficial

## Identidad y objetivo

Esta fase congela el diagnóstico reproducible de los cuatro repositorios oficiales
`AsistenciasUCO` y prepara el workspace para P1 Storage (Blob/MinIO), sin
implementar Blob/MinIO, sin reimplementar UTC v2, sin administrar el esquema DB y
sin fusionar ramas o PR automáticamente.

La variable principal es `OFFICIAL_WORKSPACE_CI_READINESS`. El resultado esperado
es un dictamen por repositorio que distinga `PASS`, `PASS_WITH_FINDINGS`, `FAIL`,
`NOT_RUN`, `BLOCKED` y `NOT_APPLICABLE`, y que no convierta un inventario de
seguridad, un mock o un check omitido en certificación.

## Autoridad y evidencia AS-IS

- Autoridad de gobierno: `AGENTS.md`, `docs/governance/SOURCE_OF_TRUTH.md`,
  `docs/baseline/LINEA_BASE.md`, `DEFINITION_OF_READY.md`,
  `DEFINITION_OF_DONE.md`, `VALIDATION_RUNBOOK.md` y los contratos canónicos.
- Backend oficial: `AsistenciasUCO/AsistenciasUCO-Backend`, rama PR
  `codex/p0-b-canonical-sync-ownership`, SHA `5249051e`; PR #1 abierto, draft,
  base `develop` en `253c3c95`. Su Maven/ArchUnit/CodeQL/Trivy reporta éxito,
  pero Sonar y Dependency Review están rojos.
- Frontend oficial: `AsistenciasUCO/AsistenciasUCO-Frontend`, PR #1 abierto,
  rama `codex/p0-b-frontend-verify`, SHA `e33ca3bd`; base `develop` en
  `bd2120f5`. El run remoto continúa rojo y no fue reejecutado. En un worktree
  P0-D separado basado en `bd2120f5`, los fixtures y el flujo de creación de
  sesión fueron corregidos sin debilitar expectativas; la suite local ejecutó
  389/389 tests exitosos.
- DB oficial: `AsistenciasUCO/AsistenciasUCO-DB`, `develop` limpio en
  `32b70eb`; el worktree P0-D elimina el fallback de credencial y hace
  obligatorio el preflight de Sonar, sin tocar SQL/schema/migraciones. CI
  remoto y la integración local con SQL Server quedaron sin ejecutar.
- Infra oficial: `AsistenciasUCO/AsistenciasUCO-Infra`; `main` está en
  `a544c49`, `develop` en `6ae52da`; `develop` contiene
  `docker-compose.yml`, `components/`, `scripts/` e `infra.ps1`. El worktree
  P0-D añade solo una inspección CI no desplegable; la validación runtime no se
  ejecutó y requiere revisión del owner.
- Sincronización local: `-Status`/`-Apply` del sincronizador P0-B fue ejecutado
  con las cuatro rutas reales. Backend quedó `UP_TO_DATE_DIRTY` y con feature
  divergente; Frontend quedó `MISSING_LOCAL_INTEGRATION_BRANCH`; DB e Infra
  quedaron `UP_TO_DATE`. No hubo checkout, merge, reset, stash, push ni borrado.

## TARGET

1. Los cuatro checkouts usan el `origin` oficial y conservan remotos históricos.
2. Cada PR y cada branch de trabajo tiene base oficial, evidencia de CI y
   rollback practicable.
3. Sonar, Dependency Review y Trivy tienen configuración y semántica explícitas:
   una configuración ausente falla con causa visible; un inventario Trivy no se
   presenta como ausencia de vulnerabilidades.
4. Frontend conserva los contratos de servicios y corrige solamente fixtures o
   adaptadores de prueba que incumplen los DTO requeridos por el AS-IS.
5. DB conserva SQL/schema/freeze manifest y corrige únicamente el pipeline y el
   runner local de credenciales; no se cambia el contrato SQL del owner DB.
6. Infra valida lo que realmente existe y marca `NOT_APPLICABLE` solo cuando no
   hay artefacto; con artefactos runtime presentes, la señal queda pendiente de
   revisión del owner, sin inventar despliegues.
7. El cierre deja un baseline candidate para P1 Storage, no una autorización para
   iniciar P1.

## Clase de cambio y Definition of Ready

- Clase inicial: `CONTRACT_ANALYSIS` + `DOCUMENTATION_ONLY` para el inventario y
  el dictamen.
- Cambios de workflows/runner: `INFRASTRUCTURE`, únicamente después de que el
  owner de cada repositorio confirme los nombres reales de variables/secretos y
  el `TEST_PLAN` conserve el RED observado.
- Corrección Frontend: `BEHAVIOR_CHANGE` solo si el test demuestra cambio de
  comportamiento; si se limita a completar fixtures, queda como test maintenance
  acotado y no cambia el contrato.
- Estado DoR: `READY_WITH_EXTERNAL_GATES_BLOCKED` para auditoría y preparación;
  `NOT_READY` para declarar cierre funcional o `READY_FOR_P1_STORAGE` mientras
  falten Sonar oficial, Dependency Graph, ejecución CI posterior a cambios y las
  integraciones reales requeridas.

## Alcance autorizado

### Backend

- Documentar y corregir la configuración del workflow para que Sonar use valores
  administrados por el repositorio oficial y falle claramente si faltan.
- Documentar la habilitación del Dependency Graph y la lectura correcta de
  Dependency Review.
- Mantener Trivy como inventario report-only hasta que exista decisión versionada
  de baseline, severidades y SLA; registrar findings por componente, severidad,
  CVE, dependencia, versión, fix disponible y estado solo con evidencia SARIF.
- No cambiar endpoints, OpenAPI, dominio, SQL, MinIO, Blob, UTC v2 ni roles.

### Frontend

- Reproducir y reparar el fallo del PR oficial en una copia de trabajo separada.
- Completar fixtures con `horaInicio`/`horaFin` válidos cuando el contrato del
  DTO los exige y con `fechaHoraInicio`/`fechaHoraFin` `LocalDateTime` válidos
  cuando el test ejercita el modo real.
- No debilitar expectativas, no ocultar errores `afterAll`, no cambiar el
  servicio para tolerar respuestas fuera del contrato y no reimplementar UTC v2.

### DB

- Eliminar fallback de contraseña en CI y runner local; exigir secreto/parámetro
  explícito sin registrar su valor.
- Hacer visible el resultado de Sonar: configurarlo con variables/secretos
  oficiales o fallar con causa; no usar `continue-on-error` para el gate.
- Publicar resumen dinámico y artefactos sanitizados de pruebas/manifiesto.
- No tocar `schema/`, migraciones, stored procedures ni contratos SQL.

### Infra

- Añadir solo el workflow/documentación de validación que corresponda a los
  artefactos realmente presentes.
- Si no hay Compose, Dockerfile o manifiesto, reportar `NOT_APPLICABLE`; si
  existen, reportar presencia y requerir revisión autorizada del owner. No
  simular PASS de runtime.
- No cambiar silenciosamente la rama por defecto ni introducir despliegues.

## Rutas permitidas y prohibidas

Permitidas: esta carpeta de work item; workflows y scripts de CI/local runner de
los cuatro repositorios; fixtures/specs Frontend estrictamente necesarios para el
RED observado; documentación de CI/Sonar/Trivy y reportes sanitizados.

Prohibidas: secretos y valores de secretos; `.env` reales; SQL/schema/migraciones;
OpenAPI y contratos públicos; Blob/MinIO/Redis; UTC v2 productivo; cambios de
roles/ownership no demostrados; `git reset --hard`, force-push, merge, borrado de
branches/worktrees/stashes/containers o datos; adaptar el código a documentación
no comprobada.

## Consumidores y dependencias

Consumidores: PR #1 Backend, PR #1 Frontend, ramas `develop` oficiales de DB e
Infra, operadores locales y el futuro trabajo P1 Storage. Dependencias externas:
owner/admin de GitHub para Dependency Graph, owner/admin de SonarCloud para
organización y project key oficiales, `SONAR_TOKEN`, runner con Docker/SQL Server,
ChromeHeadless/Node y evidencia SARIF de Trivy.

## Riesgos, rollback y stop conditions

- Sonar público actualmente visible pertenece a la organización personal
  `alejovh12`; no se inventa una organización ni una project key oficial.
- `SONAR_TOKEN` aparece ausente en el log del PR Backend: `SECURITY_FINDING`,
  configuración de secret ausente o inválida; no se registra ningún valor.
- DB contiene fallback hardcodeado: `SECURITY_FINDING`, credencial embebida en
  workflow/runner; se retira solo en un branch revisable.
- Trivy actual es inventario: `WITH_FINDINGS`/`REPORT_ONLY`, no `PASS` de cero
  vulnerabilidades.
- Toda modificación se revierte por `git revert` del commit propio o eliminando
  el branch no publicado; nunca se descartan cambios ajenos. Los worktrees,
  stashes y remotos históricos se conservan.
- Detener si aparece contrato contradictorio, falta evidencia del owner, se pide
  tocar SQL/contrato público, el checkout está dirty y la operación no es segura,
  o una validación obligatoria no puede ejecutarse.

## Criterios de aceptación

- Existe evidencia versionada de inventario, ramas, PRs, worktrees, stashes y
  remotos de los cuatro repositorios, sin secretos.
- El RED de Backend/Frontend queda reproducido o enlazado a un run oficial, y los
  cambios propuestos no debilitan tests.
- Cada gate remoto tiene estado y causa: `PASS`, `FAIL`, `NOT_RUN`, `BLOCKED` o
  `NOT_APPLICABLE`, con run/check/commit cuando exista.
- El dictamen final contiene exactamente: `OFFICIAL_ORGANIZATION_ONLY`,
  `ALL_FOUR_WORKSPACES_READY`, `BACKEND_CI`, `FRONTEND_CI`, `DB_CI`, `INFRA_CI`,
  `TRIVY_SECURITY_REVIEW`, `P0_STATUS`, `READY_FOR_P1_STORAGE`.
- No se declara `ALL_FOUR_WORKSPACES_READY=true` ni
  `READY_FOR_P1_STORAGE=YES` mientras haya gates obligatorios rojos, no
  ejecutados o bloqueados.

## Estado actual

`P0-D_IN_PROGRESS`; auditoría y documentación autorizadas. Cierre funcional:
`BLOCKED_BY_EXTERNAL_GATES`.
