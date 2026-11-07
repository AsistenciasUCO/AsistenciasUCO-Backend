---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-11-07
---

# Contrato y evolución de eventos realtime

## AS-IS y evidencia

La arquitectura detallada permanece en [reactive-realtime](../architecture/reactive-realtime.md). Este documento gobierna el contrato del evento y su compatibilidad, sin seleccionar transporte nuevo.

| Borde | Forma comprobada | Evidencia |
|---|---|---|
| Application | `RealtimePublisherPort.publish(RealtimeEvent)` sin Reactor/SSE/SDK broker | [Port](../../src/main/java/co/edu/uco/asistenciasuco/application/secondaryports/realtime/RealtimePublisherPort.java) |
| Evento neutral | eventId UUID, type String, occurredAt Instant, correlationId/traceId/spanId opcionales, payload Map | [RealtimeEvent](../../src/main/java/co/edu/uco/asistenciasuco/application/secondaryports/realtime/RealtimeEvent.java) |
| HTTP/SSE | `id=eventId`, `event=type`, `data=RealtimeEventResponse`; response incluye eventId/type/occurredAt/correlationId/payload, no traceId/spanId | [Controller](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/realtime/sse/controller/RealtimeEventsController.java), [response](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/realtime/sse/response/RealtimeEventResponse.java) |
| Lote exitoso | `ASISTENCIAS_SESION_ACTUALIZADAS`; payload `grupo` UUID como texto, `sesion` UUID como texto, `totalRegistros` número | [UseCase](../../src/main/java/co/edu/uco/asistenciasuco/application/features/asistencia/registrarasistenciassesion/usecase/impl/RegistrarAsistenciasSesionUseCaseImpl.java) |
| Stream | `GET /api/v1/realtime/stream?grupoId={UUID}`; Bearer, autorización de titularidad al suscribirse y filtro `payload.grupo` | [Gateway](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/realtime/sse/localsse/LocalSseRealtimeStreamGateway.java) |
| Sesión cancelada (CULM-001 B.1, HU047) | `SESION_CANCELADA`; payload `grupo`, `sesion` (UUID como texto), `tipo` (`DOCENTE`) y `fechaHoraInicioUtc` (ISO-8601 con `Z`, solo si la procedencia temporal está confirmada). Sin motivo ni datos personales. Se publica solo tras el commit de `usp_cancelar_sesion`; un fallo del publicador no cambia la respuesta | [UseCase](../../src/main/java/co/edu/uco/asistenciasuco/application/features/sesioncancelacion/cancelarsesion/usecase/impl/CancelarSesionUseCaseImpl.java) |
| Tomas y QR de asistencia (CULM-001 B.2) | `TOMA_ABIERTA` (`grupo, sesion, toma, numero, modo, fechaAperturaUtc`), `TOMA_CERRADA` (`grupo, sesion, toma, numero, totalAusentesMarcados, totalFaltantes, automatico`; el cierre automatico no lleva `totalFaltantes`), `ASISTENCIA_QR_ROTADO` (`grupo, sesion, toma, pin, urlAsistencia, qrPngBase64, fechaRotacionUtc, fechaExpiracionUtc, rotacionSegundos`; lleva el token: excepcion documentada, solo docente titular), `ASISTENCIA_QR_REGISTRADA` (`grupo, sesion, toma, estudiante, totalPresentes`; solo registros nuevos) y `ASISTENCIAS_TOMA_ACTUALIZADAS` (`grupo, sesion, toma, totalRegistros`). Todos tras el commit, de mejor esfuerzo, audiencia `DOCENTE_TITULAR` por igualdad exacta de tipo | [Politica](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/realtime/sse/RealtimeAudienciaPolicy.java) |
| Matrícula QR (HU176B) | `MATRICULA_QR_ROTADO` lleva el token dentro de `urlMatricula`/`qrPngBase64` (excepción documentada: solo se entrega al docente titular) y `ESTUDIANTE_MATRICULADO` lleva `grupo` y `origen` | [Rotación](../../src/main/java/co/edu/uco/asistenciasuco/application/features/matriculaqr/usecase/impl/RotarTokensMatriculaAutomaticoUseCaseImpl.java) |
| Política de audiencia (CULM-001 B.1) | `RealtimeAudienciaPolicy`: mapa tipo → audiencias con **igualdad exacta** de tipo (nunca por prefijo; un tipo `topic.action` de `POST /realtime/emit` no coincide con un tipo de negocio). En B la única audiencia es `DOCENTE_TITULAR` y un tipo no listado también llega solo al titular; el paquete C añadirá `ESTUDIANTE_GRUPO` y abrirá la suscripción a otros roles | [Policy](../../src/main/java/co/edu/uco/asistenciasuco/infrastructure/adapter/primary/realtime/sse/RealtimeAudienciaPolicy.java) |

El use case individual contiene `ASISTENCIA_REGISTRADA`, pero su adapter SQL productivo rechaza actualmente el registro individual por falta de contrato DB; no afirmar que ese flujo fue validado productivamente. El Golden Path usa lote.

## Invariantes

- Publicar el evento de lote solo después del éxito de persistencia; sin evento ante error previo. No prometer atomicidad SQL/SSE.
- Provider `local-sse`: best-effort, efímero, una JVM, sin replay ni garantía para desconectados. Recuperar estado por API/DB; no usar eventos como fuente de verdad.
- Sin JWT, refresh tokens, passwords, cookies ni datos de sesión en payload. Conservar correlation y contexto servidor; no exponer traceId/spanId por accidente.
- `grupoId` obligatorio; heartbeat es comentario SSE cada 25 s, no evento de negocio. Polling periódico de frontend != realtime.
- No añadir `version`, campos, garantías de orden/deduplicación o nombres de eventos sin revisar consumidor y aprobar contrato. No existe campo de versión en el envelope actual.
- Registrar breaking changes con decisión de migración. Formalizar schemas en LB-001 junto con HTTP; el YAML de eventos solo se genera en un work item que lo autorice explícitamente.
- Cualquier distribución futura mantiene provider neutrality y requiere ADR; in-memory no resuelve escenarios multi-instancia. WebSocket no se introduce por preferencia.

Pruebas: publicación tras persistencia, no publicación en error, payload, scope por grupo, 401/403, heartbeat/cancelación y compatibilidad de consumidor. E2E pendiente en [MV-001](../baseline/MANUAL_VALIDATION_LEDGER.md).
