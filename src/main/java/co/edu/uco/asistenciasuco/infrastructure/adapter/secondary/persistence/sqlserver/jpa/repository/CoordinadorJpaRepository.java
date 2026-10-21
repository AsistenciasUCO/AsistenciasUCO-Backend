package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvCoordinadorEntity;
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

/** Repositorio JPA directo consolidado para Coordinador. */
@Repository
public class CoordinadorJpaRepository implements CoordinadorQueryPort, CoordinadorCommandPort {

    static final String SQL_CREAR_COORDINADOR = """
            EXEC dbo.usp_crear_coordinador
                 @idCoordinador = :idCoordinador,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @correo = :correo,
                 @idPrograma = :idPrograma,
                 @idFacultad = :idFacultad,
                 @password = :password,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String HQL_POR_FACULTAD = """
            select c
            from UvCoordinadorEntity c
            where c.idFacultad = :idFacultad
            order by c.nombrePrograma, c.nombreCompleto, c.id
            """;

    static final String HQL_SIN_PROGRAMA = """
            select c.id, c.idUsuario, c.numeroIdentificacion, c.nombreCompleto,
                   c.idPrograma, c.nombrePrograma, c.estaActivoCoordinador
            from UvCoordinadorEntity c
            where c.idPrograma is null
            order by c.nombreCompleto, c.id
            """;

    static final String OP_CREAR_COORDINADOR = "crearCoordinador";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public CoordinadorJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    // COMMANDS

    @Override
    public void crearCoordinador(final UUID idCoordinador, final String numeroIdentificacion,
                                 final String primerNombre, final String segundoNombre,
                                 final String primerApellido, final String segundoApellido, final String correo,
                                 final UUID idPrograma, final UUID idFacultad, final String password,
                                 final UUID usuarioEjecutor) {
        ejecutarCrearCoordinador(idCoordinador, numeroIdentificacion, primerNombre, segundoNombre,
                primerApellido, segundoApellido, correo, idPrograma, idFacultad, password, usuarioEjecutor);
    }

    // QUERIES

    @Override
    public List<CoordinadorProjection> consultarCoordinadoresPorFacultad(final UUID idFacultad) {
        return JpaQueryExecutor.execute(
                "consultarCoordinadoresPorFacultad",
                "No fue posible consultar los coordinadores desde base de datos.",
                () -> entityManager.createQuery(HQL_POR_FACULTAD, UvCoordinadorEntity.class)
                        .setParameter("idFacultad", idFacultad)
                        .getResultList().stream()
                        .map(AcademicViewJpaProjectionMapper::toCoordinador)
                        .toList()
        );
    }

    @Override
    public List<CoordinadorProjection> consultarCoordinadoresSinPrograma() {
        return JpaQueryExecutor.execute(
                "consultarCoordinadoresSinPrograma",
                "No fue posible consultar los coordinadores disponibles sin programa desde base de datos.",
                () -> entityManager.createQuery(HQL_SIN_PROGRAMA, Object[].class)
                        .getResultList().stream()
                        .map(row -> new CoordinadorProjection(
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

    private ProcedureResult ejecutarCrearCoordinador(
            final UUID idCoordinador,
            final String numeroIdentificacion,
            final String primerNombre,
            final String segundoNombre,
            final String primerApellido,
            final String segundoApellido,
            final String correo,
            final UUID idPrograma,
            final UUID idFacultad,
            final String password,
            final UUID usuarioEjecutor
    ) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idCoordinador", idCoordinador);
        parametros.put("numeroIdentificacion", numeroIdentificacion);
        parametros.put("primerNombre", primerNombre);
        parametros.put("segundoNombre", segundoNombre);
        parametros.put("primerApellido", primerApellido);
        parametros.put("segundoApellido", segundoApellido);
        parametros.put("correo", correo);
        parametros.put("idPrograma", idPrograma);
        parametros.put("idFacultad", idFacultad);
        parametros.put("password", password);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", usuarioEjecutor);

        return procedureExecutor.execute(OP_CREAR_COORDINADOR, SQL_CREAR_COORDINADOR, parametros, correlationId);
    }
}
