# TEST PLAN — P0-D

## Requisitos verificables

| ID | Requisito | Evidencia esperada | Resultado inicial |
|---|---|---|---|
| P0D-R1 | Los cuatro repositorios tienen `origin` oficial y estado Git inventariado. | Remotes, branch, SHA, dirty state, worktrees y stashes. | `PASS` para inventario; Frontend no tiene branch local `develop`. |
| P0D-R2 | Sonar/Dependency Review no se omiten silenciosamente. | Workflow + check remoto + configuración admin. | `FAIL/BLOCKED`: Sonar personal/secret ausente; Dependency Review rojo. |
| P0D-R3 | Trivy se interpreta como inventario hasta decidir umbral. | SARIF/log + matriz de findings + decisión de owner. | `WITH_FINDINGS`, no certifica cero CVEs. |
| P0D-R4 | Frontend pasa su suite oficial sin debilitar tests. | Run CI y log de 389 tests, sin errores `afterAll`. | `PASS_LOCAL`: 389/389; PR remoto sigue sin reejecutarse. |
| P0D-R5 | DB CI exige secretos externos y muestra conteo real. | Workflow sin fallback, resumen/artefactos, run verificable. | `PREPARED/BLOCKED_REMOTE`: cambios locales listos; CI remoto e integración no ejecutados. |
| P0D-R6 | Infra valida solo artefactos presentes. | Run y clasificación `PASS`/`NOT_APPLICABLE`. | `NOT_RUN_REMOTE; PRESENT_REQUIRES_OWNER_REVIEW`: hay runtime versionado y no se ejecutó. |
| P0D-R7 | El baseline P1 no autoriza implementación Storage. | VALIDATION/CLOSURE y dictamen final. | `BLOCKED` hasta gates externos. |

## Estrategia REQUIREMENT → CONTRACT → TEST

- REQUIREMENT: los repositorios oficiales y sus consumidores deben poder
  reproducirse y auditarse sin destruir estado local.
- CONTRACT: `SOURCE_OF_TRUTH`, `LINEA_BASE`, contratos Golden Path/OpenAPI,
  workflow oficial y DTOs observados; el contrato no se deduce de una anotación o
  un mock.
- RED: PR Backend #1 tiene Sonar/Dependency Review rojos; PR Frontend #1 ejecutó
  389 tests y falló en `DeanGrupoDetalleComponent`, con `undefined.slice` en
  `CourseService` y `LocalDateTime` inválido en `SessionService` durante
  `afterAll`. En el worktree P0-D basado en `develop`, la suite local quedó en
  389/389. DB/Infra tienen gates remotos no ejecutados y restricciones de
  configuración/owner, no un RED funcional que deba ocultarse.
- GREEN: solo se acepta después de ejecutar el test/CI correspondiente en el SHA
  modificado y conservar el log/artefacto sanitizado.
- VALIDATE: `git diff --check`, validación YAML/script, suite Frontend, Maven/CI
  Backend aplicable, SQL Server/DB controlada si está disponible, y revisión de
  checks remotos. `NOT_RUN` no es PASS.

## RED_SNAPSHOT

- Backend PR #1: head `5249051e`; run `39178504360`; `Backend Quality Gate`
  falló en Sonar porque el workflow usa organización/project key personales y el
  `SONAR_TOKEN` está ausente en el entorno del run. Dependency Review falló en
  run `39178504399`; el check requiere Dependency Graph habilitado.
- Frontend PR #1: head `e33ca3bd`; run `39177999381`, job `117824572538`;
  ejecución `51 of 389 (1 FAILED) DISCONNECTED`. Primer fallo observado:
  `DeanGrupoDetalleComponent` esperaba `MAT-301`/una sesión y obtuvo
  `undefined`/`0`; después ocurrieron `undefined.slice` en
  `src/app/core/services/course.service.ts` y `Formato LocalDateTime no
  soportado: undefined` en `src/app/core/services/session.service.ts`. El
  worktree P0-D local basado en `bd2120f5` ejecutó 389/389 exitosos; el PR
  remoto no fue actualizado.
- DB: el workflow de `develop` fue inspeccionado y el worktree P0-D prepara el
  gate sin fallback, pero no se ejecutaron SQL Server, Docker ni CI remoto.
- Infra: `develop` contiene `docker-compose.yml`, `components/`, `scripts/` e
  `infra.ps1`; se añadió una clasificación no desplegable y la validación
  runtime queda `NOT_RUN/BLOCKED_BY_OWNER_REVIEW`.

## Matriz de gates

| Gate | Backend | Frontend | DB | Infra |
|---|---|---|---|---|
| Build/unit | Maven ejecutó y pasó antes del gate Sonar | local `389/389`; PR remoto rojo | `NOT_RUN` local | `NOT_RUN` runtime |
| Contract/architecture | ArchUnit `PASS` | API tests sujetos a suite | T-SQL dinámico pendiente | `NOT_APPLICABLE` |
| Sonar | `FAIL/BLOCKED` | `NOT_RUN`/configuración por verificar | `BLOCKED` por secretos/variables oficiales no disponibles | `NOT_APPLICABLE` para este workflow de inspección |
| Dependency Review | `FAIL/BLOCKED` | por verificar | por verificar | por verificar |
| Trivy | `WITH_FINDINGS` report-only | por verificar | por verificar | pendiente workflow |
| Integration/E2E | `NOT_RUN` sin DB/fixtures autorizados | `NOT_RUN` para browser real aparte del CI | `NOT_RUN` local si no hay SQL Server | `NOT_RUN`; sin despliegue ni contenedores |
