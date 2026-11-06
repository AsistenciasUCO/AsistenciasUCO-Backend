---
status: proposal
type: proposal
scope: integration
owner: backend-team
last-reviewed: 2026-11-04
---

# SES-03 — Solicitud de sesión extraordinaria: propuesta técnica (NO implementada)

Estado: **BLOQUEADO POR AUTORIZACIÓN** (tabla y contrato nuevos del owner DB) + **PENDIENTE DE DECISIÓN FUNCIONAL** (ver §4).
Fuente de la política: `docs/work-items/SEC-BASELINE-019-upgrade-performance/PLAN-CORRECCIONES-POR-ROL.md` §SES-03.

## 1. AS-IS comprobado (2026-11-04)

- No existe tabla, SP, endpoint ni pantalla de *solicitud* extraordinaria en ninguna capa.
- El DOCENTE titular crea sesiones oficiales directamente: `POST /api/v1/sesiones` y `POST /api/v2/sesiones`
  (`SecurityConfig`: `hasRole(DOCENTE)`) → `usp_crear_sesion_v2` → `usp_crear_sesion_interno`
  (`usp_validar_titularidad_jerarquica_interno`). En la SPA: `teacher-sesion-form.component.ts`.
- Esto contradice la política confirmada («la solicitud no crea sesión oficial»). Restringirlo sin el flujo nuevo
  eliminaría la única vía actual de registrar sesiones manuales: no se cambia de forma unilateral.

## 2. Contrato DB propuesto (para revisión de John Duque)

Tabla `SolicitudSesionExtraordinaria`: `id`, `grupo` (FK Grupo), `docenteSolicitante` (FK Docente), `motivo NVARCHAR(500)`,
`fechaHoraInicioPropuestaUtc`, `fechaHoraFinPropuestaUtc` (DATETIME2(7), UTC), `estado` (FK Estado: PENDIENTE/APROBADA/RECHAZADA),
`coordinadorResolutor` (FK, NULL), `fechaResolucionUtc` (NULL), `justificacionRespuesta` (NULL), `sesionCreada` (FK Sesion, NULL),
`idCorrelacion`, `fechaCreacionUtc`.
Restricciones: `CHECK (fin > inicio)`; índice único filtrado `(grupo, fechaHoraInicioPropuestaUtc) WHERE estado = PENDIENTE`;
`CHECK (estado = APROBADA) = (sesionCreada IS NOT NULL)`.

SP públicos (patrón de `usp_resolver_solicitud_revision_asistencia`):

| SP | Actor | Reglas |
|---|---|---|
| `usp_radicar_solicitud_sesion_extraordinaria` | DOCENTE titular del grupo | inicio futuro, dentro del período, sin solapar sesiones del grupo; idempotente por `idCorrelacion` |
| `usp_consultar_solicitudes_sesion_extraordinaria_coordinador` | COORDINADOR del programa del grupo | paginado, `uv_*` autorizada |
| `usp_resolver_solicitud_sesion_extraordinaria` | COORDINADOR del programa del grupo | `UPDLOCK, HOLDLOCK`; APROBADA crea la sesión con `procedenciaTemporal = UTC_V2` vía `usp_crear_sesion_interno` en la misma tx; misma decisión repetida = idempotente sin segunda sesión; decisión contraria = conflicto; RECHAZADA no crea sesión; auditoría en `AuditoriaEvento` |

Mensajes nuevos `SSE_001..SSE_00n` (no encontrada, no PENDIENTE, decisión contraria, solape, fuera de período, inicio pasado).

## 3. Backend y SPA propuestos

- `POST /api/v1/sesiones-extraordinarias/solicitudes` (DOCENTE) → 201; 400 validación; 403 no titular; 409 solape/duplicado.
- `GET /api/v1/coordinador/sesiones-extraordinarias/solicitudes?estado=PENDIENTE` (COORDINADOR).
- `POST /api/v1/coordinador/sesiones-extraordinarias/solicitudes/{id}/resolucion` `{accion, justificacion}` → 200; 403 otro programa;
  404; 409 decisión contraria. Orden 404→403→409 como SES-01/SES-04. Se define primero en el OpenAPI canónico.
- SPA: formulario de solicitud del docente (reutiliza la conversión UTC de APP-04), bandeja del coordinador con guard de doble envío.
- Notificación persistente solo con contratos existentes (outbox); **sin** MessageUcoLab ni correo hasta autorización.

## 4. Decisiones pendientes (no se asumen)

1. ¿Se retira la creación directa de sesiones por el DOCENTE, o se conserva para sesiones ordinarias faltantes?
2. ¿Decano o administrador pueden sustituir al coordinador?
3. Canal de aviso: correo institucional y su proveedor (MessageUcoLab no se activa en esta fase).
4. ¿Qué pasa si cambia el coordinador del programa con solicitudes PENDIENTE?
5. QR/PIN de asistencia de la sesión extraordinaria: fuera de alcance hasta su decisión de seguridad.

## 5. Pruebas RED derivadas (para cuando se apruebe)

DB: radicar no crea `Sesion`; aprobar crea exactamente 1; aprobar dos veces (secuencial y concurrente) → 1 sesión;
rechazar → 0; aprobar tras rechazar → conflicto; coordinador de otro programa → rechazo sin escritura; solape → rechazo.
Backend: matriz HTTP 201/200/400/403/404/409 por rol. SPA: doble clic no envía dos resoluciones; la sesión aprobada aparece en el listado real.
