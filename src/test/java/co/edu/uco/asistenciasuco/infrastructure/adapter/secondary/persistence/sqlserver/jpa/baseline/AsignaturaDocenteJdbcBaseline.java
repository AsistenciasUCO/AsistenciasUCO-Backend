package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsignaturaDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaDocenteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AsignaturaDocenteJdbcBaseline implements AsignaturaDocenteQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public AsignaturaDocenteJdbcBaseline(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<AsignaturaDocenteProjection> consultarAsignaturasDocente(final UUID idDocente) {
        return jdbcOperations.query("""
                SELECT DISTINCT d.idAsignatura, d.nombreAsignatura, d.idGrupo, d.nombreGrupo, d.idPrograma, d.nombrePrograma,
                       g.estudiantesActivos
                FROM dbo.uv_docente d
                LEFT JOIN dbo.uv_grupo g ON g.id = d.idGrupo
                WHERE d.id = :idDocente
                ORDER BY d.nombreAsignatura, d.nombreGrupo
                """, new MapSqlParameterSource("idDocente", idDocente), (rs, rowNum) -> new AsignaturaDocenteProjection(
                JdbcBaselineValueMapper.toUuid(rs.getObject("idAsignatura")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombreAsignatura")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idGrupo")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombreGrupo")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idPrograma")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombrePrograma")),
                (Integer) rs.getObject("estudiantesActivos")
        ));
    }
}


