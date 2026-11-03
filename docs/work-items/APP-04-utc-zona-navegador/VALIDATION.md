---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-03
---

# VALIDATION — APP-04 (backend)

- Base: `develop@9b765b0`; rama `fix/app04-utc-coordinador-sesiones`; fecha 2026-11-03 (America/Bogota), JDK 25, Windows.

## Comandos y resultados

| Alcance | Comando | Resultado | Limitaciones |
|---|---|---|---|
| RED focalizado | `./mvnw -o test -Dtest=ConsultarAsistenciaProgramaUseCaseImplTest,CoordinadorAsistenciaProgramaControllerTest` | RED esperado: exit 1, errores de compilación (API inexistente). | Producción sin cambios en esa corrida. |
| GREEN focalizado | mismo comando | PASS: 16 + 8 tests, 0 fallos, exit 0. | Use case con puertos simulados; controller MockMvc standalone. |
| Backend verify (1.ª) | `./mvnw -o verify` | FAIL: 3010 tests, 1 fallo `CleanArchitectureRulesTest.domain_no_depende_de_output_ports` (el record de dominio dependía de `SesionGrupoProjection`). | Hallazgo real; se corrigió el diseño, no la regla. |
| Backend verify (final) | `./mvnw -o verify` | PASS: BUILD SUCCESS, 3010 tests, 0 fallos/errores/skips; ArchUnit y JaCoCo PASS. | JaCoCo global LINE 92.35 % (14,917/16,153), BRANCH 83.83 % (4,215/5,028). No se ejecutó `-Pintegration`. |
| CI remoto | GitHub sobre el SHA final | PENDIENTE: se registra tras abrir el PR. | — |

## Casos

| ID | Resultado |
|---|---|
| APP04-01..05 (use case/dominio) | PASS |
| APP04-06 (interactor → DTO) | PASS |
| APP04-07 wire `fechaHoraInicioUtc = 2026-11-03T15:00:00.1234567Z`, v1 `2026-11-03T15:00:00.1234567` intacto | PASS |
| APP04-08 INDETERMINADA → `null` explícito | PASS |

## No ejecutado

- HTTP real contra SQL Server con el backend nuevo: NOT_RUN (el backend local en :8080 es el jar previo; no se
  redesplegó). La consulta reutiliza `uv_sesion_v2` vía `SesionProcedenciaQueryPort`, ya validada en UTC v2.
- Navegador COORDINADOR: NOT_RUN (requiere despliegue de esta rama y sesión manual del coordinador).
