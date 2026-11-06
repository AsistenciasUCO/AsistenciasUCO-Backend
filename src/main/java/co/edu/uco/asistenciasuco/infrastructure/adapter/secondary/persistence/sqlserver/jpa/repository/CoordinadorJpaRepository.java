package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenCoordinacionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoGestionCoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.mapper.AcademicViewJpaProjectionMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity.UvCoordinadorEntity;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.JpaProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.ProcedureRow;
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

    static final String SQL_ACTUALIZAR_COORDINADOR = """
            EXEC dbo.usp_actualizar_coordinador
                 @idCoordinador = :idCoordinador,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @correo = :correo,
                 @idFacultad = :idFacultad,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idTipoIdIdentificacion = :idTipoIdIdentificacion,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @motivo = :motivo
            """;

    static final String SQL_CAMBIAR_ESTADO_COORDINADOR = """
            EXEC dbo.usp_cambiar_estado_coordinador
                 @idCoordinador = :idCoordinador,
                 @estaActivo = :estaActivo,
                 @idFacultad = :idFacultad,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String HQL_POR_FACULTAD = """
            select c
            from UvCoordinadorEntity c
            where c.idFacultad = :idFacultad and c.idPrograma is not null
            order by c.nombrePrograma, c.nombreCompleto, c.id
            """;

    static final String HQL_SIN_PROGRAMA = """
            select c.id, c.idUsuario, c.numeroIdentificacion, c.nombreCompleto,
                   c.idPrograma, c.nombrePrograma, c.estaActivoCoordinador,
                   c.primerNombre, c.segundoNombre, c.primerApellido, c.segundoApellido, c.correo,
                   c.estaActivoRolCoordinador
            from UvCoordinadorEntity c
            where c.idPrograma is null and c.idFacultad = :idFacultad
            order by c.nombreCompleto, c.id
            """;

    /**
     * Certificacion F3: por programa de la facultad, docentes con vinculacion activa y grupos habilitados. Solo lee
     * vistas {@code uv_*} (concedidas al rol runtime); no cambia objetos de BD.
     */
    static final String SQL_RESUMEN_PROGRAMAS_FACULTAD = """
            SELECT pr.id,
                   pr.nombreFacultad,
                   (SELECT COUNT(DISTINCT dp.idDocente) FROM dbo.uv_docente_programa dp
                     WHERE dp.idPrograma = pr.id AND dp.estaActivoDocentePrograma = 1) AS totalDocentes,
                   (SELECT COUNT(*) FROM dbo.uv_grupo_programa gp
                     WHERE gp.idPrograma = pr.id AND gp.grupoEstaHablitado = 1) AS totalGrupos
            FROM dbo.uv_programa pr
            WHERE pr.idFacultad = :idFacultad
            ORDER BY pr.nombrePrograma, pr.id
            """;

    static final String OP_CREAR_COORDINADOR = "crearCoordinador";
    static final String OP_ACTUALIZAR_COORDINADOR = "actualizarCoordinador";
    static final String OP_CAMBIAR_ESTADO_COORDINADOR = "cambiarEstadoCoordinador";

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

    @Override
    public ResultadoGestionCoordinadorProjection actualizarCoordinador(
            final UUID idCoordinador, final String primerNombre, final String segundoNombre,
            final String primerApellido, final String segundoApellido, final String correo,
            final UUID idFacultad, final UUID usuarioEjecutor, final CorreccionDocumentoDTO documento) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idCoordinador", idCoordinador);
        parametros.put("primerNombre", primerNombre);
        parametros.put("segundoNombre", segundoNombre);
        parametros.put("primerApellido", primerApellido);
        parametros.put("segundoApellido", segundoApellido);
        parametros.put("correo", correo);
        parametros.put("idFacultad", idFacultad);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", usuarioEjecutor);
        CorreccionDocumentoParametros.agregar(parametros, documento);

        final ProcedureRow fila = procedureExecutor.executeWithExtras(
                OP_ACTUALIZAR_COORDINADOR, SQL_ACTUALIZAR_COORDINADOR, parametros, correlationId, 1);
        return new ResultadoGestionCoordinadorProjection(idUsuarioDeFila(fila, OP_ACTUALIZAR_COORDINADOR), false);
    }

    @Override
    public ResultadoGestionCoordinadorProjection cambiarEstadoCoordinador(
            final UUID idCoordinador, final boolean estaActivo, final UUID idFacultad, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.require();

        final Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("idCoordinador", idCoordinador);
        parametros.put("estaActivo", estaActivo);
        parametros.put("idFacultad", idFacultad);
        parametros.put("idCorrelacion", correlationId);
        parametros.put("idUsuarioEjecutor", usuarioEjecutor);

        final ProcedureRow fila = procedureExecutor.executeWithExtras(
                OP_CAMBIAR_ESTADO_COORDINADOR, SQL_CAMBIAR_ESTADO_COORDINADOR, parametros, correlationId, 2);
        return new ResultadoGestionCoordinadorProjection(
                idUsuarioDeFila(fila, OP_CAMBIAR_ESTADO_COORDINADOR), flag(fila.extras().get(1)));
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
    public List<CoordinadorProjection> consultarCoordinadoresSinPrograma(final UUID idFacultad) {
        return JpaQueryExecutor.execute(
                "consultarCoordinadoresSinPrograma",
                "No fue posible consultar los coordinadores disponibles sin programa desde base de datos.",
                () -> entityManager.createQuery(HQL_SIN_PROGRAMA, Object[].class)
                        .setParameter("idFacultad", idFacultad)
                        .getResultList().stream()
                        .map(row -> new CoordinadorProjection(
                                (UUID) row[0],
                                (UUID) row[1],
                                row[2] == null ? null : String.valueOf(row[2]),
                                (String) row[3],
                                (UUID) row[4],
                                (String) row[5],
                                flag(row[6]),
                                (String) row[7],
                                (String) row[8],
                                (String) row[9],
                                (String) row[10],
                                (String) row[11],
                                flag(row[12])
                        ))
                        .toList()
        );
    }

    @Override
    public List<ResumenCoordinacionProjection> consultarResumenProgramasFacultad(final UUID idFacultad) {
        return JpaQueryExecutor.execute(
                "consultarResumenProgramasFacultad",
                "No fue posible consultar el resumen de los programas de la facultad desde base de datos.",
                () -> {
                    final List<ResumenCoordinacionProjection> resumenes = new java.util.ArrayList<>();
                    for (final Object fila : entityManager.createNativeQuery(SQL_RESUMEN_PROGRAMAS_FACULTAD)
                            .setParameter("idFacultad", idFacultad).getResultList()) {
                        final Object[] columnas = (Object[]) fila;
                        resumenes.add(new ResumenCoordinacionProjection(uuid(columnas[0]), (String) columnas[1],
                                entero(columnas[2]), entero(columnas[3])));
                    }
                    return resumenes;
                }
        );
    }

    // PRIVATE HELPERS

    private static UUID uuid(final Object valor) {
        if (valor == null || valor instanceof UUID) {
            return (UUID) valor;
        }
        return UUID.fromString(String.valueOf(valor).trim());
    }

    private static int entero(final Object valor) {
        return valor == null ? 0 : ((Number) valor).intValue();
    }

    /** El SP devuelve el usuario del coordinador cuando la operacion tuvo exito; su ausencia es una violacion del contrato. */
    private static UUID idUsuarioDeFila(final ProcedureRow fila, final String operation) {
        final Object valor = fila.extras().getFirst();
        if (valor instanceof UUID uuid) {
            return uuid;
        }
        // SQL Server devuelve uniqueidentifier como texto en las consultas nativas (igual que la correlacion canonica).
        if (valor instanceof String texto) {
            try {
                return UUID.fromString(texto.trim());
            } catch (IllegalArgumentException exception) {
                throw new DatabaseOperationException(
                        DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                        "El procedimiento " + operation + " retorno un usuario con formato invalido.", exception);
            }
        }
        throw new DatabaseOperationException(
                DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                "El procedimiento " + operation + " no retorno el usuario del coordinador.");
    }

    private static boolean flag(final Object valor) {
        if (valor instanceof Boolean bit) {
            return bit;
        }
        return valor instanceof Number numero && numero.intValue() == 1;
    }

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
