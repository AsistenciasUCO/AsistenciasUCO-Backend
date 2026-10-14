---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-10-14
---

# PLAN — FIX-001: errores de `/api/v1/realtime/stream` con `Accept: text/event-stream`

## Identidad y objetivo

- Fecha / rol / base Git: 2026-10-14 / backend-team (agente) / rama `jose-valencia/fix-001-sse-error-content-negotiation` desde `develop` `4082736`.
- Origen: E2E real SEC-BASELINE-002 en la rama experimental `jose-valencia/experiment-security-baseline-mvc`; el código involucrado es idéntico al de `develop` `4082736`.
- Objetivo: que los errores del canal SSE respondan el status contractual con cuerpo `ApiErrorResponse` JSON aunque el cliente solo acepte `text/event-stream` (lo que envía un `EventSource` del navegador).
- Criterios de aceptación:
  1. `GET /api/v1/realtime/stream?grupoId=<grupo ajeno>` + `Accept: text/event-stream` → **403**, `code=FORBIDDEN`, cuerpo JSON, sin abrir stream.
  2. `GET /api/v1/realtime/stream` sin `grupoId` + mismo Accept → **400** con cuerpo JSON y detalle del campo `grupoId`.
  3. El caso exitoso (grupo propio) sigue abriendo el stream `text/event-stream`.
  4. `./mvnw.cmd -B -ntp verify` verde con gates JaCoCo LINE ≥80 % / BRANCH ≥70 %.
- Skills/fuentes: uco-realtime, uco-contratos, uco-testing, uco-seguridad (solo lectura: no se toca RBAC ni ownership); [OpenAPI canónico](../../contracts/openapi/openapi-golden-path.yaml) (`/api/v1/realtime/stream`, `components/responses/{BadRequest,Forbidden}`).

## AS-IS y evidencia

| Hecho | Archivo + símbolo / contrato | Evidencia y límites |
|---|---|---|
| El endpoint SSE declara `produces = text/event-stream` | `RealtimeEventsController#subscribe` | Spring registra `text/event-stream` como único tipo producible del handler. |
| La titularidad se valida al suscribirse y lanza `ForbiddenException` | `LocalSseRealtimeStreamGateway#subscribe` vía `InstitutionalScopePort#canDocenteAccessGrupo` | Correcto; se lanza antes de construir el `Flux` (no se abre stream ni se entregan datos). |
| El advice devuelve `ResponseEntity<ApiErrorResponse>` sin Content-Type fijo | `GlobalExceptionHandler#buildResponse` | Spring negocia contra `Accept: text/event-stream`; ningún converter produce JSON en ese tipo → `HttpMediaTypeNotAcceptableException` ("Failure in @ExceptionHandler … No acceptable representation"). |
| Resultado observado 403 | E2E SEC-BASELINE-002 | La excepción original no queda resuelta → **500** en el contenedor (en MockMvc: `ServletException`). |
| Resultado observado 400 (sin `grupoId`) | mismo mecanismo | `DefaultHandlerExceptionResolver` resuelve después con `sendError(400)`: status correcto, **sin cuerpo** `ApiErrorResponse`. Igual para `grupoId` no UUID. |
| Contrato | OpenAPI `/api/v1/realtime/stream`: `400 → BadRequest`, `403 → Forbidden`, ambos `application/json` `ApiErrorResponse` | 500 y 400 sin cuerpo violan el contrato. |
| Precedente en la cadena de seguridad | `SecurityErrorResponseWriter#write` | Ya escribe JSON con Content-Type fijo para 401/403/503, independiente de `Accept`. |

## TARGET

Toda respuesta de `GlobalExceptionHandler` fija `Content-Type: application/json`. Con el Content-Type preestablecido, `AbstractMessageConverterMethodProcessor` no negocia contra `Accept` y escribe el `ApiErrorResponse` con el converter JSON. Status, code, message, path, correlationId y details no cambian.

## Clase de cambio y alcance de rutas

