package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AreaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvAreaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Consulta JPA de {@code uv_area} y comandos vía Stored Procedures (HU150, HU151, HU152). */
@Repository
public class AreaJpaRepository implements AreaQueryPort, AreaCommandPort {

    static final String OP_CREAR_AREA = "crearArea";
    static final String OP_ACTUALIZAR_AREA = "actualizarArea";

    static final String SQL_CREAR_AREA = """
            EXEC dbo.usp_crear_area
                 @id = :id,
                 @codigo = :codigo,
                 @nombre = :nombre,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_ACTUALIZAR_AREA = """
            EXEC dbo.usp_actualizar_area
                 @id = :id,
                 @codigo = :codigo,
                 @nombre = :nombre,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String HQL_LISTAR = """
            select a
            from UvAreaEntity a
            order by a.nombre, a.id
            """;

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public AreaJpaRepository(final EntityManager entityManager) {
        this(entityManager, null);
    }

    public AreaJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de areas es obligatorio.");
        this.procedureExecutor = procedureExecutor != null ? procedureExecutor : new JpaProcedureExecutor(entityManager);
    }

    // COMMANDS
    @Override
    public void crearArea(final CrearAreaCommand command) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("id", command.id());
        parametros.put("codigo", command.codigo());
        parametros.put("nombre", command.nombre());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.actorUsuarioId());

        procedureExecutor.execute(OP_CREAR_AREA, SQL_CREAR_AREA, parametros, correlationId);
    }

    @Override
    public void actualizarArea(final ActualizarAreaCommand command) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("id", command.id());
        parametros.put("codigo", command.codigo());
        parametros.put("nombre", command.nombre());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.actorUsuarioId());

        procedureExecutor.execute(OP_ACTUALIZAR_AREA, SQL_ACTUALIZAR_AREA, parametros, correlationId);
    }

    // QUERIES
    @Override
    public List<AreaProjection> consultarAreas() {
        return JpaQueryExecutor.execute(
                "consultarAreas",
                "No fue posible consultar las areas desde base de datos.",
                () -> entityManager.createQuery(HQL_LISTAR, UvAreaEntity.class).getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toArea)
                        .toList()
        );
    }
}
