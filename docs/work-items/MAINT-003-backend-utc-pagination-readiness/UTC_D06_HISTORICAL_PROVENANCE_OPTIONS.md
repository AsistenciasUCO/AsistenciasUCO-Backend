---
status: proposed
type: contract-decision
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---
> **Archivo de evidencia histórica (2026-10-09).** Registra decisiones y pruebas de la fase inicial. Desde entonces los PR #18, #19 y #20 se fusionaron a `develop` el 2026-10-10. La implementación UTC v2 de MAINT-003K sigue en una rama separada, todavía sin integración a `develop`; la DB correspondiente también sigue pendiente de PR y aprobación. Los estados `OPEN`, `NOT_READY`, `NOT_RUN` y las propuestas siguientes describen el momento en que se redactó este documento, no el estado actual de todos los repositorios. Ver `PR21_CONFLICT_RESOLUTION.md`.


# UTC-D06 — procedencia de sesiones históricas: alternativas

**Estado:** `CONTRACT_DECISION_REQUIRED`. Ninguna alternativa está aprobada. Este documento no autoriza `UPDATE`, DDL, reinterpretación de filas, fecha de corte ni activación de `GET /api/v2/**`.

## 1. Problema

UTC-D06 propone que GET v2 bloquee o marque filas históricas "hasta que el owner resuelva su procedencia", pero no dice **cómo distingue el backend una fila histórica ambigua de una fila UTC fiable**. Con el esquema actual esa distinción no existe:

- `dbo.Sesion` solo tiene `id, nombre, numero, codigo, numeroSemana, grupo, fechaHoraInicio, fechaHoraFin` (todas NOT NULL). No hay columna de origen, versión de wire ni fecha de escritura.
- Mientras v1 siga aceptando escrituras (`POST`, `PATCH`, `PUT` deprecated), cada escritura v1 puede producir una fila nueva con la misma ambigüedad. Por eso **una fecha de corte no resolvería el problema** aunque se conociera: la ambigüedad depende de la ruta de escritura de cada fila, no del momento.

## 2. Evidencia recogida (solo lectura, 2026-10-09)

| Hecho | Fuente |
|---|---|
| Existen tres rutas de escritura: `usp_crear_sesion` (v1 POST), `usp_actualizar_sesion` (v1 PUT/PATCH) y `usp_generar_sesiones_grupo` | repo owner DB `f2871a9`, `docs/arquitectura/especificacion_sp_endpoints.md` |
| `usp_generar_sesiones_grupo` sí convierte hora local + `TIEMPO/ZONA_HORARIA_SQLSERVER` (`SA Pacific Standard Time`) a UTC con `AT TIME ZONE` | `schema/stored-procedures/usp_generar_sesiones_grupo.sql:233-234` |
| `usp_crear_sesion` y `usp_actualizar_sesion` reciben `datetime2` y no convierten; el comentario declara "instantes UTC" | `usp_crear_sesion.sql:95` |
| Parámetros vivos: `TIEMPO/ALMACENAMIENTO_INSTANTES=UTC`, `ZONA_HORARIA_IANA=America/Bogota` | `dbo.CatalogoParametro` |
| Sesiones 1 y 2 (`B2C3D4E5-…`, `C3D4E5F6-…`) son semillas del owner con `08:00–10:00`; la base viva tiene `18:00–21:00` (180 min) | `schema/seed/14_grupos_sesiones.sql:27-28` vs `dbo.Sesion` |
| `AuditoriaEvento` (127 filas desde 2026-09-23) no contiene ninguna actualización de sesión: la modificación 08:00→18:00 no es trazable | consulta `action/resourceType` |
| Sesión 3 (`13c1754a-…`) se creó por `POST /api/v1/sesiones` el `2026-10-06T20:32:17Z` (15:32 Bogotá) con inicio almacenado `20:00` | `AuditoriaEvento` `CREAR_SESION` |
| Ninguna SP de asistencia valida ventana horaria contra la sesión (no hay evidencia indirecta por éxito de registro) | `usp_registrar_asistencias_sesion*.sql` |
| `usp_sincronizar_asistencia_estudiante_interno` copia `fechaHoraInicio/Fin` de la sesión a `DetalleAsistencia`: la ambigüedad se propaga a datos derivados | líneas 58-59 y 154-163 |
| Contra `uv_horario` del grupo, 0/3 coinciden como reloj local y 0/3 como UTC→Bogotá | `utc_hypothesis_diagnostics.sql` |

Lectura de la sesión 3: almacenada `20:00`. Si es UTC equivale a 15:00 Bogotá (empezó 32 minutos antes de crearse). Si es reloj local, empieza 4 h 28 min después de crearse. Ambas son plausibles; la evidencia **no** decide.

## 3. Contrato congelado con el que se valida cada alternativa

| Fuente | Qué fija |
|---|---|
| OpenAPI canónico `LocalSessionDateTime` | wire v1 ISO local sin offset, `x-persistence-semantics: UTC`; migrar exige fase contractual separada |
| `OPENAPI_STANDARD.md` fila Tiempo | instantes nuevos con UTC u offset explícito; el wire legacy de sesión queda congelado |
| `TECHNICAL_DEBT.md` TD-005 | no convertir horas existentes por inferencia |
| `DB_BASELINE_CONTRACT.md` (owner DB) | `DATETIME2`: instante UTC **target** cuando representa eventos |
| AGENTS.md | el backend no administra el esquema DB |

Tensión documentada: el contrato declara UTC como semántica *target* de v1, pero la evidencia muestra semillas del owner escritas como hora de clase (`08:00`, 03:00 Bogotá si fuera UTC) y filas alteradas fuera de auditoría. Declarar "todo es UTC porque el contrato lo dice" sería reinterpretar por inferencia (TD-005).

