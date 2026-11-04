---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-03
---

# PLAN — SES-01: cierre estricto del registro ordinario de asistencia

## Identidad y objetivo

- Fecha / base Git: 2026-11-03 / `develop@0d4db159` (incluye APP-04, PR #76). Rama: `fix/ses01-cierre-estricto-asistencia`.
- Origen: [PLAN-CORRECCIONES-POR-ROL](../SEC-BASELINE-019-upgrade-performance/PLAN-CORRECCIONES-POR-ROL.md), decisión confirmada 1 (2026-11-02) y SES-01.
- Requisito: el docente solo registra, corrige o sustituye asistencias ordinarias cuando
  `fechaHoraInicioUtc <= ahoraUtc < fechaHoraFinUtc`. Sesión 08:00–10:00 Bogotá: 07:59:59 rechaza, 08:00:00 acepta,
  09:59:59 acepta, 10:00:00 rechaza, 12:00 rechaza. Sin tolerancia. Aplica a UI, API directa y persistencia.
- Skills: `uco-contratos`, `uco-arquitectura`, `uco-testing`, `uco-backend-pagination-utc` (UTC-D06/D07).

## AS-IS (causa)

`RegistrarAsistenciasSesionUseCaseImpl` resolvía el grupo con `SesionRepositoryPort` (v1, `LocalDateTime` sin
procedencia), validaba titularidad y persistía. No comparaba el instante actual con el horario: una sesión cerrada o
futura aceptaba el lote por API directa. `usp_registrar_asistencias_sesion` tampoco validaba la ventana.

`POST /api/v1/asistencias` (registro individual) responde 501 (`FeatureUnavailableException`): no es ruta de escritura.

## Contrato (cambio aditivo)

| Elemento | Decisión |
|---|---|
| `POST /api/v1/asistencias/lote` | Nuevo `409` (`components/responses/SesionFueraDeVentana`); request, 201 y demás errores sin cambio |
| Códigos públicos (`ErrorKind.CONFLICT`) | `ERR_SESION_NO_INICIADA`, `ERR_SESION_CERRADA`, `ERR_SESION_HORARIO_INDETERMINADO` |
| DB → backend | `SES_007` / `SES_008` / `SES_009` → los mismos códigos, operación de lote y autónoma |
| Reloj | `Clock.systemUTC()` del servidor; nunca el del navegador. Comparación por instante (horas v2 en UTC) |
| Procedencia | Solo `UTC_V2`, `UTC_GENERADOR`, `UTC_OWNER` (regla `EstadoHorarioSesion`); `INDETERMINADA` → 409 sin suponer zona |
| Orden | sesión existe (404) → titularidad (403) → ventana (409) → persistencia → evento. La ventana va después de la titularidad para no revelar el estado temporal de sesiones ajenas |
| 409 vs 422 | 409: el recurso existe y el request es válido; lo que impide la operación es el estado temporal actual de la sesión |

Defensa en profundidad: la DB (rama DB homónima, pendiente de aprobación del owner) repite la ventana dentro de la
transacción del lote con `SYSUTCDATETIME()` y la fila de la sesión bloqueada, y la reevalúa antes del `COMMIT`. Así una
solicitud que pasa la validación del backend antes del cierre y llega al punto de escritura después también se rechaza.

## Alcance / no alcance

- Incluye: caso de uso, wiring, clasificación DB, OpenAPI (+ sidecar SHA), fixture de ITs de asistencia (sesiones v2 activas).
- No incluye: reclamos (SES-04), sesiones programadas/extraordinarias (SES-02/03), QR/PIN, cierre manual
  (`usp_cerrar_sesion` sigue 501), estados persistidos de sesión.

## Archivos

- `application/features/asistencia/registrarasistenciassesion/usecase/impl/RegistrarAsistenciasSesionUseCaseImpl.java`
- `application/features/asistencia/exception/AsistenciaErrorCode.java`
- `infrastructure/config/wiring/AsistenciaWiringConfiguration.java`
- `infrastructure/adapter/secondary/persistence/sqlserver/support/error/DbFailureClassifier.java`, `DbExceptionTranslator.java`
- `docs/contracts/openapi/openapi-golden-path.yaml`, `openapi-golden-path.sha256`
- Tests: `RegistrarAsistenciasSesionUseCaseImplTest`, `DbFailureClassifierVentanaAsistenciaTest`,
  `AsistenciaControllerContractTest`, `AsistenciaWiringConfigurationTest`, `AttendanceCommandParityFixture`, `AsistenciaCommandRealtimeIT`

## Rollback

Revertir los commits de la rama. El 409 es aditivo: un cliente que no lo conoce lo trata como error genérico.

## Dependencias e integración

Orden DB → Backend → Frontend. El backend rechaza por sí mismo aunque la DB aún no tenga el cambio; la garantía ante
carreras en el instante de cierre requiere la versión DB aprobada.
