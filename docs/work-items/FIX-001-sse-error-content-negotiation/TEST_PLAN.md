---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-14
---

# TEST_PLAN — FIX-001: errores de `/api/v1/realtime/stream` con `Accept: text/event-stream`

## Fuentes

[PLAN](PLAN.md); OpenAPI canónico `/api/v1/realtime/stream` (`400 → BadRequest`, `403 → Forbidden`, `ApiErrorResponse` en `application/json`); evidencia E2E SEC-BASELINE-002 (2026-10-14).

## Behavioral Matrix

| ID | Requirement | Scenario | Precondition | Action | Observable | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|---|---|---|
| F1-01 | 403 contractual en canal SSE | Grupo ajeno | `canDocenteAccessGrupo(usuario, grupoAjeno)=false` | `GET /stream?grupoId=<ajeno>`, `Accept: text/event-stream` | status, Content-Type, `$.status`, `$.code`, `$.path`, async, suscriptores | 403, JSON, `FORBIDDEN`, sin async, 0 suscriptores | Negociación contra Accept (500/ServletException); stream abierto antes de autorizar | MockMvc slice |
| F1-02 | 400 contractual con cuerpo | `grupoId` ausente | — | `GET /stream`, `Accept: text/event-stream` | status, Content-Type, `$.code`, `$.details[0]` | 400, JSON, `VALIDATION_ERROR`, `grupoId`/`FIELD_REQUIRED` | 400 sin cuerpo vía `DefaultHandlerExceptionResolver` | MockMvc slice |
| F1-03 | 400 contractual con cuerpo | `grupoId` no UUID | — | `GET /stream?grupoId=no-es-uuid`, mismo Accept | status, Content-Type, `$.code`, `$.details[0]` | 400, JSON, `INVALID_REQUEST`, `grupoId`/`FIELD_INVALID_UUID` | Ídem F1-02 | MockMvc slice |
| F1-04 | Contrato de éxito intacto | Grupo propio | `canDocenteAccessGrupo(usuario, grupoPropio)=true` | `GET /stream?grupoId=<propio>`, mismo Accept | async, status, Content-Type, suscriptores | async iniciado, 200, `text/event-stream`, 1 suscriptor | Fix que fuerce JSON también en el éxito | MockMvc slice |

| F1-05 | 405 conserva `Allow` | Método no soportado | `HttpRequestMethodNotSupportedException(DELETE, [GET, POST])` y variante sin métodos | `handleMethodNotSupported` | status, header `Allow`, Content-Type, `$.status` | 405, `Allow: GET, POST` (vacío en la variante), `application/json` | Reconstrucción de la respuesta que pierda `Allow` o el Content-Type fijado | Unitario del handler |

F1-05 es una guardia de regresión añadida en la revisión previa al PR (no RED: la ruta 405 ya conservaba `Allow`); vive en `GlobalExceptionHandlerFrameworkErrorsTest` y no modifica el test RED.

Regresión: suites existentes `GlobalExceptionHandlerTest`, `GlobalExceptionHandlerFrameworkErrorsTest`, `*ErrorHttpTest`, controllers y `RbacSecurityFilterChainTest` dentro de `verify`.

## Integration Requirement

- REAL_PROVIDER_REQUIRED: NO
- ENVIRONMENT: JVM local (MockMvc standalone).
- MOCK_SUFFICIENT: YES para la negociación de contenido; solo `InstitutionalScopePort` y el catálogo de mensajes son mocks. Controller, `LocalSseRealtimeStreamGateway`, `ReactorRealtimeAdapter` y `GlobalExceptionHandler` son reales.
- WHY: el defecto es de la capa MVC (resolución de excepciones y converters), reproducible sin DB ni IdP. El E2E real con JWT y Tomcat queda como validación manual.

## Negative / Boundary Coverage

F1-01..F1-03 son negativos; F1-04 es el borde positivo que impide forzar JSON en el éxito.

## Side Effects / Rollback

F1-01 comprueba que no se crea suscripción (`activeSubscribersCount()==0`). F1-04 cierra el `AsyncContext` al terminar para cancelar la suscripción.

## Pruebas RED requeridas

`src/test/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/realtime/sse/controller/RealtimeStreamErrorHttpTest.java`.

Ejecución RED contra `develop` `4082736` sin fix (2026-10-14), `./mvnw.cmd -B -ntp -q test -Dtest=RealtimeStreamErrorHttpTest -Djacoco.skip=true`. El proceso salió con código ≠0 (`Tests run: 4, Failures: 2, Errors: 1`):

- F1-01: `ServletException: Request processing failed: …ForbiddenException` (en contenedor real = 500), precedida de `Failure in @ExceptionHandler …#handleApplication … HttpMediaTypeNotAcceptableException: No acceptable representation`.
- F1-02: `AssertionError: Content type not set` (400 sin cuerpo), tras `Failure in @ExceptionHandler …#handleMissingRequestParameter`.
- F1-03: `AssertionError: Content type not set`, tras `Failure in @ExceptionHandler …#handleMethodArgumentTypeMismatch`.
- F1-04: PASS (el éxito ya funcionaba; actúa como guardia de regresión).

El fallo es causal (negociación de contenido del advice) y no ambiental.

## RED_SNAPSHOT

| Campo | Valor |
|---|---|
| Base commit | `4082736b3607a7ee17770892764f1dcc0af381bb` (`develop`) |
| Archivos de test | `src/test/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/realtime/sse/controller/RealtimeStreamErrorHttpTest.java` |
| SHA-256 o commit del estado RED | `d1ac4604f9dc277c3c5ec7c92edd37aa3c96230332be43b9a07aecff271459fa` |
| Comando | `./mvnw.cmd -B -ntp -q test -Dtest=RealtimeStreamErrorHttpTest -Djacoco.skip=true` |
| Exit code | ≠0 (`Tests run: 4, Failures: 2, Errors: 1`) |
| Fallo esperado | `HttpMediaTypeNotAcceptableException` dentro del advice → 403 escalado a 500; 400 sin cuerpo |

## Congelación

El test no se modificó después del RED (ver hash en VALIDATION). Rol tester e implementador ejecutados por el mismo agente en esta sesión: la independencia es solo conceptual y la debe confirmar el auditor/revisor del PR.

## Integración y E2E

No aplica a la suite normal. Validación manual pendiente: E2E real SEC-BASELINE-002 tras integrar.

## Falsos positivos que deben evitarse

- Aceptar `status().isForbidden()` sin comprobar el cuerpo: el 400 ya tenía status correcto y faltaba el cuerpo.
- Mockear el gateway para que lance la excepción: se usa el gateway real con `InstitutionalScopePort` mockeado para ejercitar la regla de ownership.
- Forzar JSON en todas las respuestas del endpoint: F1-04 lo detecta.
