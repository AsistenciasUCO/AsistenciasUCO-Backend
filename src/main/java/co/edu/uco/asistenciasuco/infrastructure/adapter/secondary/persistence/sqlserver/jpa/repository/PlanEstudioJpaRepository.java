package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PlanEstudioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvPlanEstudioEntity;
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

/** Repositorio JPA directo consolidado para PlanEstudio. */
@Repository
public class PlanEstudioJpaRepository implements PlanEstudioQueryPort, PlanEstudioCommandPort {

    static final String SQL_REGISTRAR_O_ACTUALIZAR_PLAN = """
            EXEC dbo.usp_registrar_o_actualizar_plan_estudio
                 @idPlanEstudio = :idPlanEstudio,
                 @idPrograma = :idPrograma,
                 @inp = :inp,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String HQL_POR_PROGRAMA = """
            select p
            from UvPlanEstudioEntity p
            where p.idPrograma = :idPrograma
            order by p.nombrePrograma, p.inp, p.id
            """;

    static final String OP_REGISTRAR_O_ACTUALIZAR_PLAN = "registrarOActualizarPlanEstudio";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public PlanEstudioJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    // COMMANDS

    @Override
    public void registrarOActualizarPlanEstudio(
            final UUID idPlanEstudio,
            final UUID idPrograma,
            final Integer inp,
            final UUID idUsuarioEjecutor
    ) {
        ejecutarRegistrarOActualizarPlanEstudio(idPlanEstudio, idPrograma, inp, idUsuarioEjecutor);
    }

    // QUERIES

    @Override
    public List<PlanEstudioProjection> consultarPlanesPorPrograma(final UUID idPrograma) {
        return JpaQueryExecutor.execute(
                "consultarPlanesPorPrograma",
                "No fue posible consultar los planes de estudio desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_PROGRAMA, UvPlanEstudioEntity.class)
                        .setParameter("idPrograma", idPrograma)
                        .getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toPlanEstudio)
                        .toList()
        );
    }

    // PRIVATE HELPERS

    private ProcedureResult ejecutarRegistrarOActualizarPlanEstudio(
            final UUID idPlanEstudio,
            final UUID idPrograma,
            final Integer inp,
            final UUID idUsuarioEjecutor
    ) {
        if (ObjectHelper.isNull(idUsuarioEjecutor)) {
            throw new CrosscuttingException("El usuario ejecutor del plan de estudio es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idPlanEstudio", idPlanEstudio);
        parametros.put("idPrograma", idPrograma);
        parametros.put("inp", inp);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", idUsuarioEjecutor);

        return procedureExecutor.execute(
                OP_REGISTRAR_O_ACTUALIZAR_PLAN,
                SQL_REGISTRAR_O_ACTUALIZAR_PLAN,
                parametros,
                correlationId
        );
    }
}