## 4. Alternativas

Ninguna reescribe `fechaHoraInicio/Fin` ni usa fecha de corte.

### A — Marca de procedencia por fila, gestionada por el owner DB (recomendada)

Columna nullable nueva en `dbo.Sesion` (nombre a decidir por el owner, p. ej. `semanticaTemporal`) con valores cerrados, por ejemplo `UTC_CONFIRMADO_V2`, `UTC_GENERADO_DB`, y `NULL` = indeterminado. `usp_generar_sesiones_grupo` escribe `UTC_GENERADO_DB`; una variante o parámetro opcional de `usp_crear_sesion`/`usp_actualizar_sesion` escribe `UTC_CONFIRMADO_V2` solo cuando llama v2; cualquier escritura v1 deja o devuelve el valor a `NULL`. Las tres filas actuales quedan `NULL` sin tocar sus horas.

- Contrato congelado: v1 intacto (wire y SP con parámetro opcional por defecto). Cambia `DB_BASELINE_CONTRACT` → **requiere decisión y migración del owner DB**; el backend no la ejecuta.
- Resuelve coexistencia v1/v2 por fila y la reedición posterior por v1.
- Coste: migración + cambio de firma SP + vistas `uv_sesion`. `DetalleAsistencia` necesitaría la misma regla o leer la marca de su sesión.

### B — Procedencia derivada de `AuditoriaEvento`, sin cambio de esquema

GET v2 sirve `Z` solo si la última escritura auditada de la sesión es v2.

- Contrato congelado: no cambia esquema; pero la auditoría es contrato técnico, no fuente de negocio.
- Falla con la evidencia actual: hay escrituras fuera de auditoría (seed 08:00→18:00); en `SesionController` solo `CREAR_SESION` y `GENERAR_SESIONES_GRUPO` llevan `@AuditableOperation` (PATCH/PUT v1 no se auditan) y la generación se audita con `resourceType=GRUPO`, sin IDs de sesión. Ausencia de evento no prueba procedencia.
- **No recomendada** como fuente única; a lo sumo señal complementaria.

### C — Clasificación firmada por el owner de las tres filas existentes (inventario por ID)

El owner funcional/DB decide, por `id`, una de: `UTC`, `HORA_LOCAL_America/Bogota` o `DATO_DE_PRUEBA` (retiro por proceso propio del owner, no por el backend). La decisión se versiona con evidencia.

- Contrato congelado: compatible; no reescribe horas.
- Por sí sola no evita nuevas filas ambiguas mientras v1 escriba. Debe combinarse con A o con E.
- Si se elige "hora local", la conversión la hace el owner DB con su propio proceso auditado; el backend no la automatiza.

### D — Cierre de escritura v1 antes de activar GET v2

Retirar o desactivar POST/PATCH/PUT v1 (tras inventario de consumidores, UTC-D09) para que el conjunto de filas ambiguas deje de crecer; las existentes se identifican por inventario de IDs (no por fecha) y se tratan con C.

- Contrato congelado: es un cambio contractual de v1 (retiro de operaciones) → decisión separada con consumidores.
- No requiere esquema si el inventario es pequeño y la decisión del owner se materializa en datos que el owner gestione.

### E — Presentación explícita de indeterminación en GET v2

Para una fila sin procedencia confirmada, GET v2 no emite instante con `Z`; devuelve, por ejemplo, `fechaHoraInicio: null`, el valor almacenado tal cual en un campo distinto sin offset y un marcador `semanticaTemporal: "INDETERMINADA"`. Alternativa: excluir esas filas de GET v2 y mantenerlas solo en v1.

- Contrato congelado: no reinterpreta; v1 sigue sirviendo el valor legacy.
- Necesita saber qué filas son indeterminadas → depende de A (o de C+D).
- Requiere decidir la forma exacta en OpenAPI v2 antes del RED de GET.

### F — Opción nula: no activar GET v2 (statu quo vigente)

Solo existe v1. Es lo que garantiza hoy la guarda `SesionUtcActivationGuardTest` (PR #20). No resuelve nada, pero es seguro mientras no haya decisión.

## 5. Recomendación para el owner

**A + C + E**, con F vigente hasta la firma:

1. Owner DB decide si acepta la marca por fila (A) y su forma; si la rechaza, la vía es **C + D + E** y POST/PATCH v2 tampoco deben activarse, porque escrituras v2 sin marca serían indistinguibles después de las v1.
2. Owner funcional clasifica las tres filas por ID (C).
3. Contratos define la forma de E en OpenAPI v2 y el tester añade el RED de GET v2.

Implicación importante: con la opción A, **activar POST/PATCH v2 antes de que exista la marca destruye información** (filas UTC confirmadas que luego no se podrán separar de las v1). Por eso el micro-PR v2 queda completo en diseño y RED, pero no se implementa.

## 6. Preguntas que el owner debe responder (sin respuesta inventada)

1. ¿Acepta el owner DB una marca de procedencia por fila en `dbo.Sesion` (y en `DetalleAsistencia` o vía su sesión)? ¿Nombre, dominio y default?
2. Para las sesiones `B2C3D4E5-…`, `C3D4E5F6-…` y `13c1754a-…`: ¿UTC, hora local Bogotá o dato de prueba? ¿Quién hizo el cambio 08:00→18:00 de las semillas?
3. ¿Qué consumidores escriben hoy v1 y qué zona envía cada uno? (requisito de D y de UTC-D09)
4. ¿Forma de GET v2 para filas indeterminadas: campo marcador, exclusión o 409/otro? (API_DESIGN_RULES pide condición real para cualquier estado nuevo.)

**Firma owner DB:** PENDIENTE. **Firma owner funcional:** PENDIENTE. **Contratos:** PENDIENTE.
