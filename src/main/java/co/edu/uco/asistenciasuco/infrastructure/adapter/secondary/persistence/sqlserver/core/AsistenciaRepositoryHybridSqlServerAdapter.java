package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaAutonomaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;

import java.util.List;
import java.util.Objects;

/**
 * Adapter hibrido (LB-002.1 / LB-002.2): la query de asistencia se resuelve con
 * {@link AsistenciaQueryPersistence}, el command {@code registrarAsistenciasSesion} con
 * {@link AsistenciaCommandPersistence} y el RESTO de commands delega SIEMPRE al adapter JDBC baseline
 * (no migran). Un solo provider por invocacion: no hay dual-write, lectura dual ni shadow traffic. La
 * autorizacion sigue viviendo en Application.
 */
public final class AsistenciaRepositoryHybridSqlServerAdapter implements AsistenciaRepositoryPort {

    private final AsistenciaRepositoryPort commands;
    private final AsistenciaQueryPersistence query;
    private final AsistenciaCommandPersistence registrarAsistenciasSesionCommand;

    /** {@code registrarAsistenciasSesion} sigue en el baseline JDBC ({@code commands}). */
    public AsistenciaRepositoryHybridSqlServerAdapter(
            final AsistenciaRepositoryPort commands,
            final AsistenciaQueryPersistence query
    ) {
        this(commands, query, baselineBatchCommand(commands));
    }

    public AsistenciaRepositoryHybridSqlServerAdapter(
            final AsistenciaRepositoryPort commands,
            final AsistenciaQueryPersistence query,
            final AsistenciaCommandPersistence registrarAsistenciasSesionCommand
    ) {
        this.commands = Objects.requireNonNull(commands, "El adapter JDBC de commands de asistencia es obligatorio.");
        this.query = Objects.requireNonNull(query, "La persistencia de la query de asistencia es obligatoria.");
        this.registrarAsistenciasSesionCommand = Objects.requireNonNull(
                registrarAsistenciasSesionCommand,
                "La persistencia del command de registro en lote de asistencia es obligatoria."
        );
    }

    private static AsistenciaCommandPersistence baselineBatchCommand(final AsistenciaRepositoryPort commands) {
        return Objects.requireNonNull(commands, "El adapter JDBC de commands de asistencia es obligatorio.")
                ::registrarAsistenciasSesion;
    }

    @Override
    public void registrarAsistencia(final RegistrarAsistenciaRepositoryDTO dto) {
        commands.registrarAsistencia(dto);
    }

    @Override
    public void registrarAsistenciasSesion(final RegistrarAsistenciasSesionRepositoryDTO dto) {
        registrarAsistenciasSesionCommand.registrarAsistenciasSesion(dto);
    }

    @Override
    public void registrarAsistenciaAutonoma(final RegistrarAsistenciaAutonomaRepositoryDTO dto) {
        commands.registrarAsistenciaAutonoma(dto);
    }

    @Override
    public List<AsistenciaRepositoryProjection> consultarAsistenciasPorGrupo(
            final ConsultarAsistenciasPorGrupoRepositoryDTO dto
    ) {
        return query.consultarAsistenciasPorGrupo(dto);
    }

    @Override
    public void solicitarRevisionAsistencia(final SolicitarRevisionAsistenciaRepositoryDTO dto) {
        commands.solicitarRevisionAsistencia(dto);
    }

    @Override
    public void resolverSolicitudRevisionAsistencia(final ResolverSolicitudRevisionAsistenciaRepositoryDTO dto) {
        commands.resolverSolicitudRevisionAsistencia(dto);
    }
}
