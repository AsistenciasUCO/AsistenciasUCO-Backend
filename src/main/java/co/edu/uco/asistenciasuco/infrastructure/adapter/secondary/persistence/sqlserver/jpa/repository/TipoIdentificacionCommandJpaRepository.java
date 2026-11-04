package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionCommandPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** HU153: EXEC canonico de usp_guardar_tipo_identificacion (alta sin id; con id solo cambia el nombre). */
@Repository
public class TipoIdentificacionCommandJpaRepository implements TipoIdentificacionCommandPort {

    static final String SQL_GUARDAR = """
            EXEC dbo.usp_guardar_tipo_identificacion
                 @idTipoIdentificacion = :idTipoIdentificacion,
                 @codigo = :codigo,
                 @nombre = :nombre,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String OP_GUARDAR = "guardarTipoIdentificacion";

    private final JpaProcedureExecutor procedureExecutor;

    public TipoIdentificacionCommandJpaRepository(final JpaProcedureExecutor procedureExecutor) {
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor de procedimientos es obligatorio.");
    }

    @Override
    public void guardarTipoIdentificacion(final GuardarTipoIdentificacionCommand command) {
        final UUID correlationId = CorrelationIdContext.require();
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idTipoIdentificacion", command.id());
        parametros.put("codigo", command.codigo());
        parametros.put("nombre", command.nombre());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.actorUsuarioId());
        procedureExecutor.execute(OP_GUARDAR, SQL_GUARDAR, parametros, correlationId);
    }
}
