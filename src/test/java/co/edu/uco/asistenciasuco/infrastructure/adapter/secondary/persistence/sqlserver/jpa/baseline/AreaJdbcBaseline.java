package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AreaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;

public final class AreaJdbcBaseline implements AreaQueryPort {

    private final NamedParameterJdbcOperations jdbcOperations;

    public AreaJdbcBaseline(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
    }

    @Override
    public List<AreaProjection> consultarAreas() {
        return jdbcOperations.query("""
                SELECT id, codigo, nombre, totalAsignaturas
                FROM dbo.uv_area
                ORDER BY nombre, id
                """, (rs, rowNum) -> new AreaProjection(
                JdbcBaselineValueMapper.toUuid(rs.getObject("id")),
                JdbcBaselineValueMapper.toString(rs.getObject("codigo")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombre")),
                rs.getObject("totalAsignaturas") != null ? ((Number) rs.getObject("totalAsignaturas")).longValue() : 0L
        ));
    }
}


