package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadCommandPort.CrearFacultadCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FacultadProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvFacultadEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Consulta JPA-only de {@code uv_facultad} y comando sobre {@code usp_crear_facultad} (LB-008 JPA-05 / HU116). */
@Repository
public class FacultadJpaRepository implements FacultadQueryPort, FacultadCommandPort {

    static final String SQL_CREAR_FACULTAD = """
            EXEC dbo.usp_crear_facultad
                 @id = :id,
                 @nombre = :nombre,
                 @idInstitucion = :idInstitucion,
                 @idDecano = :idDecano,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String OP_CREAR_FACULTAD = "crearFacultad";

    static final String HQL_SELECT = """
            select f
            from UvFacultadEntity f
            """;
    static final String HQL_LISTAR = HQL_SELECT + "order by f.nombreInstitucion, f.nombreFacultad, f.id";
    static final String HQL_POR_ID = HQL_SELECT + "where f.id = :idFacultad";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public FacultadJpaRepository(final EntityManager entityManager) {
        this(entityManager, null);
    }

    @Autowired
    public FacultadJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de facultades es obligatorio.");
        this.procedureExecutor = procedureExecutor;
    }

    // COMMANDS

    @Override
    public void crearFacultad(final CrearFacultadCommand command) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("id", command.id());
        parametros.put("nombre", command.nombre());
        parametros.put("idInstitucion", command.idInstitucion());
        parametros.put("idDecano", command.idDecano());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.actorUsuarioId());

        procedureExecutor.execute(OP_CREAR_FACULTAD, SQL_CREAR_FACULTAD, parametros, correlationId);
    }

    // QUERIES

    @Override
    public List<FacultadProjection> consultarFacultades() {
        return JpaQueryExecutor.execute(
                "consultarFacultades",
                "No fue posible consultar las facultades desde base de datos.",
                () -> entityManager.createQuery(HQL_LISTAR, UvFacultadEntity.class).getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toFacultad)
                        .toList()
        );
    }

    @Override
    public Optional<FacultadProjection> consultarFacultadPorId(final UUID idFacultad) {
        return JpaQueryExecutor.execute(
                "consultarFacultadPorId",
                "No fue posible consultar la facultad desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_ID, UvFacultadEntity.class)
                        .setParameter("idFacultad", idFacultad)
                        .setMaxResults(1)
                        .getResultList().stream()
                        .findFirst()
                        .map(AcademicViewJpaProjectionMapper::toFacultad)
        );
    }
}
