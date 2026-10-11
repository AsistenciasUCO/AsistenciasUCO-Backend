---
status: proposed
type: contract-decision
scope: backend
owner: backend-team
last-reviewed: 2026-10-09
---

# UTC-D06 — borrador SQL ilustrativo para el owner DB

> **NO EJECUTABLE / NO EJECUTADO.** Texto de apoyo a [UTC_D06_OWNER_DB_PROPOSAL](UTC_D06_OWNER_DB_PROPOSAL.md). No es una migración del backend, no se ha ejecutado en ninguna base y no autoriza DDL. El owner DB decide nombres, `DBCODE`, mensajes de catálogo y lo implementa en `johnjduque/gestion-asistencia-db` con su work item post-freeze, sus tests y la regeneración del manifest DB-GP-001C. Los fragmentos omiten el cuerpo vigente de los SP; solo marcan los puntos de cambio.

## 1. Columna y dominio

```sql
ALTER TABLE dbo.Sesion ADD procedenciaTemporal NVARCHAR(30) NULL;            -- sin DEFAULT: NULL = indeterminada
ALTER TABLE dbo.Sesion ADD procedenciaTemporalCorrelacion UNIQUEIDENTIFIER NULL; -- opcional
ALTER TABLE dbo.Sesion WITH CHECK ADD CONSTRAINT CK_Sesion_ProcedenciaTemporal
    CHECK (procedenciaTemporal IN (N'UTC_CONFIRMADO_V2', N'UTC_GENERADO_DB', N'UTC_CLASIFICADO_OWNER'));
```

## 2. `usp_crear_sesion` (puntos de cambio)

```sql
-- firma: parámetro final opcional; las llamadas nombradas del backend v1 no cambian
    @idUsuarioEjecutor     UNIQUEIDENTIFIER = NULL,
    @procedenciaTemporal   NVARCHAR(30)     = NULL
...
-- validación: el backend solo puede declarar escritura v2
IF @estadoResultado = 1 AND @procedenciaTemporal IS NOT NULL
   AND @procedenciaTemporal <> N'UTC_CONFIRMADO_V2'
BEGIN
    -- DBCODE nuevo a asignar por el owner (p. ej. SES_005), sin escritura
END
...
INSERT INTO dbo.Sesion (id, nombre, numero, codigo, numeroSemana, grupo,
                        fechaHoraInicio, fechaHoraFin, procedenciaTemporal, procedenciaTemporalCorrelacion)
VALUES (..., @fechaHoraInicio, @fechaHoraFin, @procedenciaTemporal,
        CASE WHEN @procedenciaTemporal IS NULL THEN NULL ELSE @idCorrelacion END);
```

## 3. `usp_actualizar_sesion` (puntos de cambio)

```sql
    @procedenciaTemporal   NVARCHAR(30)     = NULL   -- misma validación que en crear
...
UPDATE dbo.Sesion
SET nombre = ...,                                     -- sin cambio
    fechaHoraInicio = @fechaHoraInicio,
    fechaHoraFin    = @fechaHoraFin,
    procedenciaTemporal = @procedenciaTemporal,       -- v1 (omitido) => NULL: degrada
    procedenciaTemporalCorrelacion = CASE WHEN @procedenciaTemporal IS NULL THEN NULL ELSE @idCorrelacion END
WHERE id = @idSesionDefecto;                          -- misma sentencia que las horas
```

## 4. `usp_generar_sesiones_grupo` (punto de cambio)

```sql
INSERT INTO dbo.Sesion (..., fechaHoraInicio, fechaHoraFin, procedenciaTemporal, procedenciaTemporalCorrelacion)
VALUES (..., @fechaHoraInicio, @fechaHoraFin, N'UTC_GENERADO_DB', @idCorrelacion);
```

## 5. Vistas

```sql
-- uv_sesion: añadir al final de la lista explícita
            fechaHoraFin = se.fechaHoraFin,
            procedenciaTemporal = se.procedenciaTemporal
-- uv_auth_sesion: añadir s.procedenciaTemporal al final
```

## 6. Guarda opcional contra escrituras directas (decisión del owner)

```sql
CREATE TRIGGER dbo.tr_Sesion_ProcedenciaTemporal ON dbo.Sesion AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF (UPDATE(fechaHoraInicio) OR UPDATE(fechaHoraFin)) AND NOT UPDATE(procedenciaTemporal)
        UPDATE s SET procedenciaTemporal = NULL, procedenciaTemporalCorrelacion = NULL
        FROM dbo.Sesion s JOIN inserted i ON i.id = s.id JOIN deleted d ON d.id = s.id
        WHERE i.fechaHoraInicio <> d.fechaHoraInicio OR i.fechaHoraFin <> d.fechaHoraFin;
END
```

## 7. Clasificación C (plantilla, por entorno)

```sql
-- respaldo previo obligatorio
SELECT id, fechaHoraInicio, fechaHoraFin, procedenciaTemporal, SYSUTCDATETIME() AS respaldadoEn
INTO dbo.Sesion_ClasificacionTemporal_Respaldo_<fecha>
FROM dbo.Sesion WHERE id IN (<ids firmados>);

-- decisión UTC confirmada por el owner funcional: solo marca
UPDATE dbo.Sesion SET procedenciaTemporal = N'UTC_CLASIFICADO_OWNER' WHERE id IN (<ids UTC>);

-- decisión HORA_LOCAL America/Bogota: conversión auditada + marca en la misma sentencia
UPDATE dbo.Sesion
SET fechaHoraInicio = CAST(fechaHoraInicio AT TIME ZONE 'SA Pacific Standard Time' AT TIME ZONE 'UTC' AS DATETIME2),
    fechaHoraFin    = CAST(fechaHoraFin    AT TIME ZONE 'SA Pacific Standard Time' AT TIME ZONE 'UTC' AS DATETIME2),
    procedenciaTemporal = N'UTC_CLASIFICADO_OWNER'
WHERE id IN (<ids HORA_LOCAL>);
```

## 8. Rollback (orden: backend v2 revertido antes)

```sql
SELECT id, procedenciaTemporal, procedenciaTemporalCorrelacion INTO dbo.Sesion_ProcedenciaTemporal_Archivo_<fecha>
FROM dbo.Sesion WHERE procedenciaTemporal IS NOT NULL;
-- restaurar SP y vistas desde la versión previa del repo owner
DROP TRIGGER IF EXISTS dbo.tr_Sesion_ProcedenciaTemporal;
ALTER TABLE dbo.Sesion DROP CONSTRAINT CK_Sesion_ProcedenciaTemporal;
ALTER TABLE dbo.Sesion DROP COLUMN procedenciaTemporalCorrelacion, procedenciaTemporal;
```

Rollback preferido: conservar la columna y solo restaurar SP/vistas (sin pérdida de marcas).
