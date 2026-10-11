package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline;

import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaRow;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ReporteAsistenciaJdbcBaseline implements ReporteAsistenciaQueryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReporteAsistenciaJdbcBaseline.class);
    private static final String PARAM_ID_GRUPO = "idGrupo";

    static final String SQL_REPORTE_ASISTENCIA_GRUPO = """
            SELECT
                s.codigoGrupo,
                s.nombreGrupo,
                s.numero AS numeroSesion,
                s.nombre AS nombreSesion,
                s.fechaHoraInicio,
                s.fechaHoraFin,
                CAST(ei.numeroIdentificacion AS VARCHAR(20)) AS documentoEstudiante,
                ei.nombreCompleto AS nombreEstudiante,
                u.correo AS correoEstudiante,
                da.asistio,
                da.nombreRazonCausa AS razonCausa
            FROM dbo.uv_sesion s
            INNER JOIN dbo.uv_estudiante_grupo eg
                    ON eg.idGrupo = s.idGrupo
            INNER JOIN dbo.uv_estudiante_identidad ei
                    ON ei.id = eg.idEstudiante
            INNER JOIN dbo.uv_usuario u
                    ON u.id = ei.idUsuario
            LEFT JOIN dbo.uv_asistencia a
                    ON a.idSesion = s.id
                   AND a.idEstudianteGrupo = eg.id
            LEFT JOIN dbo.uv_detalle_asistencia da
                    ON da.idAsistencia = a.id
            WHERE s.idGrupo = :idGrupo
            ORDER BY s.numero, s.fechaHoraInicio, ei.nombreCompleto, ei.id, eg.id
            """;

    private final NamedParameterJdbcOperations jdbcOperations;

    public ReporteAsistenciaJdbcBaseline(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<ReporteAsistenciaRow> consultarReporteAsistenciaGrupo(final UUID grupoId) {
        try {
            return jdbcOperations.query(
                    SQL_REPORTE_ASISTENCIA_GRUPO,
                    new MapSqlParameterSource(PARAM_ID_GRUPO, grupoId),
                    (rs, rowNum) -> new ReporteAsistenciaRow(
                            JdbcBaselineValueMapper.toString(rs.getObject("codigoGrupo")),
                            JdbcBaselineValueMapper.toString(rs.getObject("nombreGrupo")),
                            rs.getObject("numeroSesion") == null ? null : rs.getInt("numeroSesion"),
                            JdbcBaselineValueMapper.toString(rs.getObject("nombreSesion")),
                            rs.getObject("fechaHoraInicio", java.time.LocalDateTime.class),
                            rs.getObject("fechaHoraFin", java.time.LocalDateTime.class),
                            JdbcBaselineValueMapper.toString(rs.getObject("documentoEstudiante")),
                            JdbcBaselineValueMapper.toString(rs.getObject("nombreEstudiante")),
                            JdbcBaselineValueMapper.toString(rs.getObject("correoEstudiante")),
                            rs.getObject("asistio") == null ? null : rs.getBoolean("asistio"),
                            JdbcBaselineValueMapper.toString(rs.getObject("razonCausa"))
                    )
            );
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation=consultarReporteAsistenciaGrupo, correlationId={}",
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar el reporte de asistencia.", exception);
        }
    }
}


