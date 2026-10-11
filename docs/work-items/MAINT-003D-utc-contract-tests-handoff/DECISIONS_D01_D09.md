---
status: TECHNICAL_DECISION_PROPOSED_NOT_OWNER_SIGNED
work-item: MAINT-003D
base: jose-valencia/maint-003b-utc-v2-red
date: 2026-10-09
scope: backend-http-and-contract
---
# MAINT-003D — Contrato temporal listo para implementación (D01–D09)

## Propósito y autoridad
Este documento toma decisiones de diseño **para entregar a Codex**, sin falsear firmas humanas. El backend de referencia es `alejovh12/AsistenciasUCO`, rama `jose-valencia/maint-003b-utc-v2-red`; DB fuente de verdad `johnjduque/gestion-asistencia-db` en `develop`; frontend observado `AsistenciasUCO/AsistenciasUCO-Frontend` en `develop`. La propuesta SQL del owner y el trabajo local MAINT-003C NO están publicados en estas ramas: reconciliar antes de cherry-pick. El freeze DB-GP-001C impide modificar estructura/SP/vistas sin work item y aprobación del owner DB.

**Regla de oro:** una sesión es un par de instantes; el navegador decide exclusivamente la zona de representación. `datetime2` sin marca temporal NO demuestra que un histórico sea UTC. Nunca emitir `Z` para una fila indeterminada.

| Decisión | Resolución técnica operativa | Límite / puerta |
|---|---|---|
| D01 Rutas | `POST /api/v2/sesiones`, `PATCH /api/v2/sesiones/{sesionId}`, `GET /api/v2/sesiones/{sesionId}`, `GET /api/v2/sesiones/grupo/{grupoId}`. Sin PUT v2. | No exponer rutas vacías ni 501; activar juntas tras DB + contrato. |
| D02 Wire | `fechaHoraInicio` y `fechaHoraFin`: **string RFC3339 estricto** `YYYY-MM-DDTHH:mm:ss[.1..7 dígitos](Z|±HH:mm)`; ambos obligatorios en POST/PATCH, `T` y `Z` mayúsculas, sin espacios. | Rechazar falta de offset, `+02`, `+02:00:30`, falta de segundos, 8–9 decimales; validar calendario/offset con parser además de regex. |
| D03 Persistencia | Normalizar offset a `Instant`; convertir al instante UTC representado como `LocalDateTime` para SQL Server `datetime2(7)`; **no usar zona de JVM**. Reutilizar use cases/JPA. | Un único punto de conversión HTTP; evitar doble conversión y redondeo. |
| D04 DST | Offset explícito determina instante. Si UI usa `Europe/Berlin`, una hora inexistente se rechaza, una ambigua requiere selección de offset. | Un offset por sí solo no prueba pertenencia a zona IANA; frontend verifica reglas IANA del día escogido. |
| D05 Seguridad | Actor únicamente de JWT, perfil `DOCENTE`, ownership del grupo/sesión en DB y backend; heredar códigos de error y correlación. | Rechazar campos actor en body y no confiar en `docente` enviado. |
| D06 Histórico | Marca nullable `procedenciaTemporal` de una sola fila; `NULL` = `INDETERMINADA`; solo valores confirmados reciben `Z`. | Depende del **nuevo work item DB** + clasificación por ID/entorno, firma de owner funcional. |
| D07 GET v2 | Mantener envelope existente; sumar `estadoTemporal` `CONFIRMADA/INDETERMINADA` y `procedenciaTemporal`. Confirmadas retornan UTC `Z`; indeterminadas retornan `fechaHoraInicio:null`, `fechaHoraFin:null`. | No devolver `LocalDateTime` ambiguo como instante; no eliminar filas del listado. En vista y JPA debe leerse la marca junto con las horas. |
| D08 OpenAPI | Documentar schemas `CrearSesionV2Request`, `ActualizarSesionV2Request`, `SesionV2`; security bearer, `x-roles: [DOCENTE]` en escrituras; 400/401/403/404 y fallo de dominio. | OpenAPI contractual versionada y aprobada por owner de contratos. |
| D09 Retrocompatibilidad | Rutas y payloads v1, GET de estudiante `/api/v1/estudiante/materias/{id}/sesiones`, exportación Excel asistencia y horarios recurrentes **sin cambios**. | No conectar codec a v1 ni reinterpretar sus registros; controles regresión explícitos. |

