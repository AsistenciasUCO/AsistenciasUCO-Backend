package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteGrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.StoredProcedureQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistencia JPA de queries de Grupo sobre los Stored Procedures paginados:
 * <ul>
 *     <li>{@code dbo.usp_consultar_grupos_paginado} (7 parametros)</li>
 *     <li>{@code dbo.usp_consultar_estudiantes_grupo_paginado} (7 parametros)</li>
 * </ul>
 *
 * <p>Usa {@link StoredProcedureQuery} con binding posicional (Hibernate emite
 * {@code {call dbo.usp_xxx(?,?,?,...)}}) — el orden de
 * {@code registerStoredProcedureParameter} debe coincidir con
 * {@code sys.parameters.parameter_id} del SP.</p>
 *
 * <p>El resultado es un {@code READ_QUERY_RESULTSET}: filas de datos + {@code totalRegistros}
 * (no el contrato canonico de 4 columnas usado por commands). Cada fila se mapea posicionalmente
 * a la proyeccion de aplicacion correspondiente.</p>
 */
public final class GrupoJpaQueryPersistence {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrupoJpaQueryPersistence.class);

    private static final String SP_CONSULTAR_GRUPOS = "dbo.usp_consultar_grupos_paginado";
    private static final String SP_CONSULTAR_ESTUDIANTES = "dbo.usp_consultar_estudiantes_grupo_paginado";

    private static final String OPERATION_CONSULTAR_GRUPOS = "consultarGrupos";
    private static final String OPERATION_CONSULTAR_ESTUDIANTES = "consultarEstudiantesGrupo";

    // Parametros comunes
    private static final String PARAM_ID_USUARIO_EJECUTOR = "idUsuarioEjecutor";
    private static final String PARAM_ID_CORRELACION = "idCorrelacion";
    private static final String PARAM_ID_PERIODO_ACADEMICO = "idPeriodoAcademico";
    private static final String PARAM_ID_ASIGNATURA = "idAsignatura";
    private static final String PARAM_FILTRO_TEXTO = "filtroTexto";
    private static final String PARAM_NUMERO_PAGINA = "numeroPagina";
    private static final String PARAM_TAMANO_PAGINA = "tamanoPagina";
    private static final String PARAM_ID_GRUPO = "idGrupo";
    private static final String PARAM_ID_ESTADO_ESTUDIANTE = "idEstadoEstudiante";

    private final EntityManagerFactory entityManagerFactory;

    public GrupoJpaQueryPersistence(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = Objects.requireNonNull(
                entityManagerFactory,
                "El EntityManagerFactory para queries de grupo es obligatorio."
        );
    }

    /**
     * Consulta paginada de grupos invocando {@code dbo.usp_consultar_grupos_paginado}.
     *
     * <p>Parametros del SP (orden posicional segun {@code sys.parameters}):
     * <ol>
     *     <li>{@code @idUsuarioEjecutor} — UNIQUEIDENTIFIER</li>
     *     <li>{@code @idCorrelacion} — UNIQUEIDENTIFIER</li>
     *     <li>{@code @idPeriodoAcademico} — UNIQUEIDENTIFIER = NULL</li>
     *     <li>{@code @idAsignatura} — UNIQUEIDENTIFIER = NULL</li>
     *     <li>{@code @filtroTexto} — NVARCHAR(100) = NULL</li>
     *     <li>{@code @numeroPagina} — INT = 1</li>
     *     <li>{@code @tamanoPagina} — INT = 10</li>
     * </ol>
     * </p>
     *
     * <p>Columnas del resultado (orden posicional):
     * id, codigo, nombre, idAsignatura, nombreAsignatura, idDocente,
     * capacidadMaximaPermitida, estudiantesActivos, cuposDisponibles,
     * grupoEstaHablitado, fechaInicioPeriodoAcademico, fechaFinPeriodoAcademico,
     * totalRegistros</p>
     */
    public List<GrupoRepositoryProjection> consultarGrupos() {
        return consultarGrupos(null);
    }

    public List<GrupoRepositoryProjection> consultarGrupos(final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.get();

        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            final StoredProcedureQuery query = entityManager.createStoredProcedureQuery(SP_CONSULTAR_GRUPOS);

            // Registro posicional (debe coincidir con sys.parameters.parameter_id)
            query.registerStoredProcedureParameter(PARAM_ID_USUARIO_EJECUTOR, UUID.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_ID_CORRELACION, UUID.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_ID_PERIODO_ACADEMICO, UUID.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_ID_ASIGNATURA, UUID.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_FILTRO_TEXTO, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_NUMERO_PAGINA, Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_TAMANO_PAGINA, Integer.class, ParameterMode.IN);

            query.setParameter(PARAM_ID_USUARIO_EJECUTOR, usuarioEjecutor);
            query.setParameter(PARAM_ID_CORRELACION, correlationId);
            query.setParameter(PARAM_ID_PERIODO_ACADEMICO, (UUID) null);
            query.setParameter(PARAM_ID_ASIGNATURA, (UUID) null);
            query.setParameter(PARAM_FILTRO_TEXTO, (String) null);
            query.setParameter(PARAM_NUMERO_PAGINA, 1);
            query.setParameter(PARAM_TAMANO_PAGINA, 100);

            final List<?> results = query.getResultList();
            final List<GrupoRepositoryProjection> projections = new ArrayList<>(results.size());
            for (final Object row : results) {
                projections.add(mapGrupoRow(row));
            }
            return projections;
        } catch (PersistenceException | IllegalStateException | IllegalArgumentException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_CONSULTAR_GRUPOS,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar los grupos.", exception);
        }
    }

    /**
     * Consulta paginada de estudiantes de un grupo invocando
     * {@code dbo.usp_consultar_estudiantes_grupo_paginado}.
     *
     * <p>Parametros del SP (orden posicional segun {@code sys.parameters}):
     * <ol>
     *     <li>{@code @idUsuarioEjecutor} — UNIQUEIDENTIFIER</li>
     *     <li>{@code @idCorrelacion} — UNIQUEIDENTIFIER</li>
     *     <li>{@code @idGrupo} — UNIQUEIDENTIFIER</li>
     *     <li>{@code @filtroTexto} — NVARCHAR(100) = NULL</li>
     *     <li>{@code @idEstadoEstudiante} — UNIQUEIDENTIFIER = NULL</li>
     *     <li>{@code @numeroPagina} — INT = 1</li>
     *     <li>{@code @tamanoPagina} — INT = 10</li>
     * </ol>
     * </p>
     *
     * <p>Columnas del resultado (orden posicional, 20 columnas):
     * idEstudianteGrupo(0), idEstudiante(1), idUsuario(2), tipoIdentificacionId(3),
     * numeroIdentificacion(4), primerApellido(5), segundoApellido(6), primerNombre(7),
     * segundoNombre(8), nombreCompleto(9), correo(10), idEstadoEstudiante(11),
     * codigoEstadoEstudiante(12), nombreEstadoEstudiante(13), estaActivoUsuario(14),
     * totalFallas(15), totalJustificadas(16), totalAsistencias(17),
     * totalRegistrosAsistencia(18), totalRegistros(19)</p>
     */
    public List<EstudianteGrupoRepositoryProjection> consultarEstudiantesGrupo(final UUID grupoId) {
        return consultarEstudiantesGrupo(grupoId, null);
    }

    public List<EstudianteGrupoRepositoryProjection> consultarEstudiantesGrupo(final UUID grupoId, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.get();

        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            final StoredProcedureQuery query = entityManager.createStoredProcedureQuery(SP_CONSULTAR_ESTUDIANTES);

            query.registerStoredProcedureParameter(PARAM_ID_USUARIO_EJECUTOR, UUID.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_ID_CORRELACION, UUID.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_ID_GRUPO, UUID.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_FILTRO_TEXTO, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_ID_ESTADO_ESTUDIANTE, UUID.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_NUMERO_PAGINA, Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_TAMANO_PAGINA, Integer.class, ParameterMode.IN);

            query.setParameter(PARAM_ID_USUARIO_EJECUTOR, usuarioEjecutor);
            query.setParameter(PARAM_ID_CORRELACION, correlationId);
            query.setParameter(PARAM_ID_GRUPO, grupoId);
            query.setParameter(PARAM_FILTRO_TEXTO, (String) null);
            query.setParameter(PARAM_ID_ESTADO_ESTUDIANTE, (UUID) null);
            query.setParameter(PARAM_NUMERO_PAGINA, 1);
            query.setParameter(PARAM_TAMANO_PAGINA, 100);

            final List<?> results = query.getResultList();
            final List<EstudianteGrupoRepositoryProjection> projections = new ArrayList<>(results.size());
            for (final Object row : results) {
                projections.add(mapEstudianteRow(row));
            }
            return projections;
        } catch (PersistenceException | IllegalStateException | IllegalArgumentException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_CONSULTAR_ESTUDIANTES,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar los estudiantes del grupo.", exception);
        }
    }

    /**
     * Mapea una fila {@code Object[]} del SP {@code usp_consultar_grupos_paginado}
     * a {@link GrupoRepositoryProjection}.
     *
     * <p>Columnas posicionales (0-based):
     * 0=id, 1=codigo, 2=nombre, 3=idAsignatura, 4=nombreAsignatura, 5=idDocente,
     * 6=capacidadMaximaPermitida, 7=estudiantesActivos, 8=cuposDisponibles,
     * 9=grupoEstaHablitado, 10=fechaInicioPeriodoAcademico, 11=fechaFinPeriodoAcademico,
     * 12=totalRegistros (ignorado)</p>
     */
    private GrupoRepositoryProjection mapGrupoRow(final Object resultRow) {
        if (!(resultRow instanceof Object[] columns) || columns.length < 12) {
            throw new DatabaseOperationException(
                    DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                    "El SP de grupos retorno un formato de fila inesperado (se esperaban al menos 12 columnas)."
            );
        }
        return new GrupoRepositoryProjection(
                JdbcValueMapper.toUuid(columns[0]),          // id
                JdbcValueMapper.toString(columns[1]),        // codigo
                JdbcValueMapper.toString(columns[2]),        // nombre
                JdbcValueMapper.toUuid(columns[3]),          // idAsignatura
                JdbcValueMapper.toString(columns[4]),        // nombreAsignatura
                JdbcValueMapper.toUuid(columns[5]),          // idDocente
                JdbcValueMapper.toInteger(columns[6]),       // capacidadMaximaPermitida
                JdbcValueMapper.toInteger(columns[7]),       // estudiantesActivos
                JdbcValueMapper.toInteger(columns[8]),       // cuposDisponibles
                JdbcValueMapper.toBoolean(columns[9]),       // grupoEstaHablitado
                JdbcValueMapper.toLocalDate(columns[10]),    // fechaInicioPeriodoAcademico
                JdbcValueMapper.toLocalDate(columns[11])     // fechaFinPeriodoAcademico
        );
    }

    /**
     * Mapea una fila {@code Object[]} del SP {@code usp_consultar_estudiantes_grupo_paginado}
     * a {@link EstudianteGrupoRepositoryProjection}.
     *
     * <p>Columnas posicionales (0-based):
     * 0=idEstudianteGrupo, 1=idEstudiante, 2=idUsuario, 3=tipoIdentificacionId,
     * 4=numeroIdentificacion, 5=primerApellido, 6=segundoApellido, 7=primerNombre,
     * 8=segundoNombre, 9=nombreCompleto, 10=correo, 11=idEstadoEstudiante,
     * 12=codigoEstadoEstudiante, 13=nombreEstadoEstudiante, 14=estaActivoUsuario,
     * 15=totalFallas, 16=totalJustificadas, 17=totalAsistencias,
     * 18=totalRegistrosAsistencia, 19=totalRegistros (ignorado)</p>
     */
    private EstudianteGrupoRepositoryProjection mapEstudianteRow(final Object resultRow) {
        if (!(resultRow instanceof Object[] columns) || columns.length < 14) {
            throw new DatabaseOperationException(
                    DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                    "El SP de estudiantes retorno un formato de fila inesperado (se esperaban al menos 14 columnas)."
            );
        }
        return new EstudianteGrupoRepositoryProjection(
                JdbcValueMapper.toUuid(columns[0]),          // idEstudianteGrupo -> id
                JdbcValueMapper.toUuid(columns[1]),          // idEstudiante
                JdbcValueMapper.toString(columns[4]),        // numeroIdentificacion -> documento
                JdbcValueMapper.toString(columns[9]),        // nombreCompleto
                JdbcValueMapper.toString(columns[10]),       // correo
                JdbcValueMapper.toString(columns[12]),       // codigoEstadoEstudiante -> codigoEstado
                JdbcValueMapper.toString(columns[13])        // nombreEstadoEstudiante -> nombreEstado
        );
    }
}
