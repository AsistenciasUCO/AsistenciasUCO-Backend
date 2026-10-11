---
status: proposed
type: contract-decision
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---

# UTC-D06 — propuesta definitiva para el owner DB: procedencia temporal de `dbo.Sesion`

**Estado:** `CONTRACT_DECISION_REQUIRED`. Destinatarios: owner DB (`johnjduque/gestion-asistencia-db`) y owner funcional. Este documento **no** autoriza DDL, cambios de SP/vistas, `UPDATE` de horas ni activación de `/api/v2/**`. El backend no administra el esquema (AGENTS.md); la ejecución corresponde al owner DB en un work item propio posterior al freeze DB-GP-001C.

Origen: alternativas en [UTC_D06_HISTORICAL_PROVENANCE_OPTIONS](UTC_D06_HISTORICAL_PROVENANCE_OPTIONS.md), auditadas en [MAINT-003C_INDEPENDENT_REVIEW](MAINT-003C_INDEPENDENT_REVIEW.md) §4 (hallazgos D06-F1..F9). Texto SQL ilustrativo, no ejecutable: [UTC_D06_SQL_DRAFT](UTC_D06_SQL_DRAFT.md).

## 1. Decisión que se solicita

Adoptar **A+C+E corregida**:

- **A** — marca de procedencia por fila en `dbo.Sesion`, escrita por los SP en la misma sentencia que las horas.
- **C** — clasificación de filas existentes por ID y por entorno, decidida por el owner funcional, ejecutada por el owner DB.
- **E1** — GET v2 emite `Z` solo con procedencia confirmada; el resto se presenta como indeterminado sin reinterpretar.
- **F** sigue vigente (v2 apagado, guarda de PR #20) hasta que A esté desplegada en el entorno y E1 aprobada.

Si el owner rechaza A, la vía alternativa es C + D + E y **no** se activan escrituras v2 (serían indistinguibles de las v1).

## 2. Esquema de procedencia

| Elemento | Propuesta | Motivo |
|---|---|---|
| Columna | `dbo.Sesion.procedenciaTemporal NVARCHAR(30) NULL` (nombre final del owner) | `NULL` = indeterminada; nullable evita reescribir filas y el `ADD` es solo metadatos |
| Dominio | `CHECK (procedenciaTemporal IN (N'UTC_CONFIRMADO_V2', N'UTC_GENERADO_DB', N'UTC_CLASIFICADO_OWNER'))` | cerrado; `NULL` admitido por semántica de `CHECK` |
| Sin default | ningún `DEFAULT` | una fila nueva sin marca explícita queda indeterminada (fail-closed) |
| Alternativa A2 | FK a un catálogo pequeño (`id`, `codigo`, `nombre`) | si el owner prefiere su patrón de catálogos; mismo comportamiento |
| Trazabilidad opcional | `procedenciaTemporalCorrelacion UNIQUEIDENTIFIER NULL` = `@idCorrelacion` de la escritura que fijó la marca | enlaza con `AuditoriaEvento`/logs sin depender de ellos |
| `DetalleAsistencia` | **sin columna** | sus copias pueden estar desactualizadas (D06-F6) y no se exponen por HTTP; su semántica deriva de la sesión. Pregunta aparte al owner |

Significado de cada valor:

| Valor | Quién lo escribe | Garantía |
|---|---|---|
| `UTC_CONFIRMADO_V2` | `usp_crear_sesion`/`usp_actualizar_sesion` invocados por la API v2 | el cliente envió offset explícito y el backend normalizó a UTC |
| `UTC_GENERADO_DB` | `usp_generar_sesiones_grupo` | conversión DB `AT TIME ZONE TIEMPO/ZONA_HORARIA_SQLSERVER → UTC` |
| `UTC_CLASIFICADO_OWNER` | proceso auditado del owner (C) | el owner funcional confirmó UTC o el owner DB convirtió la fila con evidencia |
| `NULL` | v1, semillas sin clasificar, escrituras fuera de SP | ninguna: `HISTORICAL_TZ_UNDETERMINED` |

## 3. Reglas de actualización

| Ruta de escritura | Marca resultante |
|---|---|
| `POST /api/v2/sesiones` → `usp_crear_sesion @procedenciaTemporal = 'UTC_CONFIRMADO_V2'` | `UTC_CONFIRMADO_V2` |
| `PATCH /api/v2/sesiones/{id}` → `usp_actualizar_sesion @procedenciaTemporal = 'UTC_CONFIRMADO_V2'` | `UTC_CONFIRMADO_V2` (también si la fila era `NULL`: las horas nuevas son confirmadas) |
| `POST /api/v1/sesiones` (parámetro omitido) | `NULL` |
| `PATCH`/`PUT /api/v1/sesiones/{id}` (parámetro omitido) | `NULL` — **degrada** una fila confirmada, porque la zona del cliente v1 es desconocida |
| `usp_generar_sesiones_grupo` | `UTC_GENERADO_DB` |
| Proceso de clasificación del owner (C) | `UTC_CLASIFICADO_OWNER` o sin cambio |
| Semillas / scripts del owner | marca explícita; una semilla que escribe reloj local deja `NULL` |
| `UPDATE` directo de `fechaHora*` fuera de SP | `NULL` (regla de contrato; opción de trigger `AFTER UPDATE` que la haga cumplir, a decisión del owner) |

Invariantes:

1. Horas y marca cambian en **la misma sentencia** `INSERT`/`UPDATE` (D06-F3).
2. Los SP públicos solo aceptan `NULL` o `UTC_CONFIRMADO_V2` en el parámetro; `UTC_GENERADO_DB` y `UTC_CLASIFICADO_OWNER` no son asignables por el backend. Un valor fuera de esa lista es error de programación: el owner asigna un `DBCODE` nuevo y el backend lo trata como `500 INTERNAL_ERROR` (no es error del cliente).
3. Nunca se infiere procedencia por fecha de creación, rango horario ni ausencia de auditoría.
4. `usp_actualizar_sesion` siempre reemplaza horas (son obligatorias); por tanto siempre reescribe la marca.

## 4. Impacto en procedimientos almacenados y vistas

| Objeto | Cambio | Compatibilidad |
|---|---|---|
| `dbo.Sesion` | `ADD procedenciaTemporal` + `CHECK` | filas existentes quedan `NULL`; sin reescritura |
| `usp_crear_sesion` | parámetro final opcional `@procedenciaTemporal NVARCHAR(30) = NULL`; validación de dominio; columna en el `INSERT` | backend v1 invoca con parámetros nombrados → no cambia |
| `usp_actualizar_sesion` | mismo parámetro; `SET ... procedenciaTemporal = @procedenciaTemporal` en el mismo `UPDATE` | idem; v1 degrada a `NULL` por diseño |
| `usp_generar_sesiones_grupo` | columna en el `INSERT` con `'UTC_GENERADO_DB'` | firma sin cambio |
| `uv_sesion` | añadir `procedenciaTemporal` al final de la lista explícita | consumidores por nombre de columna no cambian |
| `uv_auth_sesion` | idem (proyecta `uv_sesion`) | idem; política fail-closed intacta |
| `uv_asistencia`, `uv_detalle_asistencia` | sin cambio | no proyectan la columna |
| `usp_registrar_asistencias_sesion`, `usp_registrar_asistencia_estudiante_autonomo`, `usp_ejecutar_cierre_masivo_periodo`, `usp_radicar/resolver_solicitud_revision_asistencia`, `usp_sincronizar_asistencia_estudiante_interno`, `usp_validar_*_interno` | sin cambio | no escriben horas de `Sesion` |
| Semilla `14_grupos_sesiones.sql` | marca explícita (`NULL` para las dos sesiones de ejemplo) | solo entornos de desarrollo |
| Freeze DB-GP-001C | nuevo work item, actualizar `DB_BASELINE_CONTRACT` (§Sesion, §tiempo) y regenerar manifest | obligatorio por el propio freeze |

Impacto backend (fase posterior a la firma, no ahora): `SesionJpaRepository` añade `@procedenciaTemporal = :procedenciaTemporal` solo en las llamadas de v2; `UvSesionEntity` mapea la columna para GET v2; v1 no cambia ninguna línea. Si v2 se despliega contra una base sin la migración, SQL Server rechaza el parámetro desconocido → error 500 fail-closed, nunca una escritura sin marca.

## 5. Coexistencia v1/v2 y orden de despliegue

1. **Owner DB**: migración aditiva (columna, CHECK, SP con parámetro opcional, vistas) en cada entorno. v2 sigue apagado.
2. **Backend**: rerun `-Pintegration verify` contra la base migrada (clon) para demostrar v1 sin regresión; guarda de PR #20 verde.
3. **Owner funcional + DB**: clasificación C por entorno (§6).
4. **Contratos**: aprobar D01–D09, perfil y errores D02 ([propuesta](UTC_D02_ERROR_CONTRACT_PROPOSAL.md)) y forma E1.
5. **Backend**: micro-PR v2 POST/PATCH con marca; después GET v2 con E1. Pruebas reales POST/GET/PATCH con SQL Server, JWT, DST.
6. v1 se mantiene; su deprecación es decisión separada (UTC-D09).

Mientras 1 no esté desplegado en un entorno, ese entorno no puede activar escrituras v2: perdería la distinción v1/v2 de forma irreversible.

## 6. Clasificación histórica (C)

Procedimiento por entorno, con las filas locales como primer caso:

1. Inventario de solo lectura: `id`, `grupo`, horas, existencia de `CREAR_SESION`/`GENERAR_SESIONES_GRUPO` en `AuditoriaEvento`, horario del grupo.
2. Owner funcional decide por ID: `UTC` (confirmada con evidencia externa, p. ej. horario real de la clase), `HORA_LOCAL_America/Bogota`, `DATO_DE_PRUEBA` o `SIN_DECISION`.
3. Owner DB ejecuta: `UTC` → marca `UTC_CLASIFICADO_OWNER` sin tocar horas; `HORA_LOCAL` → conversión auditada `AT TIME ZONE 'SA Pacific Standard Time' AT TIME ZONE 'UTC'` + marca, guardando valores previos en una tabla de respaldo; `DATO_DE_PRUEBA` → retiro por el proceso del owner; `SIN_DECISION` → `NULL` permanente.
4. Evidencia versionada en el repo owner (IDs, antes/después, firma).

Entorno local (`gestionasistenciadb`, lectura 2026-10-09):

| ID | Almacenado | Lectura UTC (Bogotá) | Lectura reloj local | Evidencia | Recomendación |
|---|---|---|---|---|---|
| `B2C3D4E5-F6A7-8B9C-0D1E-2F3A4B5C6D7E` | 2026-08-17 18:00–21:00 | 13:00–16:00 | 18:00–21:00 | semilla del owner dice 08:00; cambio sin auditoría | `SIN_DECISION` o `DATO_DE_PRUEBA` |
| `C3D4E5F6-A7B8-9C0D-1E2F-3A4B5C6D7E8F` | 2026-08-24 18:00–21:00 | 13:00–16:00 | 18:00–21:00 | idem | idem |
| `13C1754A-B1B8-4227-95AA-B8602E6B33E8` | 2026-10-06 20:00–22:00 | 15:00–17:00 | 20:00–22:00 | `POST /api/v1` a las 20:32Z; ninguna lectura se descarta | `SIN_DECISION` o `DATO_DE_PRUEBA` |

El backend no propone `UTC` ni `HORA_LOCAL` para ninguna: la evidencia no decide (`HISTORICAL_TZ_UNDETERMINED`).

## 7. Forma E1 para GET v2 (para aprobación de contratos)

```json
{ "fechaHoraInicio": "2026-07-15T14:00:00Z", "fechaHoraFin": "2026-07-15T15:00:00Z",
  "semanticaTemporal": "UTC" }
```

```json
{ "fechaHoraInicio": null, "fechaHoraFin": null,
  "fechaHoraInicioSinZona": "2026-10-06T20:00:00", "fechaHoraFinSinZona": "2026-10-06T22:00:00",
  "semanticaTemporal": "INDETERMINADA" }
```

- `semanticaTemporal = UTC` solo con marca no nula; nunca `Z` para `NULL`.
- Listas por grupo conservan todas las filas (sin exclusión); el cliente decide cómo mostrar las indeterminadas.
- Los nombres exactos los fija contratos con RED de GET v2 antes de implementar.

## 8. Rollback

| Capa | Acción | Efecto |
|---|---|---|
| Backend v2 | revert del micro-PR v2 | v1 intacto; la marca deja de escribirse `UTC_CONFIRMADO_V2`; filas ya marcadas conservan su valor |
| Backend GET v2 | revert independiente | sin efecto en datos |
| DB (preferido) | dejar la columna y volver los SP a la versión previa del repo owner | sin pérdida; nuevas escrituras sin marca = `NULL` (correcto) |
| DB (completo) | solo tras revertir backend v2: respaldar `(id, procedenciaTemporal, procedenciaTemporalCorrelacion)` en tabla de archivo; restaurar SP/vistas; `DROP CONSTRAINT`; `DROP COLUMN`; regenerar manifest | sin respaldo, la pérdida de marcas es **irreversible**: las filas v2 vuelven a ser indistinguibles |
| Clasificación C | restaurar horas desde la tabla de respaldo del paso 6.3 y poner marca `NULL` | requiere que el owner haya guardado los valores previos |

Orden obligatorio: backend antes que DB. Revertir la DB con v2 activo provoca 500 en escrituras v2 (fail-closed, sin corrupción).

## 9. Pruebas que deberán acompañar cada lado

Owner DB (en su repo): v1 sin parámetro → `NULL`; v2 → `UTC_CONFIRMADO_V2`; v1 sobre fila confirmada → `NULL`; generación → `UTC_GENERADO_DB`; valor no permitido → `DBCODE` nuevo sin escritura; CHECK rechaza valores fuera de dominio; vistas proyectan la columna; manifest regenerado.

Backend (tras firma): UTC-IT-01..09 de [UTC_V2_MICRO_PR_DESIGN](UTC_V2_MICRO_PR_DESIGN.md) más UTC-IT-10 (rango juzgado sobre instantes) y UTC-IT-11 (GET v2 de fila `NULL` sin `Z`), en clon aislado, con JWT real y 0 skips focales.

## 10. Firmas

| Rol | Decisión | Estado |
|---|---|---|
| Owner DB | A (o A2) y su forma; nuevo work item post-freeze | PENDIENTE |
| Owner funcional | clasificación C por ID y entorno; obsolescencia de copias en `DetalleAsistencia` | PENDIENTE |
| Contratos | D01–D09, perfil/errores D02, forma E1 | PENDIENTE |