## Formato de errores HTTP v2
- `400`, `code=VALIDATION_ERROR`, `details=[{field:"fechaHoraInicio",code:"FIELD_INVALID_FORMAT",message:"..."}]` para fecha inválida o no RFC3339; `FIELD_REQUIRED` si falta. Otro campo inválido produce detalle independiente. No incluir texto proporcionado ni stacktrace en respuesta.
- Campos desconocidos (`docente`, `usuarioEjecutor`) `400 VALIDATION_ERROR` con `FIELD_UNKNOWN`.
- Cuando `fin <= inicio` **comparando instantes UTC**, conservar código de dominio vigente `SES_004` y su traducción actual, no convertirlo artificialmente en error de formato.
- No filtrar como `500` `IllegalArgumentException` del codec. Usar `RequestValidationException` y `ValidationResult`/mapeo existente o adaptador equivalente; evitar handler global indiscriminado para IllegalArgumentException.
- `401` sin token; `403` rol/ownership prohibido; `404` sesión inexistente según contrato existente. Mantener correlation ID y forma `ApiErrorResponse`.

## Ejemplo canónico confirmado
Entrada en Bogotá: `2026-07-15T20:00:00-05:00` a `2026-07-15T22:00:00-05:00`.
SQL UTC: `2026-07-16 01:00:00.0000000` a `2026-07-16 03:00:00.0000000`.
GET v2: `2026-07-16T01:00:00Z` a `2026-07-16T03:00:00Z`.
Visualización Berlín (verano): `16/07/2026 03:00–05:00`; Londres: `02:00–04:00`; Bogotá: `15/07/2026 20:00–22:00`.
En invierno Berlín tiene +01 y Londres +00: NO codificar offset fijo por ciudad.

## Ejemplo canónico indeterminado
```json
{"exitoso":true,"datos":{"sesion":"<uuid>","fechaHoraInicio":null,"fechaHoraFin":null,"estadoTemporal":"INDETERMINADA","procedenciaTemporal":null}}
```
La respuesta real preservará además los otros campos existentes de sesión. La UI muestra "Horario pendiente de verificación" sin inventar zona. **No** enviar hora legacy con `Z`, ni ocultar la sesión ni serializar hora legacy como 00:00.

## Orden de dependencias innegociable
1. DB owner publica esquema/SP/vistas y seguridad de escritura en un nuevo work item posterior a freeze, pruebas y rollback.
2. Owner funcional publica manifiesto de clasificación de históricos por identificador y ambiente, sin suposiciones automáticas. Las 3 sesiones locales evidenciadas permanecen NULL hasta decisión individual.
3. Contratos firma esta versión de D01–D09 y forma de D02/E1; actualizar OpenAPI y RED tests.
4. Codex implementa POST/PATCH/GET en backend y completa tests SQL reales + JWT; **solo después** se migra UI a v2, manteniendo v1.
5. No declarar READY_FOR_FRONTEND hasta evidencias reales y firmas; los tests RED previos son especificación, no una regresión de producción.

## Compatibilidad y rollback
Desplegar DB backward-compatible primero y backend v2 después; vigilar uso de v1. Ante incidente, revertir backend v2, frontend v2, luego DB únicamente tras respaldo/plan aprobado por owner. La eliminación de `procedenciaTemporal` sin exportación id/valor/evidencia pierde información no reconstruible. No aplicar DDL desde este repositorio.

## Conflictos abiertos detectados en la implementación (2026-10-09)
- **`TEST_CONTRACT_CONFLICT` D02 fracciones de segundo.** D02 de este documento (y HTTP-08 / `HttpUtcInstantCodecD02ContractRedTest.retainsExactlySevenDecimalPlacesWithoutRounding`) admite 1..7 decimales. La propuesta MAINT-003C [UTC_D02_ERROR_CONTRACT_PROPOSAL](../MAINT-003-backend-utc-pagination-readiness/UTC_D02_ERROR_CONTRACT_PROPOSAL.md) §1 rechaza toda fracción, y su test `HttpUtcInstantCodecStrictProfileRedTest.d02d04RejectsFractionalSecondsInsteadOfRoundingInDatetime2` exige rechazar `2026-07-15T14:00:00.5Z`. Ambos no pueden pasar. El codec implementa D02 (este documento) y el test 003C se conserva **intacto y en RED**, sin deshabilitar, por decisión del usuario. Resolución pendiente: owner de contratos + tester mediante nueva revisión conjunta de D02 y tests. Detalle en [VALIDATION.md](VALIDATION.md).
