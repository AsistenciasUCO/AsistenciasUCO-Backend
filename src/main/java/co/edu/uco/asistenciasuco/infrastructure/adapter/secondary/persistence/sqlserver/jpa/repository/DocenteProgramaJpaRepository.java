package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteProgramaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvDocenteEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query.JpaQueryExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Repository
public class DocenteProgramaJpaRepository implements DocenteProgramaQueryPort, DocenteProgramaCommandPort {

    static final String SQL_CREAR_DOCENTE = """
            EXEC dbo.usp_crear_docente
                 @idDocente = :idDocente,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @correo = :correo,
                 @idPrograma = :idPrograma,
                 @password = :password,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String HQL_POR_PROGRAMA = """
            select d
            from UvDocenteEntity d
            where d.idPrograma = :idPrograma
            order by d.nombreCompleto, d.id, d.idGrupo
            """;

    static final String HQL_DISPONIBLES = """
            select di.id, di.idUsuario, di.numeroIdentificacion, di.nombreCompleto,
                   u.correo, di.estaActivoUsuario
            from UvDocenteIdentidadEntity di, UvUsuarioEntity u
            where u.id = di.idUsuario
            order by di.nombreCompleto, di.id
            """;

    static final String OP_CREAR_DOCENTE = "crearDocente";

    private final EntityManager entityManager;
    private final JpaProcedureExecutor procedureExecutor;

    public DocenteProgramaJpaRepository(final EntityManager entityManager, final JpaProcedureExecutor procedureExecutor) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager es obligatorio.");
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El JpaProcedureExecutor es obligatorio.");
    }

    @Override
    public void crearDocente(final UUID idDocente, final String numeroIdentificacion,
                             final String primerNombre, final String segundoNombre,
                             final String primerApellido, final String segundoApellido,
                             final String correo, final UUID idPrograma,
                             final String password, final UUID usuarioEjecutor) {
        ejecutarCrearDocente(idDocente, numeroIdentificacion, primerNombre, segundoNombre,
                primerApellido, segundoApellido, correo, idPrograma, password, usuarioEjecutor);
    }

    @Override
    public List<DocenteProgramaProjection> consultarDocentesPorPrograma(final UUID idPrograma) {
        return JpaQueryExecutor.execute(
                "consultarDocentesPorPrograma",
                "No fue posible consultar los docentes del programa desde base de datos.",
                () -> {
                    final List<UvDocenteEntity> filas = entityManager.createQuery(HQL_POR_PROGRAMA, UvDocenteEntity.class)
                            .setParameter("idPrograma", idPrograma)
                            .getResultList();
                    final Map<UUID, DocenteProgramaProjection> agrupados = new LinkedHashMap<>();
                    for (final UvDocenteEntity row : filas) {
                        if (row == null || row.id() == null) {
                            continue;
                        }
                        agrupados.compute(row.id(), (id, existente) -> {
                            final int grupos = (existente == null ? 0 : existente.totalGruposAsignados())
                                    + (row.idGrupo() != null ? 1 : 0);
                            return new DocenteProgramaProjection(
                                    row.id(),
                                    row.idUsuario(),
                                    row.numeroIdentificacion() == null ? null : String.valueOf(row.numeroIdentificacion()),
                                    row.nombreCompleto(),
                                    null,
                                    row.idPrograma(),
                                    row.nombrePrograma(),
                                    grupos,
                                    row.estaActivoDocente() != null && row.estaActivoDocente() == 1
                            );
                        });
                    }
                    return List.copyOf(agrupados.values());
                }
        );
    }

    @Override
    public List<DocenteProgramaProjection> consultarDocentesDisponibles() {
        return JpaQueryExecutor.execute(
                "consultarDocentesDisponibles",
                "No fue posible consultar los docentes disponibles desde base de datos.",
                () -> entityManager.createQuery(HQL_DISPONIBLES, Object[].class)
                        .getResultList().stream()
                        .map(row -> new DocenteProgramaProjection(
                                (UUID) row[0],
                                (UUID) row[1],
                                row[2] == null ? null : String.valueOf(row[2]),
                                (String) row[3],
                                (String) row[4],
                                null,
                                null,
                                0,
                                row[5] instanceof Boolean b ? b : (row[5] instanceof Number n && n.intValue() == 1)
                        ))
                        .toList()
        );
    }

    private ProcedureResult ejecutarCrearDocente(
            final UUID idDocente,
            final String numeroIdentificacion,
            final String primerNombre,
            final String segundoNombre,
            final String primerApellido,
            final String segundoApellido,
            final String correo,
            final UUID idPrograma,
            final String password,
            final UUID usuarioEjecutor
    ) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idDocente", idDocente);
        parametros.put("numeroIdentificacion", numeroIdentificacion);
        parametros.put("primerNombre", primerNombre);
        parametros.put("segundoNombre", segundoNombre);
        parametros.put("primerApellido", primerApellido);
        parametros.put("segundoApellido", segundoApellido);
        parametros.put("correo", correo);
        parametros.put("idPrograma", idPrograma);
        parametros.put("password", password);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", usuarioEjecutor);

        return procedureExecutor.execute(OP_CREAR_DOCENTE, SQL_CREAR_DOCENTE, parametros, correlationId);
    }
}
