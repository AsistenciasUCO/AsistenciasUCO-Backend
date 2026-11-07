---
status: active
type: normative
scope: backend
owner: backend-team
last-reviewed: 2026-09-24
---

# Contrato backend del Golden Path (LB-001B.4)

Insumo para la verificación frontend y para LB-001C. **No es OpenAPI** ni congela la representación HTTP final de campos temporales. Extraído de controllers, DTOs, `SecurityConfig` y tests del checkout (rama `sergio`); no describe endpoints fuera del Golden Path. Evidencia: [VALIDATION](../work-items/LB-001B.4-final-backend-contract-closure/VALIDATION.md), [AUDIT](../work-items/LB-001B.4-final-backend-contract-closure/AUDIT.md). Contrato DB consumido: `docs/contracts/external/db/DB_BASELINE_CONTRACT.md` (SHA-256 `45e48c5a0ab321d0c8cbffb55ee224e3b6fd29febc39a62ca723b2b209945aec`).

## A. Endpoints y B. Roles (RBAC de `SecurityConfig`, prefijo `ROLE_`)

| # | Endpoint | Roles HTTP | Ownership adicional |
|---|---|---|---|
| 1 | `GET /api/v1/docente/horarios` | DOCENTE | horario del docente autenticado (Usuario→Docente) |
| 2 | `GET /api/v1/sesiones/grupo/{grupoId}` | DOCENTE | titularidad sobre el grupo (`InstitutionalScopePort`) |
| 3 | `GET /api/v1/grupos/{grupoId}/estudiantes` | DOCENTE, COORDINADOR, ADMINISTRADOR | docente: titularidad del grupo |
| 4 | `GET /api/v1/grupos/{grupoId}/asistencias?sesionId=` | DOCENTE, COORDINADOR, ADMINISTRADOR | docente: titularidad del grupo; `sesionId` opcional |
| 5 | `POST /api/v1/asistencias/lote` | DOCENTE | titularidad de la sesión (SP: `SEC_002`) |
| 6 | `GET /api/v1/realtime/stream?grupoId=` | autenticado (capa HTTP) | el gateway exige titularidad docente sobre `grupoId` al suscribirse |
| 7 | `POST /api/v1/sesiones` | DOCENTE | scope docente resuelto desde JWT (publicado hoy) |
| 8 | `PUT /api/v1/sesiones/{sesionId}` (legacy, `deprecated`; coexiste con #9 hasta migrar consumidores, LB-001C.2A) | DOCENTE | scope docente resuelto desde JWT (publicado hoy) |
| 9 | `PATCH /api/v1/sesiones/{sesionId}` (mismo caso de uso, request y respuesta que #8; `operationId=actualizarSesion`) | DOCENTE | scope docente resuelto desde JWT |

Sin Bearer: 401. Rol no permitido o titularidad ajena: 403. Endpoints legacy (`POST /asistencias/consultas/grupo`, `POST /sesiones/cierres`, etc.) están fuera de este contrato.

## C. Request / response

Envelopes de éxito (`infrastructure/.../controller/response`): `ApiListResponse<T> {exitoso, datos[], total}`, `ApiDataResponse<T> {exitoso, datos}`, `ApiMessageResponse {exitoso, mensaje}`.

| # | Status | Cuerpo |
|---|---|---|
| 1 | 200 | `ApiListResponse<HorarioDocenteDTO>`: `id, idDocente, idGrupo, codigoMateria, nombreMateria, seccion, dia, horaInicio (LocalTime), horaFin (LocalTime), totalEstudiantes`. Sin `aula`. |
| 2 | 200 | `ApiListResponse<SesionConsultadaDTO>`: `sesion, grupo, nombre, numero, codigo, numeroSemana, codigoGrupo, nombreGrupo, fechaHoraInicio, fechaHoraFin` (LocalDateTime sin zona; ver I). Sin estado de sesión, `descripcion`, `aula`, `tipo`. |
| 3 | 200 | `ApiListResponse<EstudianteGrupoDTO>`: `id, idEstudiante, documento, nombreCompleto, correo, codigoEstado, nombreEstado`. Devuelve todos los estudiantes del grupo sin filtrar por `codigoEstado` (filtrado activo = decisión del consumidor, DR-007). |
| 4 | 200 | `ApiListResponse<AsistenciaConsultadaDTO>`: `asistencia, estudiante, grupo, sesion, presente, estado, observacion`. Lista completa AS-IS, sin `page/size/sort/q` (DR-010 RESOLVED, Opción A). Sin orden público garantizado (no hay `ORDER BY` contractual); el consumidor no debe asumir ningún orden. |
| 5 | 201 | Request `{ "sesionId": UUID, "registros": [ { "estudianteId": UUID, "estado": "AN"\|"SJC"\|"EX" } ] }`; `estado` se normaliza trim+mayúsculas. Respuesta `ApiMessageResponse` (`"Asistencias de sesion registradas correctamente."`). El ejecutor sale del principal autenticado, no del body. Operación atómica: ante error no se escribe nada ni se publica evento. CULM-001 B: escribe en la toma vigente (la abierta o, si no hay, la última); una sesión cancelada responde 409 `ERR_SESION_CANCELADA` (además de `ERR_SESION_NO_INICIADA`, `ERR_SESION_CERRADA` y `ERR_SESION_HORARIO_INDETERMINADO`). |
| 6 | 200 `text/event-stream` | Ver K. `grupoId` (UUID) obligatorio. |
| 7 | 201 | Request `{ "grupo": UUID, "nombre", "fechaHoraInicio", "fechaHoraFin" }` (fechas string ISO local, sin zona). Respuesta `ApiMessageResponse`. El ejecutor sale del principal autenticado (`usuarioEjecutor`), no del body. Cualquier otro campo (`tema`, `topic`, `descripcion`, `aula`, `tipo`, `status`, `room`, `docente`) → 400 `FIELD_UNKNOWN` (DR-001/DR-004). |
| 8 | 200 | Request `{ "nombre", "fechaHoraInicio", "fechaHoraFin" }`. Respuesta `ApiDataResponse<Void>` = `{ "exitoso": true, "datos": null }`. Cualquier otro campo (`tema`, `topic`, `descripcion`, `aula`, `tipo`, `status`, `room`, `docente`) → 400 `FIELD_UNKNOWN`. |

Nota Sesion (CULM-001 B, golden path 1.3.0): `Sesion` y `SesionV2` agregan `estado` (`PROGRAMADA`|`CANCELADA`) y `cancelacion` (`null` si está programada; si no, `{ tipo: DOCENTE|FESTIVO, motivo, nombreFestivo|null, fechaCancelacionUtc }`), ambos requeridos. Nota Sesion (LB-001B.4A/4B): `nombre` es el único texto de la sesión; los códigos de validación son `ERR_NOMBRE_SESION_*`. `Sesion.nombre`: string requerido, `minLength=1`, `maxLength=50` (alineado con la DB congelada `NVARCHAR(50)`, decisión humana; cierra [TD-048](../baseline/TECHNICAL_DEBT.md#td-048)). Aplica a creación, actualización y lectura. Un `nombre` de 51+ caracteres en `POST /sesiones` responde 400 `VALIDATION_ERROR` con `details[].field=nombre`, `details[].code=FIELD_INVALID_LENGTH`; en `PUT /sesiones/{id}` (sin validador HTTP) lo rechaza el Domain con `ERR_NOMBRE_SESION_LONGITUD_INVALIDA` ("El nombre de la sesion debe tener entre 1 y 50 caracteres.").

## D. AttendanceStatus

Dominio cerrado `AN | SJC | EX` (`RegistroAsistenciaSesionDomain.ESTADOS_VALIDOS`; DB: `RC_001` para códigos inexistentes). Escritura: otro valor → 400 `VALIDATION_ERROR`/`ERR_ESTADO_ASISTENCIA_INVALIDO` sin escrituras. Lectura: `AsistenciaConsultadaEntity` **falla cerrado** ante estado legacy/desconocido/ausente (DR-006, Opción A); no se traduce ni se oculta. Riesgo registrado: `RazonCausa` conserva códigos históricos (`CPI`, `CPVP`) que harían fallar el listado completo ([TD-045](../baseline/TECHNICAL_DEBT.md#td-045)).

## E. Ausencia de registro

La lectura (4) solo devuelve asistencias persistidas (`INNER JOIN` desde `uv_detalle_asistencia`). **Ausencia = sin fila.** El backend no sintetiza `AN` ni ningún estado por estudiante sin registro; el frontend la presenta como «Sin registrar» (DR-002).

## F. Envelope de error HTTP

`ApiErrorResponse { timestamp (OffsetDateTime), status, error, code, message, path, correlationId, details[] }`; `details` (`{field, code, message}`, códigos `FIELD_REQUIRED|FIELD_INVALID_FORMAT|FIELD_INVALID_LENGTH|FIELD_OUT_OF_RANGE|FIELD_INVALID_TYPE|FIELD_INVALID_UUID|FIELD_UNKNOWN|FIELD_INVALID_VALUE`) se omite si está vacío. Status derivado de `ErrorKind` (`ApiErrorCatalog`). La serialización común de errores emitidos por la cadena de seguridad (401/403 previos al controller) es [TD-021](../baseline/TECHNICAL_DEBT.md#td-021), abierta: verificar en LB-001C.

## G. Códigos semánticos (`code`)

| `code` | HTTP | Origen en el Golden Path |
|---|---:|---|
| `UNAUTHORIZED` | 401 | sin/invalid Bearer |
| `FORBIDDEN` | 403 | rol/titularidad; DB `SEC_001`, `SEC_002`, `EST_004` |
| `VALIDATION_ERROR` | 400 | DB `ATT_001`, `ATT_002`, `ATT_003`, `GEN_002`, `RC_001`, `SES_004`; validación de request |
| `INVALID_REQUEST` | 400 | cuerpo/tipos no interpretables |
| `RESOURCE_NOT_FOUND` | 404 | DB `SES_001` |
| `FEATURE_UNAVAILABLE` | 501 | DB `SES_003` (cierre legacy; fuera del Golden Path) |
| `CONFLICT` | 409 | conflicto de estado |
| `INTERNAL_ERROR` / `ERR_DB_UNCLASSIFIED` | 500 | DBCODE desconocido/malformado (fail-closed) o error no clasificado |
| `ERR_*` de catálogo de feature (p. ej. `ERR_ESTADO_ASISTENCIA_INVALIDO`, `ERR_FECHA_HORA_INVALIDA`) | según `ErrorKind` | validaciones de Application/HTTP |

## H. DBCODE es interno

El canal `DBCODE=<código>|<detalle>` (parser estricto `^DBCODE=([A-Z0-9_]+)\|(.*)$`, `DbTechnicalError`/`DbFailureClassifier`) es **exclusivamente DB↔backend**. `DBCODE`, `mensajeTecnicoResultado` y el detalle **nunca** forman parte del contrato ni de la respuesta al frontend. Sin marcador DBCODE aplica el clasificador textual legacy, solo fuera del Golden Path.

## I. Temporal

- `Horario`: hora local institucional (`LocalTime`, día como texto); sin zona.
- `Sesion.fechaHoraInicio/fechaHoraFin`: semántica UTC en persistencia; los lectores usan `JdbcValueMapper.toLocalDateTimeUtc` (independiente de `TimeZone.getDefault()`).
- `RealtimeEvent.occurredAt`: `Instant` UTC, ISO-8601 con sufijo `Z`.
- La **representación HTTP final** de los campos `LocalDateTime` de sesión (hoy ISO local sin zona en request y response) se congela en **LB-001C** ([TD-005](../baseline/TECHNICAL_DEBT.md#td-005)). El frontend no debe asumir una zona.

## J. Correlación

`X-Correlation-Id`: `CorrelationIdFilter` acepta un UUID canónico del cliente; si falta o es inválido (formato, UUID cero, CRLF codificado) lo reemplaza por un UUID generado (no responde error) y siempre lo devuelve en la respuesta; se propaga a auditoría, logs y `@idCorrelacion` de los SP. `ApiErrorResponse.correlationId` lo refleja.

## K. Envelope realtime

SSE: `id=eventId`, `event=type`, `data=RealtimeEventResponse { eventId (UUID), type, occurredAt (Instant, ISO-8601 Z), correlationId, payload }`; no expone `traceId/spanId` ni campo `version`. Evento del Golden Path: `ASISTENCIAS_SESION_ACTUALIZADAS`, payload `{ grupo: UUID texto, sesion: UUID texto, totalRegistros: número }`, publicado tras persistencia exitosa. Filtro por `payload.grupo`; heartbeat = comentario SSE cada 25 s, no evento de negocio. Detalle: [REALTIME_EVENT_STANDARD](REALTIME_EVENT_STANDARD.md).

## L. Fuente de verdad

**HTTP es la fuente de verdad; SSE es una señal de notificación** best-effort, sin replay ni garantía de orden/entrega. Ante un evento, o al reconectar, el frontend recarga por HTTP (endpoint 4). Una pérdida de evento no implica pérdida de datos.

## M. Límite de despliegue

`SINGLE_INSTANCE_OK`: el provider `local-sse` (`Sinks` en memoria, una JVM) es correcto con una instancia. `MULTI_INSTANCE_PROVIDER_REQUIRED`: con más de una réplica se requiere un provider distribuido neutral decidido por ADR ([TD-003](../baseline/TECHNICAL_DEBT.md#td-003), LB-005). No implementado. HTTP y DB no guardan estado por réplica.

## Fuera de este contrato

OpenAPI y congelación temporal HTTP (LB-001C); paginación/orden (DR-010 RESOLVED Opción A: listas completas AS-IS; la paginación es una future feature con contrato propio); `usp_sincronizar_usuario`, `usp_registrar_o_actualizar_plan_estudio`, `usp_registrar_estudiante_en_grupo_usuario_no_existente` ([TD-043](../baseline/TECHNICAL_DEBT.md#td-043)); validación E2E manual ([MV-001](../baseline/MANUAL_VALIDATION_LEDGER.md)).
