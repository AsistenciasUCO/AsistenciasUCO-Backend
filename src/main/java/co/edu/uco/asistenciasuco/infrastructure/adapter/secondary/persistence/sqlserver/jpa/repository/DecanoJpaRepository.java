package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort.CrearDecanoCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DecanoProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Repositorio JPA directo consolidado para Decano. */
@Repository
public class DecanoJpaRepository implements DecanoQueryPort, DecanoCommandPort {

    static final String SQL_CREAR_DECANO = """
            EXEC dbo.usp_crear_decano
                 @idDecano = :idDecano,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @correo = :correo,
                 @idFacultad = :idFacultad,
                 @nombreFacultad = :nombreFacultad,
                 @password = :password,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String HQL_LISTAR = """
            select d.id, d.idUsuario, d.numeroIdentificacion, d.nombreCompleto,
                   d.idFacultad, d.nombreFacultad, d.estaActivoDecano
            from UvDecanoEntity d
            order by d.nombreCompleto, d.id
            """;

    static final String OP_CREAR_DECANO = "crearDecano";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public DecanoJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    // COMMANDS

    @Override
    public void crearDecano(final CrearDecanoCommand command) {
        ejecutarCrearDecano(command);
    }

    // QUERIES

    @Override
    public List<DecanoProjection> consultarDecanos() {
        return JpaQueryExecutor.execute(
                "consultarDecanos",
                "No fue posible consultar los decanos desde base de datos.",
                () -> entityManager.createQuery(HQL_LISTAR, Object[].class).getResultList().stream()
                        .map(row -> new DecanoProjection(
                                (UUID) row[0],
                                (UUID) row[1],
                                row[2] == null ? null : String.valueOf(row[2]),
                                (String) row[3],
                                (UUID) row[4],
                                (String) row[5],
                                row[6] != null && ((Integer) row[6]) == 1
                        ))
                        .toList()
        );
    }

    // PRIVATE HELPERS

    private ProcedureResult ejecutarCrearDecano(final CrearDecanoCommand command) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idDecano", command.idDecano());
        parametros.put("numeroIdentificacion", command.numeroIdentificacion());
        parametros.put("primerNombre", command.primerNombre());
        parametros.put("segundoNombre", command.segundoNombre());
        parametros.put("primerApellido", command.primerApellido());
        parametros.put("segundoApellido", command.segundoApellido());
        parametros.put("correo", command.correo());
        parametros.put("idFacultad", command.idFacultad());
        parametros.put("nombreFacultad", command.nombreFacultad());
        parametros.put("password", command.password());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.usuarioEjecutor());

        return procedureExecutor.execute(OP_CREAR_DECANO, SQL_CREAR_DECANO, parametros, correlationId);
    }
}
