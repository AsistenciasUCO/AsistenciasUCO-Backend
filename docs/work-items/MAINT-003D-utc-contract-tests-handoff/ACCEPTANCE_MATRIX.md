---
status: TEST_SPEC_READY_NOT_EXECUTED
work-item: MAINT-003D
---
# Matriz Given/When/Then — oráculos UTC v2 / v1 / frontend

Los escenarios no sustituyen tests realmente ejecutados. Incluir Java JUnit/MockMvc/OpenAPI y SQL IT con Docker SQL Server 2022 + Keycloak; sin `assumeTrue` que transforme ausencia de infraestructura en verde. Codex hará que los RED de MAINT-003B y MAINT-003C (si existen localmente) pasen sin borrar expectativas.

| ID | Given / When | Then |
|---|---|---|
| HTTP-01 | POST `2026-07-15T20:00:00-05:00` → `22:00-05:00` | 201; port recibe `2026-07-16 01:00`→`03:00` UTC, actor token, marca v2 |
| HTTP-02 | POST mismo instante expresado `2026-07-16T03:00:00+02:00` | SQL idéntico a HTTP-01; no restar offset dos veces |
| HTTP-03 | PATCH con offsets válidos y nombre | conserva id, ownership, actualiza instantes y marca en una sentencia |
| HTTP-04 | naive / `+02` / `+02:00:30` / falta segundos / 8–9 decimales / espacios | 400 `VALIDATION_ERROR`, `FIELD_INVALID_FORMAT` del campo preciso, 0 llamadas a input port |
| HTTP-05 | `null` / ausente en inicio o fin | 400 `VALIDATION_ERROR`, `FIELD_REQUIRED` y 0 escrituras |
| HTTP-06 | fecha imposible (29/02 no bisiesto), offset fuera de rango, 24:00 | 400 por campo, 0 persistencia |
| HTTP-07 | `fechaHoraFin` ≤ `fechaHoraInicio` **en UTC** (aunque wall-clock aparente ascendente) | rechazo de dominio `SES_004` existente, 0 filas mutadas |
| HTTP-08 | input `2026-07-15T14:00:00.1234567Z` | exactitud 7 decimales en `datetime2(7)`; no redondear ni truncar |
| HTTP-09 | body contiene `docente` o `usuarioEjecutor` | 400 `FIELD_UNKNOWN`, 0 escrituras |
| HTTP-10 | PUT v2 | 405, sin uso del caso de uso |
| READ-01 | fila `UTC_V2`, `UTC_GENERADOR`, `UTC_OWNER` | GET 200 con campos hora terminados `Z`, `CONFIRMADA`, procedencia correcta |
| READ-02 | fila legacy `procedenciaTemporal IS NULL` | GET 200, horas `null`, `estadoTemporal=INDETERMINADA`; no suposición, no filtrado en GET grupo |
| READ-03 | lista mixta (confirmadas + indeterminadas) | mantiene orden, número de sesiones e IDs y marca individual; sin transformación de todo el lote |
| READ-04 | reconsultar fila tras v1 PATCH | marca ahora NULL y GET v2 ya NO expone `Z` |
| DB-01 | UPDATE SQL directo horas 08→18 de fila confirmada | sentencia falla o se revierte sin dejar marca falsa; grants runtime probados |
| DB-02 | falla SP después de intento UPDATE | rollback simultáneo de horas y marca |
| DB-03 | owner clasifica una fila, no otra | solo primera cambia marca; evidencia por ID y ambiente, segunda sigue NULL |
| DB-04 | generar sesiones grupo repetido | idempotencia; marcas válidas; timezone institucional verificada |
| DB-05 | modificar sesión con `DetalleAsistencia` asociado | outcome según decisión documentada por owner; sin copias incoherentes inadvertidas |
| AUTH-01 | sin JWT | 401 |
| AUTH-02 | ESTUDIANTE/COORDINADOR/ADMIN no habilitados para crear/editar | 403 conforme permisos contrato; no convertir 403 en 501 |
| AUTH-03 | DOCENTE sin titularidad | 403 o código controlado de autorización ya existente; sin mutación |
| AUTH-04 | DOCENTE titular | éxito con user ID del token (no del body) |
| REG-01 | POST/PATCH/GET v1 con wire naive previo | shape/semántica/status/error `ERR_FECHA_HORA_INVALIDA` sin cambios |
| REG-02 | `/api/v1/estudiante/materias/{id}/sesiones` | contrato sin cambio ni conversión silenciosa |
| REG-03 | descarga Excel asistencia | columnas/horarios v1 idénticos; snapshot probado |
| REG-04 | SSE + generación horarios recurrentes | sin reinterpretación `LocalTime` ni cambio de contratos |
| UI-01 | sesión Bogotá `20:00–22:00`, mismo GET UTC, zona `America/Bogota` | `15/07 20:00–22:00` |
| UI-02 | usuario selecciona `Europe/Berlin` y recarga | `16/07 03:00–05:00`, fecha cambia; misma sesión/id/instant |
| UI-03 | selecciona `Europe/London` en verano | `16/07 02:00–04:00` |
| UI-04 | navegador en `Europe/Berlin` invierno | offset +01 para enero; no usar +02 fijo |
| UI-05 | DST gap Berlin `2026-03-29 02:30` | impedir submit y mostrar "hora inexistente" |
| UI-06 | DST overlap Berlin `2026-10-25 02:30` | elegir explícitamente +02 o +01; instantes distintos |
| UI-07 | fila `INDETERMINADA` | mostrar texto sin horario falso, bloqueo de edición instantánea hasta decisión |
| UI-08 | cambiar timezone en sesión abierta, refrescar y cambiar filtros | actualizar TODAS las superficies v2; ninguna transformación acumulativa |
| UI-09 | móvil/browser sin zona IANA válida | zona de visualización fallback explícita `America/Bogota`, señal visible, nunca conversión silenciosa de escritura |

## Oráculo de precisión y gramática
Patrón lexical de referencia: `^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,7})?(?:Z|[+-]\\d{2}:\\d{2})$`. Adicionalmente validar fecha real y offset `ZoneOffset`; regex sola insuficiente. El valor SQL Server `datetime2(7)` tiene precisión máxima 100 ns. No rellenar segundos faltantes ni redondear fracciones >7.

## Ejecución / evidencia
- Unitarios existentes `HttpUtcInstantCodecTest` + strict MAINT-003C si presentes.
- Contract RED `SesionV2HttpContractRedTest` y `OpenApiSesionesV2ContractRedTest`; sumar read/provenance RED de esta rama.
- `./mvnw -B -ntp clean verify` en Java25 (RED intencional antes implementación; no etiquetar como CI failure de producción).
- `./mvnw -B -ntp -Pintegration verify` con SQL real: aserciones de tablas, vista, historia, roles, race, rollback y exportación. Reportar test count, 0 skips focales.
- Angular `npm ci && npm test -- --watch=false` (ajustar comando a package scripts reales), scripts zona horaria con `TZ=America/Bogota`, `TZ=Europe/Berlin`, `TZ=Europe/London`; además tests de override explícito in-app; tests E2E con recarga.
- Capturar YAML OpenAPI diff, SHA DB/backend/frontend, statement trace redacted, 401/403, captura DB before/after, reporte de clasificación firmado. No credenciales ni tokens en evidencias.
