package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvProgramaEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
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

/** Consulta JPA-only de {@code uv_programa} y comando sobre {@code usp_crear_programa_academico} (HU076). */
@Repository
public class ProgramaJpaRepository implements ProgramaQueryPort, ProgramaCommandPort {

    static final String SQL_CREAR_PROGRAMA = """
            EXEC dbo.usp_crear_programa_academico
                 @idPrograma = :idPrograma,
                 @codigo = :codigo,
                 @nombre = :nombre,
                 @idCoordinador = :idCoordinador,
                 @idFacultad = :idFacultad,
                 @idTipoPrograma = :idTipoPrograma,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String OP_CREAR_PROGRAMA = "crearProgramaAcademico";

    static final String HQL_SELECT = """
            select p
            from UvProgramaEntity p
            """;
    static final String HQL_POR_FACULTAD = HQL_SELECT + "where p.idFacultad = :idFacultad order by p.nombrePrograma, p.id";
    static final String HQL_POR_ID = HQL_SELECT + "where p.id = :idPrograma";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public ProgramaJpaRepository(final EntityManager entityManager) {
        this(entityManager, null);
    }

    @Autowired
    public ProgramaJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de programas es obligatorio.");
        this.procedureExecutor = procedureExecutor;
    }

    @Override
    public void crearPrograma(final CrearProgramaCommand command) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idPrograma", command.idPrograma());
        parametros.put("codigo", command.codigo());
        parametros.put("nombre", command.nombre());
        parametros.put("idCoordinador", command.idCoordinador());
        parametros.put("idFacultad", command.idFacultad());
        parametros.put("idTipoPrograma", command.idTipoPrograma());
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", command.actorUsuarioId());

        procedureExecutor.execute(OP_CREAR_PROGRAMA, SQL_CREAR_PROGRAMA, parametros, correlationId);
    }

    @Override
    public List<ProgramaProjection> consultarProgramasPorFacultad(final UUID idFacultad) {
        return JpaQueryExecutor.execute(
                "consultarProgramasPorFacultad",
                "No fue posible consultar los programas academicos desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_FACULTAD, UvProgramaEntity.class)
                        .setParameter("idFacultad", idFacultad)
                        .getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toPrograma)
                        .toList()
        );
    }

    @Override
    public Optional<ProgramaProjection> consultarProgramaPorId(final UUID idPrograma) {
        return JpaQueryExecutor.execute(
                "consultarProgramaPorId",
                "No fue posible consultar el programa academico desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_ID, UvProgramaEntity.class)
                        .setParameter("idPrograma", idPrograma)
                        .setMaxResults(1)
                        .getResultList().stream()
                        .findFirst()
                        .map(AcademicViewJpaProjectionMapper::toPrograma)
        );
    }
}
