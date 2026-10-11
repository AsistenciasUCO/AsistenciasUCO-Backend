---
status: proposed
type: contract-decision
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---

# UTC-D02 — perfil de wire y contrato de errores de sesiones v2

**Estado:** `CONTRACT_DECISION_REQUIRED` (rol contratos). No modifica el OpenAPI canónico ni v1. Hallazgos de origen: [MAINT-003C_INDEPENDENT_REVIEW](MAINT-003C_INDEPENDENT_REVIEW.md) §5 (D02-F1..F6).

## 1. Perfil de entrada (D02-a)

Patrón propuesto para `fechaHoraInicio`/`fechaHoraFin` en `CrearSesionV2Request` y `ActualizarSesionV2Request`:

```text
^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(Z|[+-]\d{2}:\d{2})$      format: date-time
```

| Entrada | Decisión | Motivo |
|---|---|---|
| `2026-07-15T16:00:00+02:00`, `...Z`, `...-05:00` | acepta | D02 |
| `2026-07-15T14:00:00-00:00` | acepta como UTC | RFC 3339 §4.3: el instante UTC es conocido |
| `2026-07-15T16:00:00` (naive) | rechaza | D02; no se infiere zona |
| `+02`, `+0200`, `+02:00:30` | rechaza | fuera de `Z|±HH:mm` (hoy el codec acepta `+02` y `+02:00:30`) |
| `2026-07-15T16:00+02:00` (sin segundos) | rechaza | RFC 3339 exige segundos |
| fracciones de segundo | rechaza | v1 tampoco las admite; `datetime2(7)` no guarda 9 dígitos sin redondeo (UTC-D04); sesiones son de granularidad de minuto |
| sufijo `[Europe/Berlin]`, fechas/horas imposibles | rechaza | no es RFC 3339 / inválido |

El codec debe aplicar exactamente el patrón publicado (o un formatter estricto equivalente), no `ISO_OFFSET_DATE_TIME` en modo lenient. Pregunta abierta para contratos: aceptar `t`/`z` en minúscula (RFC 3339 lo permite); la propuesta los rechaza por coherencia con el patrón.

## 2. Errores (D02-b..e)

| Situación | HTTP | `code` | `details[]` | Llega al puerto |
|---|---:|---|---|---|
| fecha ausente o en blanco | 400 | `VALIDATION_ERROR` | `{field, code: FIELD_REQUIRED}` | no |
| fecha fuera del perfil (incl. naive) | 400 | `VALIDATION_ERROR` | `{field, code: FIELD_INVALID_FORMAT}` | no |
| varios campos inválidos (`grupo`, `nombre`, fechas) | 400 | `VALIDATION_ERROR` | un ítem por campo, en una sola respuesta | no |
| campo desconocido (`docente`, `usuarioEjecutor`, …) | 400 | `VALIDATION_ERROR` | `FIELD_UNKNOWN` (mecanismo existente) | no |
| JSON no interpretable | 400 | `INVALID_REQUEST` | — | no |
| `fin <= inicio` tras normalizar a UTC | 400 | `ERR_RANGO_FECHAS_SESION_INVALIDO` | — | sí (regla de dominio, igual que v1) |
| sesión inexistente / no titular | 404 / 403 | `RESOURCE_NOT_FOUND`/`ERR_SESION_NO_EXISTE`, `FORBIDDEN` | — | sí (AS-IS) |
| valor de procedencia rechazado por la DB | 500 | `INTERNAL_ERROR` | — | sí (error de programación, no del cliente) |

Reglas:

1. PATCH v2 se valida en HTTP igual que POST (v1 PATCH no tiene validador; v2 no hereda esa brecha).
2. Ningún mensaje ni `details[].message` repite el valor recibido.
3. El rango se compara sobre instantes UTC, nunca sobre el reloj de pared enviado.
4. **v1 no cambia:** `ERR_FECHA_HORA_INVALIDA` sin `details` sigue siendo su contrato (Golden Path §G) y la guarda de PR #20 lo verifica. La divergencia v1/v2 es intencional y se documenta en ambos OpenAPI.

Alternativa considerada y no recomendada: validar el rango también en HTTP como `FIELD_OUT_OF_RANGE`. Duplica la regla de dominio y haría divergir v1 y v2 en un caso que hoy es idéntico.

## 3. Trazabilidad RED (rama `jose-valencia/maint-003c-utc-v2-red-hardening`)

| Regla | Test |
|---|---|
| perfil `+02`, `+02:00:30`, sin segundos, fracciones | `HttpUtcInstantCodecStrictProfileRedTest` (4 RED) |
| `-00:00`, sufijo de zona, fechas imposibles, no eco, salida con segundos y `Z` | mismo test (4 GREEN de caracterización) |
| `FIELD_REQUIRED`, `FIELD_INVALID_FORMAT` (no 500), agregación, no eco HTTP, PATCH validado | `SesionV2HttpContractRedTest` (5 nuevos RED) |
| rango sobre instantes, procedencia | diseño IT: UTC-IT-10 y UTC-IT-09/11 (requieren endpoint y SQL real) |

**Firma contratos:** PENDIENTE.
