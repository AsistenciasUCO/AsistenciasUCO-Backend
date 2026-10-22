# CONTRACT — HU035 + HU036: Bandeja y detalle de solicitudes de revisión del docente

> **Work Item:** `HU035-HU036-bandeja-detalle-reclamos-docente`
> **Estado:** `FROZEN` — aprobado por el owner el 2026-10-21 con D1 = B, D2 = B, D3 = A (ver [PLAN](PLAN.md)).
> **Fecha:** 2026-10-21
> **Fuentes:** [HTTP_AS_IS_MATRIX](../../contracts/HTTP_AS_IS_MATRIX.md), [http-success-responses](../../architecture/http-success-responses.md),
> `GestioAsistenciaDB/schema/stored-procedures/usp_consultar_solicitudes_revision_docente.sql` (LB-004D R2), `CommonErrorCode`.

Principio: AS-IS → diff → decisión → TARGET. Ningún contrato existente se modifica; todo cambio es aditivo.

---

## 1. Matriz AS-IS / TARGET

| Elemento | AS-IS | TARGET | Tipo |
|---|---|---|---|
| `usp_consultar_solicitudes_revision_docente` (R2) | Paginado, titularidad en SP, sin causal | **Sin cambios** | Congelado (LB-004D) |
| `usp_consultar_detalle_solicitud_revision_docente` | No existe | Nuevo SP de lectura, 0..1 fila con causal | Aditivo |
| `GET /api/v1/docente/reclamos` | `ApiListResponse<ReclamoDocenteResponse>`, query `estado`, `pagina`, `tamano` | **Sin cambios de contrato**; el frontend empieza a enviar los query params | Sin cambio |
| `GET /api/v1/docente/reclamos/{id}` | No existe | Nuevo, `ApiDataResponse<DetalleReclamoDocenteResponse>` | Aditivo |
| `PATCH /api/v1/docente/reclamos/{id}` | Implementado (HU037) | Sin cambios | Fuera de alcance |
| Parámetro `ARCHIVO.ESTADO_ADJUNTO` | No existe (R2 usa el literal `'ATTACHED'`) | Nuevo parámetro de catálogo con valor `ATTACHED` (grupo `ARCHIVO` ya existente en `15_lb004d_archivo_soporte.sql`) | Aditivo (D3 = A) |

---

## 2. Base de datos

### 2.1 `dbo.usp_consultar_detalle_solicitud_revision_docente` (NUEVO)

Clasificación: `NON_GOLDEN_PATH_EXTENSION`, `READ_QUERY_RESULTSET` (mismo estilo que R2: los parámetros inválidos se propagan con `THROW 51000`).

| Parámetro | Tipo SQL | Requerido | Regla |
|---|---|---|---|
| `@idSolicitud` | `UNIQUEIDENTIFIER` | SÍ | `IS NULL` → `THROW` con mensaje de catálogo `VAL_001` (`Solicitud`). |
| `@idUsuarioEjecutor` | `UNIQUEIDENTIFIER` | SÍ | `IS NULL` → `THROW` con `VAL_001` (`UsuarioEjecutor`). |
| `@idCorrelacion` | `UNIQUEIDENTIFIER` | SÍ | Validado con `usp_validar_id_correlacion_esta_presente_interno`. |

Reglas:
- **Titularidad dentro del SP:** misma cadena que R2 (`SolicitudRevisionAsistencia → Asistencia → Sesion → Grupo → Docente.usuario = @idUsuarioEjecutor`).
  Solicitud inexistente o ajena → **0 filas** (no distingue ambos casos).
- Solo lectura: `SET NOCOUNT ON; SET XACT_ABORT ON;` sin transacción explícita (no muta).
- Estado del soporte adjunto resuelto con `dbo.ufn_obtener_parametro('ARCHIVO', 'ESTADO_ADJUNTO')`; sin literales quemados.
- Datos personales mínimos: nombre del estudiante, **sin correo**.

Resultset (0..1 fila; nombres de columna exactos):

