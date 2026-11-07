package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * CULM-001 B.2 (CONTRACT 2.16.3 (d), [SUPUESTO-B-20]): soporte de fixtures Java para la columna obligatoria
 * {@code DetalleAsistencia.toma} y para la limpieza en el orden {@code DetalleAsistencia -> TokenAsistenciaToma ->
 * TomaAsistencia -> SesionCancelacion -> Asistencia} antes de borrar la {@code Sesion}. Mismo criterio que el helper T-SQL
 * {@code test/fixtures/culm001_b2_toma_fixture.sql}: la toma 1 implicita nace cerrada, modo sin QR.
 */
public final class TomaAsistenciaTestSql {

    /** Crea la toma 1 implicita de la sesion de la asistencia indicada si aun no existe. Parametro: idAsistencia. */
    public static final String SQL_ASEGURAR_TOMA_1_DE_ASISTENCIA = """
            INSERT INTO dbo.TomaAsistencia (id, sesion, numero, modo, estado, esImplicita, docenteApertura, fechaAperturaUtc,
                                            fechaCierreUtc, idCorrelacionApertura)
            SELECT NEWID(), a.sesion, 1,
                   (SELECT m.id FROM dbo.ModoTomaAsistencia m WHERE m.usaCodigoQr = 0),
                   (SELECT e.id FROM dbo.EstadoTomaAsistencia e WHERE e.estaAbierta = 0),
                   1, g.docente, SYSUTCDATETIME(), SYSUTCDATETIME(), NEWID()
            FROM dbo.Asistencia a
            INNER JOIN dbo.Sesion s ON s.id = a.sesion
            INNER JOIN dbo.Grupo g ON g.id = s.grupo
            WHERE a.id = ? AND NOT EXISTS (SELECT 1 FROM dbo.TomaAsistencia t WHERE t.sesion = a.sesion AND t.numero = 1)
            """;

    /** Subconsulta de la toma 1 de la sesion de la asistencia (parametro: idAsistencia). */
    public static final String SQL_TOMA_1_DE_ASISTENCIA = """
            (SELECT t.id FROM dbo.TomaAsistencia t INNER JOIN dbo.Asistencia a ON a.sesion = t.sesion WHERE a.id = ? AND t.numero = 1)
            """;

    private TomaAsistenciaTestSql() {
    }

    /** Borra los dependientes de la sesion en el orden del contrato (no borra la sesion). */
    public static void limpiarDependientesDeSesion(final JdbcTemplate jdbc, final String sesion) {
        jdbc.update("DELETE FROM dbo.DetalleAsistencia WHERE asistencia IN (SELECT id FROM dbo.Asistencia WHERE sesion = ?)", sesion);
        jdbc.update("DELETE FROM dbo.DetalleAsistencia WHERE toma IN (SELECT id FROM dbo.TomaAsistencia WHERE sesion = ?)", sesion);
        jdbc.update("DELETE FROM dbo.TokenAsistenciaToma WHERE toma IN (SELECT id FROM dbo.TomaAsistencia WHERE sesion = ?)", sesion);
        jdbc.update("DELETE FROM dbo.TomaAsistencia WHERE sesion = ?", sesion);
        jdbc.update("DELETE FROM dbo.SesionCancelacion WHERE sesion = ?", sesion);
        jdbc.update("DELETE FROM dbo.Asistencia WHERE sesion = ?", sesion);
    }

    /** Igual que {@link #limpiarDependientesDeSesion} para todas las sesiones que cumplen el filtro SQL dado. */
    public static void limpiarDependientesDeSesiones(final JdbcTemplate jdbc, final String filtroSesiones, final Object... parametros) {
        final String sesiones = "(SELECT s.id FROM dbo.Sesion s WHERE " + filtroSesiones + ")";
        jdbc.update("DELETE FROM dbo.DetalleAsistencia WHERE asistencia IN (SELECT id FROM dbo.Asistencia WHERE sesion IN " + sesiones + ")", parametros);
        jdbc.update("DELETE FROM dbo.DetalleAsistencia WHERE toma IN (SELECT id FROM dbo.TomaAsistencia WHERE sesion IN " + sesiones + ")", parametros);
        jdbc.update("DELETE FROM dbo.TokenAsistenciaToma WHERE toma IN (SELECT id FROM dbo.TomaAsistencia WHERE sesion IN " + sesiones + ")", parametros);
        jdbc.update("DELETE FROM dbo.TomaAsistencia WHERE sesion IN " + sesiones, parametros);
        jdbc.update("DELETE FROM dbo.SesionCancelacion WHERE sesion IN " + sesiones, parametros);
        jdbc.update("DELETE FROM dbo.Asistencia WHERE sesion IN " + sesiones, parametros);
    }
}
