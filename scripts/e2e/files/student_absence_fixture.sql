-- LB-004D E2E: ausencia de prueba controlada para files.e2e.estudiante1 (Carlos Zapata, EstudianteGrupo B1A2C3D4-...-0001)
-- en la SESION 2 (SES-02) del Grupo 001 - Arquitectura de Software.
--
-- No inventa datos: registra la asistencia con el PROCEDIMIENTO OFICIAL usp_registrar_asistencias_sesion, ejecutado como el
-- docente titular del grupo, para UN solo estudiante (estado SJC = sin justa causa => asistio=0). No toca fechas ni otros estudiantes.
--
-- Uso (dentro del contenedor SQL o con sqlcmd): sqlcmd -C -S localhost -U sa -P ... -d gestionasistenciadb -i student_absence_fixture.sql -v MODE=apply|cleanup|status
-- Idempotente: 'apply' no hace nada si la sesion ya tiene detalle; 'cleanup' elimina solo las filas de ese (EstudianteGrupo, Sesion).
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;

DECLARE @modo          VARCHAR(10)      = '$(MODE)';
DECLARE @estudiante    UNIQUEIDENTIFIER = 'F1A2B3C4-0000-0000-0000-000000000004';
DECLARE @estGrupo      UNIQUEIDENTIFIER = 'B1A2C3D4-0000-0000-0000-000000000001';
DECLARE @sesion        UNIQUEIDENTIFIER = 'C3D4E5F6-A7B8-9C0D-1E2F-3A4B5C6D7E8F';   -- SES-02, 2026-08-24
DECLARE @docenteUsuario UNIQUEIDENTIFIER = 'E1F2A3B4-0000-0000-0000-000000000003';   -- titular del Grupo 001
DECLARE @asistencia    UNIQUEIDENTIFIER = (SELECT TOP 1 id FROM dbo.Asistencia WHERE estudianteGrupo = @estGrupo AND sesion = @sesion);

IF NOT EXISTS (SELECT 1 FROM dbo.EstudianteGrupo WHERE id = @estGrupo AND estudiante = @estudiante)
BEGIN RAISERROR('El EstudianteGrupo del fixture no corresponde al estudiante esperado; abortado.', 16, 1); RETURN; END

IF @modo = 'apply'
BEGIN
    IF @asistencia IS NOT NULL AND EXISTS (SELECT 1 FROM dbo.DetalleAsistencia WHERE asistencia = @asistencia)
        PRINT 'FIXTURE_ALREADY_PRESENT';
    ELSE
    BEGIN
        DECLARE @json NVARCHAR(MAX) = CONCAT('[{"idEstudiante":"', CAST(@estudiante AS VARCHAR(36)), '","estado":"SJC"}]');
        DECLARE @corr UNIQUEIDENTIFIER = NEWID();
        DECLARE @res TABLE (idCorrelacion UNIQUEIDENTIFIER, mensajeUsuario NVARCHAR(4000), mensajeTecnico NVARCHAR(4000), estado BIT);
        INSERT INTO @res EXEC dbo.usp_registrar_asistencias_sesion @idSesion = @sesion, @asistenciaJSON = @json, @idCorrelacion = @corr, @idUsuarioEjecutor = @docenteUsuario;
        SELECT 'SP_RESULT' k, estado, mensajeUsuario FROM @res;
    END
END
ELSE IF @modo = 'cleanup'
BEGIN
    IF @asistencia IS NULL PRINT 'NOTHING_TO_CLEAN';
    ELSE
    BEGIN
        BEGIN TRAN;
        DELETE a FROM dbo.ArchivoSoporte a JOIN dbo.SolicitudRevisionAsistencia s ON s.id = a.solicitudRevisionAsistencia WHERE s.asistencia = @asistencia;
        DELETE FROM dbo.SolicitudRevisionAsistencia WHERE asistencia = @asistencia;
        DELETE FROM dbo.DetalleAsistencia WHERE asistencia = @asistencia;
        DELETE FROM dbo.Asistencia WHERE id = @asistencia;
        COMMIT;
        PRINT 'CLEANUP_DONE';
    END
END

SELECT 'STATUS' k, a.id idAsistencia, d.id idDetalle, d.asistio, rc.codigo razon,
       (SELECT COUNT(*) FROM dbo.SolicitudRevisionAsistencia s WHERE s.asistencia = a.id) solicitudes
FROM dbo.Asistencia a
LEFT JOIN dbo.DetalleAsistencia d ON d.asistencia = a.id
LEFT JOIN dbo.RazonCausa rc ON rc.id = d.razonCausa
WHERE a.estudianteGrupo = @estGrupo AND a.sesion = @sesion;