| Columna | Tipo | Nulo | Origen |
|---|---|:---:|---|
| `idSolicitudRevisionAsistencia` | `UNIQUEIDENTIFIER` | NO | `sra.id` |
| `nombreSolicitud` | `NVARCHAR(50)` | NO | `sra.nombre` |
| `fechaSolicitud` | `DATE` | NO | `sra.fecha` (semántica de calendario v1) |
| `codigoEstado` | `NVARCHAR(5)` | NO | `Estado.codigo` (`P`/`A`/`R`) |
| `nombreEstado` | `NVARCHAR(50)` | NO | `Estado.nombre` |
| `justificacionSolicitud` | `NVARCHAR(MAX)` | NO | `sra.justificacionSolicitud` |
| `justificacionRespuesta` | `NVARCHAR(MAX)` | NO | `sra.justificacionRespuesta` (vacío si no hay respuesta) |
| `idEstudiante` | `UNIQUEIDENTIFIER` | NO | `Estudiante.id` |
| `nombreEstudiante` | `NVARCHAR(400)` | NO | `CONCAT_WS` de nombres y apellidos (igual que R2) |
| `idSesion` | `UNIQUEIDENTIFIER` | NO | `Sesion.id` |
| `numeroSesion` | `INT` | NO | `Sesion.numero` |
| `fechaHoraInicioSesion` | `DATETIME2(7)` | NO | `Sesion.fechaHoraInicio` tal cual |
| `procedenciaTemporalSesion` | `NVARCHAR` | SÍ | `Sesion.procedenciaTemporal` (contrato UTC v2) |
| `idGrupo` / `codigoGrupo` / `nombreGrupo` | `UNIQUEIDENTIFIER` / `INT` / `NVARCHAR` | NO | `Grupo` |
| `idAsignatura` / `codigoAsignatura` / `nombreAsignatura` | `UNIQUEIDENTIFIER` / `NVARCHAR` / `NVARCHAR` | NO | `Asignatura` |
| `asistio` | `BIT` | SÍ | `DetalleAsistencia.asistio` (NULL si no hay detalle) |
| `codigoRazonCausa` | `NVARCHAR(5)` | SÍ | `RazonCausa.codigo` (`AN`, `SJC`, `EX`, `CPVP`, `CPI`) |
| `nombreRazonCausa` | `NVARCHAR(50)` | SÍ | `RazonCausa.nombre` |
| `idArchivoSoporte` | `UNIQUEIDENTIFIER` | SÍ | `ArchivoSoporte.id` adjunto |
| `nombreSoporte` | `NVARCHAR` | SÍ | `ArchivoSoporte.nombreOriginal` |
| `tipoContenidoSoporte` | `NVARCHAR` | SÍ | `ArchivoSoporte.tipoContenido` |
| `tamanioSoporteBytes` | `BIGINT` | SÍ | `ArchivoSoporte.tamanioBytes` |
| `fechaRegistroSoporteUtc` | `DATETIME2(7)` | SÍ | `ArchivoSoporte.fechaRegistroUtc` |

`DetalleAsistencia` y `ArchivoSoporte` se unen con `LEFT JOIN` (la solicitud existe aunque falte el detalle o el soporte).

### 2.2 Parámetro de catálogo (D3 = A)

Seed idempotente nuevo `schema/seed/16_hu035_parametro_estado_adjunto.sql` (`MERGE` como `03_parametro.sql`) en
`dbo.CatalogoParametro`: grupo `ARCHIVO`, clave `ESTADO_ADJUNTO`, valor `ATTACHED`, tipo `NVARCHAR`. No se modifica
`15_lb004d_archivo_soporte.sql` ni R2 (ambos en el manifiesto LB-004D; el literal de R2 se registra como deuda).

---

## 3. HTTP (Backend)

### 3.1 `GET /api/v1/docente/reclamos` — sin cambios

