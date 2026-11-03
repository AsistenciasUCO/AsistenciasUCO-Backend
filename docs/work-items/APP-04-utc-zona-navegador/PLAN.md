---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-03
---

# PLAN — APP-04: horario inequívoco de sesiones para el coordinador

## Identidad y objetivo

- Fecha / base Git: 2026-11-03 / `develop@9b765b0`. Rama: `fix/app04-utc-coordinador-sesiones`.
- Origen: `SEC-BASELINE-019/PLAN-CORRECCIONES-POR-ROL.md` APP-04 y `SEC-BASELINE-018/VALIDATION.md` §K.1: la misma
  sesión `2026-08-17T13:00:00Z` se ve 08:00 (docente, v2) y 13:00 (coordinador, v1 `LocalDateTime` sin zona).
- Objetivo: HU060 `GET /api/v1/coordinador/grupos/{grupoId}/sesiones` expone, de forma aditiva, los instantes UTC
  confirmados de cada sesión para que el frontend los presente en la zona del navegador.
- Skills: `uco-contratos`, `uco-arquitectura`, `uco-testing`, `uco-backend-pagination-utc` (reglas UTC-D06/D07).
- Autorización: encargo del usuario del 2026-11-03 (corrección 05 APP-04).

## AS-IS

| Hecho | Fuente |
|---|---|
| HU060 serializa `SesionGrupoDTO` con `LocalDateTime fechaHoraInicio/Fin` (sin zona) leído de `usp_consultar_sesiones_grupo_paginado`; el SP aísla por programa y devuelve lista vacía para grupos ajenos. | `ConsultasAsistenciaProgramaDTO`, `HU064-HU060-HU061/CONTRACT.md` |
| La procedencia temporal ya está disponible por grupo en `uv_sesion_v2` mediante `SesionProcedenciaQueryPort.consultarSesionesPorGrupo`; `SesionProcedenciaEntity` anula horas no CONFIRMADAS. | `SesionProcedenciaMapper`, `EstadoHorarioSesion` |
| El wire v2 formatea instantes con `HttpUtcInstantCodec` (`Z`, hasta 7 decimales). | `SesionV2ConsultaController` |

## TARGET y decisiones

- Campos v1 intactos. Nuevos: `fechaHoraInicioUtc`, `fechaHoraFinUtc` (string RFC3339 `Z` vía `HttpUtcInstantCodec`)
  y `estadoTemporal` (`CONFIRMADA`/`INDETERMINADA`), siempre presentes (`null` explícito si no hay instante).
- La procedencia solo se consulta si la página aislada por programa tiene filas, y solo se aplica a filas del mismo
  `grupo` e id de sesión. Sin fila o con procedencia no confirmada → `INDETERMINADA`, sin instantes.
- No se modifica DB, SP, vistas ni datos; no se convierten históricos. Ownership sigue en el SP + `exigirCoordinador`.
- Change class: `ADDITIVE_CONTRACT_CHANGE`. PRIMARY_VARIABLE: `coordinatorSessionUtcInstants`.

## Rutas

- ALLOWED: feature `coordinador/consultarasistenciaprograma/**`, `controller/coordinador/CoordinadorAsistenciaProgramaController`
  y `SesionGrupoCoordinadorResponse`, `AsistenciaProgramaWiringConfiguration`, sus tests, `HU064-HU060-HU061/CONTRACT.md`
  y este work item.
- FORBIDDEN: DB/SQL, contratos v2, seguridad/PEP/PDP, reportes y exportaciones.

## Pendiente (bloques siguientes, no en esta rama)

- `/api/v1/decano/sesiones` (HU145) y `/api/v1/estudiante/materias/{id}/sesiones`: mismo enriquecimiento aditivo.
- HU061 `AsistenciaSesionDTO.fechaHoraInicio/Fin` y reporte Excel: v1 sin zona; requieren política explícita.
- Bitácora del coordinador (`DocenteProgramaJpaRepository`) convierte `datetime2` a `Instant` asumiendo UTC sin leer
  procedencia: riesgo de reinterpretar históricos INDETERMINADOS. Requiere decisión contractual.

## Rollback

Revertir la rama; el wire v1 no cambió y los campos nuevos son ignorables por consumidores existentes.