- Change class: BEHAVIOR_CHANGE (corrección de defecto para cumplir el contrato vigente; sin cambio contractual).
- PRIMARY_VARIABLE: Content-Type de las respuestas de error del `GlobalExceptionHandler`.
- ALLOWED_PATHS: `src/main/java/.../adapter/primary/controller/error/GlobalExceptionHandler.java`; `src/test/java/.../adapter/primary/realtime/sse/controller/RealtimeStreamErrorHttpTest.java` (NUEVO); `src/test/java/.../adapter/primary/controller/error/GlobalExceptionHandlerFrameworkErrorsTest.java` (regresión 405/`Allow`); `docs/work-items/FIX-001-sse-error-content-negotiation/**`; `docs/work-items/README.md` (índice).
- FORBIDDEN_PATHS: OpenAPI y contratos, `SecurityConfig`/RBAC, `InstitutionalScopePort` y reglas de ownership, `LocalSseRealtimeStreamGateway`, `RealtimeEventsController` (contrato de éxito), `pom.xml`, reglas ArchUnit, DB/frontend.
- CONTRACTS: ninguno cambia; se cumple el vigente.
- PROVIDERS: ninguno.
- EXTERNAL_ENVIRONMENT: ninguno para la suite normal; E2E real queda como validación manual.
- SECURITY_IMPACT: neutro. La denegación ya era segura (no se abre stream); ahora además se devuelve el 403 contractual. No cambia quién puede suscribirse.
- OBSERVABILITY_IMPACT: desaparece el WARN espurio `Failure in @ExceptionHandler`; el log `Handled application exception. status=403` ya existente pasa a reflejar el status final real.
- TEST_LEVEL_REQUIRED: slice HTTP MockMvc (standalone) con controller, gateway local y advice reales; `verify` completo.
- ROLLBACK: revertir el commit del fix; el RED vuelve a fallar.
- CONSUMERS: frontend (EventSource) y cualquier cliente que envíe un Accept no JSON a un endpoint con errores JSON.
- STOP_CONDITIONS: si un test existente exige un Content-Type de error distinto de JSON → `TEST_CONTRACT_CONFLICT`.

## Alcance

Fijar `application/json` en `buildResponse` y conservar los headers al reconstruir la respuesta 405 (`Allow`). Test RED HTTP y evidencia.

## No alcance

RBAC, ownership, filtro por grupo, heartbeat, formato SSE de éxito, cambios OpenAPI, `ResponseStatusException` de `/api/v1/realtime/emit` (ver Observaciones), cadena de seguridad.

## Archivos afectados

- EXISTENTES: `GlobalExceptionHandler.java`, `GlobalExceptionHandlerFrameworkErrorsTest.java` (solo se añaden dos tests 405), `docs/work-items/README.md`.
- NUEVOS: `RealtimeStreamErrorHttpTest.java`, este work item.

## Riesgos y dependencias

- Un cliente que envíe `Accept: application/xml` u otro tipo recibirá ahora el error JSON en vez de un fallo de negociación; es lo que declara el contrato (`ApiErrorResponse` siempre `application/json`).
- `handleMethodNotSupported` reconstruye la respuesta: se copian los headers para no perder el Content-Type.

## Test plan

Ver [TEST_PLAN](TEST_PLAN.md). Secuencia RED → GREEN → VALIDATE.

## Observaciones fuera de alcance

`RealtimeEventsController#emitCustomEvent` lanza `ResponseStatusException(400)`, que el advice atrapa en `handleUnexpected` (500 `INTERNAL_ERROR`). `/api/v1/realtime/emit` no está en el OpenAPI canónico; se registra como **defecto independiente** (fuera de FIX-001), no se corrige aquí.

## Deuda conocida y validación manual

- Validación manual pendiente: repetir el E2E real SEC-BASELINE-002 (EventSource/curl con `Accept: text/event-stream`, grupo ajeno) tras integrar el fix.
- Sin nueva deuda técnica.

## Definition of Ready

READY: defecto reproducido con evidencia E2E, contrato vigente identificado, alcance acotado a una variable, rollback trivial. Aprobación: instrucción explícita del usuario del repositorio (2026-10-14).