| Query | Tipo | Regla (AS-IS, `ConsultarReclamosDocenteDomain`) |
|---|---|---|
| `estado` | string opcional | `P`/`A`/`R` o `PENDIENTE`/`APROBADA`/`RECHAZADA`; desconocido → `400 VALIDATION_ERROR` |
| `pagina` | int opcional | `< 1` o nulo → 1 |
| `tamano` | int opcional | nulo o `< 1` → 10; `> 100` → 100 |

Respuesta `200`: `ApiListResponse<ReclamoDocenteResponse>` → `{ "exitoso": true, "datos": [...], "total": <totalRegistros> }`.

### 3.2 `GET /api/v1/docente/reclamos/{id}` — NUEVO

- **Autorización:** matcher existente `/api/v1/docente/**` → `hasRole("DOCENTE")`. El docente se resuelve desde el JWT
  (`AuthenticatedUserResolver.requireAuthenticatedUserId()`); nunca desde el request.
- **Path:** `id` UUID. Formato inválido → `400 INVALID_REQUEST` (comportamiento global de `GlobalExceptionHandler` para `@PathVariable UUID`; enmienda E-01).
- **Flujo:** `DocentePortalController` → `ConsultarDetalleReclamoDocenteInputPort` → `ConsultarDetalleReclamoDocenteUseCaseImpl`
  (valida que el usuario sea docente con `InstitutionalScopePort.findDocenteIdByUsuario`) → `SolicitudRevisionDocenteQueryPort#consultarDetalleSolicitudDocente`
  → `SolicitudRevisionDocenteJpaRepository` → `EXEC dbo.usp_consultar_detalle_solicitud_revision_docente`.

Respuesta `200 OK` — `ApiDataResponse<DetalleReclamoDocenteResponse>`:

```json
{
  "exitoso": true,
  "datos": {
    "id": "3f0c…",
    "nombre": "REV-…",
    "fechaSolicitud": "2026-10-15",
    "codigoEstado": "P",
    "estadoSolicitud": "PENDIENTE",
    "nombreEstado": "pendiente",
    "justificacionSolicitud": "Cita médica programada.",
    "justificacionRespuesta": "",
    "estudianteId": "…",
    "estudianteNombre": "Ana María Pérez Gómez",
    "sesionId": "…",
    "sesionNumero": 4,
    "fechaSesionUtc": "2026-10-14T13:00:00.0000000Z",
    "procedenciaTemporalSesion": "UTC_V2",
    "grupoId": "…",
    "grupoCodigo": 1,
    "grupoNombre": "Grupo 1",
    "materiaId": "…",
    "materiaCodigo": "MAT101",
    "materiaNombre": "Cálculo I",
    "causalInasistencia": { "codigo": "SJC", "nombre": "sin justa causa", "asistio": false },
    "soporteAdjunto": { "fileId": "…", "nombre": "incapacidad.pdf", "tipo": "application/pdf", "fechaSubidaUtc": "…", "url": "/api/v1/archivos/…" }
  }
}
```

Reglas de mapeo (iguales a la bandeja para no duplicar semántica):
- `estadoSolicitud` usa la misma traducción `P→PENDIENTE`, `A→APROBADA`, `R→RECHAZADA` de `ConsultarReclamosDocenteMapper`.
- `fechaSesionUtc` solo se emite cuando `procedenciaTemporalSesion` no es nula (contrato UTC v2); si no, `null`.
- `causalInasistencia` es `null` si no existe `DetalleAsistencia`.
- `soporteAdjunto` es `null` si no hay archivo adjunto; `url` apunta a `GET /api/v1/archivos/{fileId}` (descarga con Bearer; HU039 fuera de alcance).

| Caso | HTTP | Cuerpo |
|---|---|---|
| Solicitud del docente titular | 200 | `ApiDataResponse` |
| Solicitud inexistente o de otro docente | 404 | `ApiErrorResponse` `RESOURCE_NOT_FOUND` |
| `id` no UUID | 400 | `INVALID_REQUEST` (E-01) |
| Usuario autenticado sin registro de docente | 403 | `ForbiddenException` (igual que la bandeja) |
| Rol distinto de `DOCENTE` | 403 | Respuesta de `SecurityErrorResponseWriter` |
| Sin token | 401 | Respuesta de `SecurityErrorResponseWriter` |
| Fallo técnico de DB | 500 | Error seguro, sin `mensajeTecnico` expuesto; `X-Correlation-Id` presente |

