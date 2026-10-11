package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.SesionMateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class SesionMateriaEstudianteJdbcBaseline implements SesionMateriaEstudianteQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public SesionMateriaEstudianteJdbcBaseline(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<SesionMateriaEstudianteProjection> consultarSesionesMateria(final UUID idEstudiante, final UUID idAsignatura) {
        return jdbcOperations.query("""
                SELECT s.id, s.nombre, s.numero, s.codigo, s.numeroSemana, s.idGrupo, s.codigoGrupo, s.nombreGrupo,
                       s.fechaHoraInicio, s.fechaHoraFin
                FROM dbo.uv_sesion s
                INNER JOIN dbo.uv_estudiante_grupo eg ON eg.idGrupo = s.idGrupo
                INNER JOIN dbo.uv_grupo g ON g.id = s.idGrupo
                WHERE eg.idEstudiante = :idEstudiante
                  AND g.idAsignatura = :idAsignatura
                ORDER BY s.numero, s.fechaHoraInicio
                """, new MapSqlParameterSource()
                .addValue("idEstudiante", idEstudiante)
                .addValue("idAsignatura", idAsignatura), (rs, rowNum) -> new SesionMateriaEstudianteProjection(
                JdbcBaselineValueMapper.toUuid(rs.getObject("id")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombre")),
                JdbcBaselineValueMapper.toInteger(rs.getObject("numero")),
                JdbcBaselineValueMapper.toString(rs.getObject("codigo")),
                JdbcBaselineValueMapper.toInteger(rs.getObject("numeroSemana")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idGrupo")),
                JdbcBaselineValueMapper.toString(rs.getObject("codigoGrupo")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombreGrupo")),
                rs.getObject("fechaHoraInicio", java.time.LocalDateTime.class),
                rs.getObject("fechaHoraFin", java.time.LocalDateTime.class)
        ));
    }
}


