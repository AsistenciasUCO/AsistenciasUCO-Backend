---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-03
---

# TEST_PLAN — APP-04 (backend)

| ID | Prueba | Expectativa |
|---|---|---|
| APP04-01 | `ConsultarAsistenciaProgramaUseCaseImplTest.T_APP04_01_*` | Procedencia `UTC_V2` del mismo grupo → `CONFIRMADA`, instantes con 7 decimales; valor v1 intacto. |
| APP04-02 | `T_APP04_02_*` | Procedencia `null` o sin fila → `INDETERMINADA`, instantes `null`. |
| APP04-03 | `T_APP04_03_*` | Fila de procedencia con otro `grupo` se ignora. |
| APP04-04 | `T_APP04_04_*` | Página vacía (grupo ajeno/sin sesiones) no consulta procedencia. |
| APP04-05 | `T_APP04_05_*` | El dominio descarta instantes si el estado no es CONFIRMADA; dependencias obligatorias. |
| APP04-06 | `interactor_mapea_proyecciones_a_dto_con_pagina_y_tamano` | DTO lleva v1 + `*Utc` + `estadoTemporal`. |
| APP04-07 | `CoordinadorAsistenciaProgramaControllerTest.T_APP04_01_*` | Wire: `fechaHoraInicio = 2026-11-03T15:00:00.1234567`, `fechaHoraInicioUtc = 2026-11-03T15:00:00.1234567Z`. |
| APP04-08 | `T_APP04_02_*` (controller) | INDETERMINADA → `*Utc` como `null` explícito. |

RED: `./mvnw -o test -Dtest=ConsultarAsistenciaProgramaUseCaseImplTest,CoordinadorAsistenciaProgramaControllerTest`
2026-11-03 ~02:00 America/Bogota → exit 1, errores de compilación (constructor de 3 argumentos, `estadoTemporal()`,
`fechaHoraInicioUtc()` inexistentes) con producción sin cambios.
