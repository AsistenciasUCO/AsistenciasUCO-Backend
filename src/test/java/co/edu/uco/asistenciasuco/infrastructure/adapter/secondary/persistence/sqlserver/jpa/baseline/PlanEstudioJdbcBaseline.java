package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.*;


import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.PlanEstudioJpaRepository;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcBaselineValueMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador SQL Server del plan de estudio.
 *
 * <p>COMMAND delega en {@link PlanEstudioJpaRepository} (LB-008 JPA-03; el provider público
 * {@code usp_registrar_o_actualizar_plan_estudio} lo entrega el work item DB CC-003G-01, {@code CODE_MIGRATION_STATUS = JPA}). La QUERY es oráculo JDBC de paridad (solo {@code src/test}).</p>
 */
public final class PlanEstudioJdbcBaseline implements PlanEstudioQueryPort, PlanEstudioCommandPort {

    private final NamedParameterJdbcOperations jdbcOperations;
    private final PlanEstudioJpaRepository commands;

    public PlanEstudioJdbcBaseline(
            final NamedParameterJdbcOperations jdbcOperations,
            final PlanEstudioJpaRepository commands
    ) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
        this.commands = Objects.requireNonNull(commands, "El command JPA de plan de estudio es obligatorio.");
    }

    @Override
    public void registrarOActualizarPlanEstudio(
            final UUID idPlanEstudio,
            final UUID idPrograma,
            final Integer inp,
            final UUID idUsuarioEjecutor
    ) {
        commands.registrarOActualizarPlanEstudio(idPlanEstudio, idPrograma, inp, idUsuarioEjecutor);
    }

    @Override
    public List<PlanEstudioProjection> consultarPlanesPorPrograma(final UUID idPrograma) {
        return jdbcOperations.query("""
                SELECT id, idPrograma, nombrePrograma, inp, estaActivoPlanEstudio,
                       estaActivoTextoPlanEstudio, justificacionEstado
                FROM dbo.uv_plan_estudio
                WHERE idPrograma = :idPrograma
                ORDER BY nombrePrograma, inp, id
                """, new MapSqlParameterSource("idPrograma", idPrograma), (rs, rowNum) -> new PlanEstudioProjection(
                JdbcBaselineValueMapper.toUuid(rs.getObject("id")),
                JdbcBaselineValueMapper.toUuid(rs.getObject("idPrograma")),
                JdbcBaselineValueMapper.toString(rs.getObject("nombrePrograma")),
                JdbcBaselineValueMapper.toString(rs.getObject("inp")),
                JdbcBaselineValueMapper.toBoolean(rs.getObject("estaActivoPlanEstudio")),
                JdbcBaselineValueMapper.toString(rs.getObject("estaActivoTextoPlanEstudio")),
                JdbcBaselineValueMapper.toString(rs.getObject("justificacionEstado"))
        ));
    }
}



