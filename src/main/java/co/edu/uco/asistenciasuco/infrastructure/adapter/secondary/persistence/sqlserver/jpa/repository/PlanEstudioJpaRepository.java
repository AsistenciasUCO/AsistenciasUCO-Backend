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
import java.util.Collection;
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

    static final String SQL_CAMBIAR_ESTADO_PLAN = """
            EXEC dbo.usp_cambiar_estado_plan_estudio
                 @idPlanEstudio = :idPlanEstudio,
                 @estaActivo = :estaActivo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_AGREGAR_SEMESTRE_PLAN = """
            EXEC dbo.usp_agregar_semestre_plan_estudio
                 @idPlanEstudio = :idPlanEstudio,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_QUITAR_SEMESTRE_PLAN = """
            EXEC dbo.usp_quitar_semestre_plan_estudio
                 @idPlanEstudio = :idPlanEstudio,
                 @numeroSemestre = :numeroSemestre,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String HQL_POR_PROGRAMA = """
            select p
            from UvPlanEstudioEntity p
            where p.idPrograma = :idPrograma
            order by p.nombrePrograma, p.inp, p.id
            """;

    static final String HQL_POR_FACULTAD = """
            select p
            from UvPlanEstudioEntity p
            where p.idPrograma in (select pr.id from UvProgramaEntity pr where pr.idFacultad = :idFacultad)
            order by p.nombrePrograma, p.inp, p.id
            """;

    /** HU082/HU083: mayor numero de semestre de cada plan (la malla va del semestre 1 a ese numero). */
    static final String HQL_TOTAL_SEMESTRES = """
            select sp.idPlanEstudio, max(sp.numeroSemestre)
            from UvSemestrePlanEstudioEntity sp
            where sp.idPlanEstudio in :idsPlan
            group by sp.idPlanEstudio
            """;

    /**
     * Certificacion F3: asignaturas de la malla de cada plan y suma de sus creditos (misma regla que
     * {@code usp_consultar_programas_creditos_facultad_paginado}). Los planes sin asignaturas no aparecen y quedan en 0.
     */
    static final String HQL_TOTALES_MALLA = """
            select sp.idPlanEstudio, count(a.id), sum(a.credito)
            from UvAsignaturaEntity a, UvSemestrePlanEstudioEntity sp
            where sp.id = a.idSemestrePlanEstudio
              and sp.idPlanEstudio in :idsPlan
            group by sp.idPlanEstudio
            """;

    static final String OP_REGISTRAR_O_ACTUALIZAR_PLAN = "registrarOActualizarPlanEstudio";
    static final String OP_CAMBIAR_ESTADO_PLAN = "cambiarEstadoPlanEstudio";
    static final String OP_AGREGAR_SEMESTRE_PLAN = "agregarSemestrePlanEstudio";
    static final String OP_QUITAR_SEMESTRE_PLAN = "quitarSemestrePlanEstudio";

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

    @Override
    public void cambiarEstadoPlanEstudio(final UUID idPlanEstudio, final boolean estaActivo, final UUID idUsuarioEjecutor) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idPlanEstudio", idPlanEstudio);
        parametros.put("estaActivo", estaActivo);
        ejecutarComandoPlan(OP_CAMBIAR_ESTADO_PLAN, SQL_CAMBIAR_ESTADO_PLAN, parametros, idUsuarioEjecutor);
    }

    @Override
    public void agregarSemestrePlanEstudio(final UUID idPlanEstudio, final UUID idUsuarioEjecutor) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idPlanEstudio", idPlanEstudio);
        ejecutarComandoPlan(OP_AGREGAR_SEMESTRE_PLAN, SQL_AGREGAR_SEMESTRE_PLAN, parametros, idUsuarioEjecutor);
    }

    @Override
    public void quitarSemestrePlanEstudio(final UUID idPlanEstudio, final Integer numeroSemestre, final UUID idUsuarioEjecutor) {
        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idPlanEstudio", idPlanEstudio);
        parametros.put("numeroSemestre", numeroSemestre);
        ejecutarComandoPlan(OP_QUITAR_SEMESTRE_PLAN, SQL_QUITAR_SEMESTRE_PLAN, parametros, idUsuarioEjecutor);
    }

    // QUERIES

    @Override
    public List<PlanEstudioProjection> consultarPlanesPorPrograma(final UUID idPrograma) {
        return JpaQueryExecutor.execute(
                "consultarPlanesPorPrograma",
                "No fue posible consultar los planes de estudio desde base de datos.",
                () -> conTotalSemestres(entityManager.createQuery(HQL_POR_PROGRAMA, UvPlanEstudioEntity.class)
                        .setParameter("idPrograma", idPrograma)
                        .getResultList())
        );
    }

    @Override
    public List<PlanEstudioProjection> consultarPlanesPorFacultad(final UUID idFacultad) {
        return JpaQueryExecutor.execute(
                "consultarPlanesPorFacultad",
                "No fue posible consultar los planes de estudio de la facultad desde base de datos.",
                () -> conTotalSemestres(entityManager.createQuery(HQL_POR_FACULTAD, UvPlanEstudioEntity.class)
                        .setParameter("idFacultad", idFacultad)
                        .getResultList())
        );
    }

    // PRIVATE HELPERS

    private List<PlanEstudioProjection> conTotalSemestres(final List<UvPlanEstudioEntity> planes) {
        final List<UUID> ids = planes.stream().map(UvPlanEstudioEntity::id).toList();
        final Map<UUID, Integer> totales = consultarTotalSemestres(ids);
        final Map<UUID, int[]> malla = consultarTotalesMalla(ids);
        return planes.stream()
                .map(plan -> {
                    final int[] totalMalla = malla.getOrDefault(plan.id(), new int[] {0, 0});
                    return AcademicViewJpaProjectionMapper.toPlanEstudio(plan, totales.getOrDefault(plan.id(), 0),
                            totalMalla[0], totalMalla[1]);
                })
                .toList();
    }

    private Map<UUID, int[]> consultarTotalesMalla(final Collection<UUID> idsPlan) {
        final Map<UUID, int[]> totales = new LinkedHashMap<>();
        if (idsPlan.isEmpty()) {
            return totales;
        }
        entityManager.createQuery(HQL_TOTALES_MALLA, Object[].class)
                .setParameter("idsPlan", idsPlan)
                .getResultList()
                .forEach(fila -> totales.put((UUID) fila[0], new int[] {entero(fila[1]), entero(fila[2])}));
        return totales;
    }

    private static int entero(final Object valor) {
        return valor == null ? 0 : ((Number) valor).intValue();
    }

    private Map<UUID, Integer> consultarTotalSemestres(final Collection<UUID> idsPlan) {
        final Map<UUID, Integer> totales = new LinkedHashMap<>();
        if (idsPlan.isEmpty()) {
            return totales;
        }
        entityManager.createQuery(HQL_TOTAL_SEMESTRES, Object[].class)
                .setParameter("idsPlan", idsPlan)
                .getResultList()
                .forEach(fila -> totales.put((UUID) fila[0], fila[1] == null ? 0 : ((Number) fila[1]).intValue()));
        return totales;
    }

    private void ejecutarComandoPlan(
            final String operacion,
            final String sql,
            final Map<String, Object> parametros,
            final UUID idUsuarioEjecutor
    ) {
        if (ObjectHelper.isNull(idUsuarioEjecutor)) {
            throw new CrosscuttingException("El usuario ejecutor del plan de estudio es obligatorio.");
        }
        final UUID correlationId = CorrelationIdContext.require();
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", idUsuarioEjecutor);
        procedureExecutor.execute(operacion, sql, parametros, correlationId);
    }

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