Documentación del endpoint (enmienda E-02): sin anotaciones `@Operation`/`@ApiResponse`, porque `src/main` no las usa y el
contrato OpenAPI vive en `openapi-golden-path.yaml` (WORM, fuera del Golden Path de esta HU). El endpoint se registra en
[HTTP_AS_IS_MATRIX](../../contracts/HTTP_AS_IS_MATRIX.md).

---

## 4. Frontend

### 4.1 Servicio `AttendanceClaimService`

```typescript
getReclamosDocente(filtro: { estado?: 'P' | 'A' | 'R'; pagina: number; tamano: number }):
  Observable<ApiListResponse<SolicitudRevisionItem>>;   // envía estado/pagina/tamano como query params

getDetalleReclamoDocente(id: string):
  Observable<ApiDataResponse<DetalleReclamoDocenteItem>>; // GET /docente/reclamos/{id}
```

- Se elimina el enmascaramiento de `501/404` como éxito: los errores se propagan y la vista muestra el `message` del backend.
- `id` se valida con `isUuid` antes de construir la ruta.

### 4.2 Modelo `DetalleReclamoDocenteItem` (en `role-management.model.ts`)

Extiende `SolicitudRevisionItem` con:

```typescript
causalInasistencia: { codigo: string; nombre: string; asistio: boolean | null } | null;
nombreEstado: string;
```

Sin `any`. Fechas presentadas con `SessionTimeZoneService` como hoy.

### 4.3 `TeacherClaimsComponent`

- Estado con Signals: `estadoFiltro = signal<'TODOS' | 'P' | 'A' | 'R'>('TODOS')`, `pagina = signal(1)`, `tamano = signal(10)`, `total = signal(0)`.
- Cada cambio de filtro, página o tamaño dispara una recarga (sin `setInterval`).
- `<app-pagination [totalItems]="total()" [pageSize]="tamano()" [currentPage]="pagina()" (pageChange)=… (pageSizeChange)=…>`.
- Contador de pendientes = `total` de una consulta con `estado=P` (no se calcula sobre la página cargada).
- Abrir una solicitud llama a `getDetalleReclamoDocente(id)` y la vista de resolución usa ese detalle; se muestra la causal real.
- Se mantiene `ChangeDetectionStrategy.OnPush` y el flujo de resolución existente (HU037/HU038).

---

## 5. Realtime

No aplica: HU035/HU036 son consultas. La notificación al estudiante tras la resolución (HU042) no cambia.

## 6. Aprobación

| Decisión | Responsable | Estado |
|---|---|---|
| D1 = B, D2 = B, D3 = A | Usuario (owner del work item) | APROBADO 2026-10-21 |
| Congelamiento del contrato | Usuario | APROBADO 2026-10-21 |
| E-01: id no UUID → `400 INVALID_REQUEST` (antes `VALIDATION_ERROR`, desviación D-01) | Usuario | APROBADO 2026-10-21 |
| E-02: sin anotaciones OpenAPI; registro en `HTTP_AS_IS_MATRIX.md` (desviación D-02) | Usuario | APROBADO 2026-10-21 |

### Enmiendas

- **E-01 (D-01):** el contrato original pedía `VALIDATION_ERROR` para un `id` mal formado, pero el handler global responde
  `INVALID_REQUEST` en todos los `@PathVariable UUID`. Se alinea el contrato con el comportamiento global AS-IS; cambiar el
  handler quedaría fuera del alcance de la HU. El test CT-02 ya verifica `400 INVALID_REQUEST`.
- **E-02 (D-02):** se retira el requisito de anotaciones OpenAPI en el controlador, porque el código no usa ese mecanismo.
  La trazabilidad HTTP del endpoint nuevo queda en `HTTP_AS_IS_MATRIX.md`.
