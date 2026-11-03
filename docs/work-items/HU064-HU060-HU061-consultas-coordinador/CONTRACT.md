---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-25
---

# CONTRACT — HU064 / HU060 / HU061

Todos los endpoints: `GET`, rol `COORDINADOR` (regla existente `/api/v1/coordinador/**`), respuesta `ApiListResponse` `{ exitoso, datos[], total }`. Parámetros `pagina` (≥ 1, por defecto 1) y `tamano` (1..100, por defecto 10).

| Endpoint | Filtros | SP |
|---|---|---|
| `/api/v1/coordinador/grupos` | `idPeriodo` (UUID), `busqueda` (texto) | `usp_consultar_grupos_paginado` |
| `/api/v1/coordinador/grupos/{grupoId}/sesiones` | `desde`, `hasta` (ISO `yyyy-MM-dd`, `desde ≤ hasta`) | `usp_consultar_sesiones_grupo_paginado` |
| `/api/v1/coordinador/sesiones/{sesionId}/asistencias` | `asistio` (`true`/`false`) | `usp_consultar_asistencias_sesion_paginado` |

## Elementos

- **Grupo:** `id, codigo, nombre, idAsignatura, nombreAsignatura, idDocente, capacidadMaxima, estudiantesActivos, cuposDisponibles, habilitado, inicioPeriodo, finPeriodo`.
- **Sesión:** `id, idGrupo, codigoGrupo, nombreGrupo, nombre, numero, codigo, numeroSemana, fechaHoraInicio, fechaHoraFin`.
  - APP-04 (2026-11-03, aditivo): `fechaHoraInicio`/`fechaHoraFin` conservan el wire v1 (`LocalDateTime` sin zona). Se agregan `fechaHoraInicioUtc`, `fechaHoraFinUtc` (RFC3339 con `Z`, hasta 7 decimales, mismo códec que `/api/v2/sesiones`) y `estadoTemporal` (`CONFIRMADA`/`INDETERMINADA`) leídos de `uv_sesion_v2`. Los instantes solo se informan si la procedencia es `CONFIRMADA`; en otro caso son `null` explícito. Ver `docs/work-items/APP-04-utc-zona-navegador/`.
- **Asistencia:** `idDetalleAsistencia, idSesion, nombreSesion, idEstudiante, numeroIdentificacion, nombreCompleto, asistio, codigoRazonCausa, nombreRazonCausa, fechaHoraInicio, fechaHoraFin`.

## Errores

| Caso | HTTP |
|---|---|
| Usuario sin programa de coordinador | 403 `FORBIDDEN` |
| Paginación fuera de rango (`PAG_001` o validación del caso de uso), `desde > hasta`, UUID inválido | 400 |
| Grupo o sesión de otro programa | 200 con lista vacía (el SP filtra por ejecutor) |
