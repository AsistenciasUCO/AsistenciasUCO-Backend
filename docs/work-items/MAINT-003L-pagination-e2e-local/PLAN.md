# MAINT-003L — Preparación E2E local de docentes para fixtures de paginación

## Identidad y objetivo

- Fecha / autor o rol / base Git: 2026-10-11 / backend-team / `develop`.
- Objetivo y criterios de aceptación: preparar, de forma limitada y reversible, tres identidades DOCENTE de desarrollo y los horarios faltantes para `FIX-PAG-300`, `FIX-PAG-1000` y `FIX-PAG-5000`, sin tocar `docente.prueba`, matrículas, estudiantes, grupos, sesiones ni el contrato HTTP. La validación real debe demostrar `idUsuario`, `issuer`, audiencia, rol `DOCENTE`, titularidad, cardinalidades 300/1000/5000 y aislamiento de sesiones v2.
- Skills y fuentes autoritativas: `uco-arquitectura`, `uco-baseline`, `uco-seguridad`, `uco-persistencia`, `uco-testing`; [runtime security](../../security/runtime-security-provider-architecture.md), [Keycloak](../../security/keycloak-identity-provider.md), [Golden Path](../../contracts/BACKEND_GOLDEN_PATH_CONTRACT.md), [OpenAPI](../../contracts/openapi/openapi-golden-path.yaml), [DB baseline](../../contracts/external/db/DB_BASELINE_CONTRACT.md).

## AS-IS y evidencia

| Hecho | Archivo + símbolo / contrato + versión | Evidencia y límites |
|---|---|---|
| Los tres grupos existen con 300/1000/5000 matrículas y titulares `DE000003…` | `test/fixtures/dev/dev_pagination_fixture.sql` del repo DB; consulta real 2026-10-11 | SQL Server Developer `sql_server_asistencias` / `gestionasistenciadb`; lectura real, sin reconstrucción |
| Solo `FIX-PAG-300` tenía un horario | `dbo.Horario` + `uv_horario_docente` | El registro existente tiene UUID no reservado y proviene del entorno local; se conserva, no se reasigna |
| Las tres cuentas solicitadas no existen | Keycloak Admin REST, realm `asistencias-uco` | Consulta real 2026-10-11; no se modificaron usuarios durante el diagnóstico |
| `docente.prueba` no es titular de los fixtures | `get-test-token.ps1` + SQL `Usuario→Docente` | `idUsuario` válido pero distinto de los tres titulares sintéticos; no se cambia |
| El runtime exige `idUsuario`, issuer exacto, audiencia `asistencias-api` y roles del client API | `docs/security/runtime-security-provider-architecture.md` | Contrato vigente; no se amplían permisos |

## TARGET

- Script DEV Keycloak idempotente y de alcance exacto para `docente.paginacion300`, `docente.paginacion1000` y `docente.paginacion5000`.
- Script/fixture DB idempotente que crea solo horarios faltantes para grupos sintéticos, con día de catálogo y UUID reservado determinista.
- Limpieza que elimina únicamente horarios reservados de este work item y permite que el limpiador general de fixtures quite horarios de grupos sintéticos antes de borrar grupos.
- Evidencia sanitizada de diagnóstico, backup, cambios de cardinalidad, tokens sin imprimir y HTTP/Angular con estado real.

## Clase de cambio y alcance de rutas

