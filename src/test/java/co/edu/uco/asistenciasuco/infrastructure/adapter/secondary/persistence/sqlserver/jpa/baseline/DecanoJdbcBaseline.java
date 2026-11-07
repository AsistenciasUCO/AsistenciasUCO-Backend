package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DecanoProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.DecanoJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;

/**
 * Adaptador SQL Server de decanos.
 *
 * <p>COMMAND (alta de decano) delega en {@link DecanoJpaRepository} sobre {@code EntityManager}
 * (LB-008 JPA-03). La QUERY sigue en JDBC temporal hasta JPA-05.</p>
 */
public final class DecanoJdbcBaseline implements DecanoQueryPort, DecanoCommandPort {

    private final NamedParameterJdbcOperations jdbcOperations;
    private final DecanoJpaRepository commands;

    public DecanoJdbcBaseline(
            final NamedParameterJdbcOperations jdbcOperations,
            final DecanoJpaRepository commands
    ) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
        this.commands = Objects.requireNonNull(commands, "El command JPA de decano es obligatorio.");
    }

    @Override
    public List<DecanoProjection> consultarDecanos() {
        return jdbcOperations.query("""
                SELECT id, idUsuario, numeroIdentificacion, nombreCompleto, idFacultad, nombreFacultad, estaActivoDecano
                FROM dbo.uv_decano
                ORDER BY nombreCompleto, id
                """, (rs, rowNum) -> new DecanoProjection(
                JdbcBaselineValueMapper.toUuid(rs.getObject("id")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idUsuario")),
                JdbcBaselineValueMapper.toString(rs.getObject("numeroIdentificacion")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombreCompleto")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idFacultad")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombreFacultad")),
                JdbcBaselineValueMapper.toBoolean(rs.getObject("estaActivoDecano"))
        ));
    }

    @Override
    public void crearDecano(final CrearDecanoCommand command) {
        // usp_crear_decano ya no declara @idTipoIdIdentificacion (resuelve el tipo internamente);
        // command.tipoIdentificacionId() se conserva en capas superiores por compatibilidad HTTP
        // pero no se reenvia a la SP.
        commands.crearDecano(command);
    }

    @Override
    public ResultadoCambioEstadoDecano cambiarEstadoDecano(final java.util.UUID idDecano, final Boolean estaActivo,
            final java.util.UUID usuarioEjecutor) {
        throw new UnsupportedOperationException("CULM-001 A.2-8: el cambio de estado del decano no forma parte de la linea base JDBC.");
    }
}