- Change class: `INFRASTRUCTURE`.
- PRIMARY_VARIABLE: `LOCAL_PAGINATION_E2E_FIXTURE_READINESS`.
- ALLOWED_PATHS: `infra/keycloak/scripts/seed-pagination-e2e-users.ps1`, `infra/keycloak/scripts/get-test-token.ps1`, `infra/keycloak/scripts/diagnose-idusuario.ps1`, `infra/keycloak/.env.example`, `scripts/e2e/prepare-pagination-fixture.ps1`, work item `MAINT-003L-*`; scripts de fixture DEV equivalentes en el checkout local del repo DB.
- FORBIDDEN_PATHS: `src/main/**`, `src/test/**`, `pom.xml`, OpenAPI, controllers, Domain/Application, realm bootstrap, permisos/RBAC, datos de usuarios no E2E, estudiantes, matrículas, grupos, asistencias, sesiones y horarios de grupos reales.
- CONTRACTS: solo consumo/validación; no cambio de HTTP, persistencia ni seguridad.
- PROVIDERS: Keycloak actual y SQL Server local existente; no se crean contenedores.
- EXTERNAL_ENVIRONMENT: `sql_server_asistencias` / `gestionasistenciadb`, Keycloak local y frontend real si está disponible.
- SECURITY_IMPACT: solo cuentas nuevas de desarrollo, cada una con client role `DOCENTE`; secretos y JWT nunca se versionan ni imprimen.
- OBSERVABILITY_IMPACT: ninguno.
- TEST_LEVEL_REQUIRED: PowerShell parse, SQL Server real, Keycloak Admin/OIDC real, HTTP real; Angular manual si el frontend está disponible.
- ROLLBACK: backup validado de la DB; limpieza de horarios reservados; eliminación explícita de las tres cuentas E2E por username; no tocar otras identidades.
- CONSUMERS: frontend docente, endpoints Golden Path y `GET /api/v1/estudiantes` con cuenta `COORDINADOR` separada.
- STOP_CONDITIONS: password local ausente, mismatch de `idUsuario`, grupo/docente ambiguo, DB no Developer/no esperada, contrato contradictorio, o backend/frontend no disponible para un gate que se pretenda declarar PASS.

Lo no listado como Allowed no se modifica.

## Alcance

Diagnóstico de solo lectura, preparación reversible de horarios faltantes, script DEV de provisioning limitado, validación de claims/roles/ownership y documentación de resultados reales.

## No alcance

No reconstruir la DB, recargar estudiantes, modificar matrículas o sesiones, cambiar Keycloak realm/bootstrap, tocar `docente.prueba`, ampliar `DOCENTE`, modificar endpoints, desplegar, hacer merge o almacenar credenciales/JWT.

## Archivos afectados

### Existentes

- `infra/keycloak/scripts/lib/KeycloakAdmin.psm1`
- `infra/keycloak/scripts/get-test-token.ps1`
- `infra/keycloak/scripts/diagnose-idusuario.ps1`
- `infra/keycloak/.env.example`
- fixture DEV de paginación en el repo DB local.

### Nuevos

- `infra/keycloak/scripts/seed-pagination-e2e-users.ps1`
- `scripts/e2e/prepare-pagination-fixture.ps1`
- `docs/work-items/MAINT-003L-pagination-e2e-local/*`

### Retirar

Ninguno.

## Contratos y consumidores afectados

DOMAIN: ninguno. HTTP: solo lectura de contratos existentes. PERSISTENCE: DML de fixture DEV, sin esquema ni SP. SECURITY: identities nuevas con el mismo contrato vigente (`idUsuario`, issuer, audiencia, `resource_access[asistencias-api].roles`). REALTIME: ninguno.

## Riesgos y dependencias

- Las passwords deben ser proporcionadas localmente en `infra/keycloak/.env`; sin ellas el provisioning queda `NOT_RUN`.
- El horario preexistente de `FIX-PAG-300` no tiene ID reservado por este work item; se conserva y se reporta como reutilizado.
- El endpoint de directorio paginado requiere una cuenta `COORDINADOR`; no se concede ese rol a docentes.
- La pantalla Angular necesita un checkout/servidor frontend externo a este backend; su ausencia no se convierte en PASS.

## Test plan

Ver `TEST_PLAN.md`.

## Rollback y stop conditions

Antes de escribir: backup `COPY_ONLY + CHECKSUM` y verificación del backup. Si falla cualquier precheck, no escribir. Para rollback, borrar horarios con IDs reservados y eliminar únicamente usernames E2E creados por este work item; restaurar DB solo si el rollback de datos lo exige.

## Deuda conocida y validación manual

No se crea deuda técnica. Registrar la validación manual en `VALIDATION.md`; no inventar un MV PASS para frontend ausente.

## Definition of Ready

`READY` para scripts y preparación DB: requisitos, grupos, docentes, esquema de `Horario`, Keycloak y contratos comprobados localmente; `NOT_READY` para el gate final de login/HTTP hasta recibir passwords locales y tener backend activo. Responsable: backend-team.
