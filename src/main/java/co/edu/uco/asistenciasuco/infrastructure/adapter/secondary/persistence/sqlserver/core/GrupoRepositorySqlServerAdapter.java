package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteGrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoCommandRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.GrupoRepositoryRowMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador SQL Server para operaciones publicas de grupos.
 */
public final class GrupoRepositorySqlServerAdapter implements GrupoRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrupoRepositorySqlServerAdapter.class);
    private static final RowMapper<GrupoRepositoryProjection> GRUPO_ROW_MAPPER = new GrupoRepositoryRowMapper();
    private static final RowMapper<EstudianteGrupoRepositoryProjection> ESTUDIANTE_GRUPO_ROW_MAPPER =
            (resultSet, rowNum) -> new EstudianteGrupoRepositoryProjection(
                    JdbcValueMapper.toUuid(resultSet.getObject("id")),
                    JdbcValueMapper.toUuid(resultSet.getObject("idEstudiante")),
                    JdbcValueMapper.toString(resultSet.getObject("documento")),
                    JdbcValueMapper.toString(resultSet.getObject("nombreCompleto")),
                    JdbcValueMapper.toString(resultSet.getObject("correo")),
                    JdbcValueMapper.toString(resultSet.getObject("codigoEstado")),
                    JdbcValueMapper.toString(resultSet.getObject("nombreEstado"))
            );

    private static final String OPERATION_CREAR_GRUPO = "crearGrupo";
    private static final String OPERATION_ACTUALIZAR_GRUPO = "actualizarGrupo";
    private static final String OPERATION_REGISTRAR_ESTUDIANTE = "registrarEstudianteEnGrupo";
    private static final String OPERATION_CONSULTAR_GRUPOS = "consultarGrupos";
    private static final String OPERATION_CONSULTAR_ESTUDIANTES = "consultarEstudiantesGrupo";

    static final String PARAM_ID_TIPO_ID_IDENTIFICACION = "idTipoIdIdentificacion";
    static final String PARAM_NUMERO_IDENTIFICACION = "numeroIdentificacion";
    static final String PARAM_PRIMER_APELLIDO = "primerApellido";
    static final String PARAM_SEGUNDO_APELLIDO = "segundoApellido";
    static final String PARAM_PRIMER_NOMBRE = "primerNombre";
    static final String PARAM_SEGUNDO_NOMBRE = "segundoNombre";
    static final String PARAM_CORREO = "correo";
    static final String PARAM_PASSWORD = "password";
    static final String PARAM_ID_GRUPO = "idGrupo";
    static final String PARAM_ID_ASIGNATURA = "idAsignatura";
    static final String PARAM_ID_PERIODO_ACADEMICO = "idPeriodoAcademico";
    static final String PARAM_CODIGO = "codigo";
    static final String PARAM_NOMBRE = "nombre";
    static final String PARAM_ID_DOCENTE = "idDocente";
    static final String PARAM_CUPO_MAXIMO = "cupoMaximo";
    static final String PARAM_ID_CORRELACION = "idCorrelacion";
    static final String PARAM_ID_USUARIO_EJECUTOR = "idUsuarioEjecutor";

    static final String SQL_CREAR_GRUPO = """
            EXEC dbo.usp_crear_grupo
                 @idGrupo = :idGrupo,
                 @idAsignatura = :idAsignatura,
                 @idPeriodoAcademico = :idPeriodoAcademico,
                 @codigo = :codigo,
                 @nombre = :nombre,
                 @idDocente = :idDocente,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_ACTUALIZAR_GRUPO = """
            EXEC dbo.usp_actualizar_grupo
                 @idGrupo = :idGrupo,
                 @codigo = :codigo,
                 @nombre = :nombre,
                 @idDocente = :idDocente,
                 @cupoMaximo = :cupoMaximo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;
    static final String SQL_REGISTRAR_ESTUDIANTE = """
            EXEC dbo.usp_registrar_estudiante_en_grupo
                 @idGrupo = :idGrupo,
                 @numeroIdentificacion = :numeroIdentificacion,
                 @primerNombre = :primerNombre,
                 @segundoNombre = :segundoNombre,
                 @primerApellido = :primerApellido,
                 @segundoApellido = :segundoApellido,
                 @correo = :correo,
                 @password = :password,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idTipoIdIdentificacion = :idTipoIdIdentificacion
            """;
    static final String SQL_CONSULTAR_GRUPOS = """
            SELECT
                id,
                codigo,
                nombre,
                idAsignatura,
                nombreAsignatura,
                idDocente,
                capacidadMaximaPermitida,
                estudiantesActivos,
                cuposDisponibles,
                grupoEstaHablitado,
                fechaInicioPeriodoAcademico,
                fechaFinPeriodoAcademico
            FROM dbo.uv_grupo
            ORDER BY nombreAsignatura, codigo, nombre, id
            """;
    static final String SQL_CONSULTAR_ESTUDIANTES_GRUPO = """
            SELECT
                eg.id,
                eg.idEstudiante,
                CAST(ei.numeroIdentificacion AS VARCHAR(20)) AS documento,
                ei.nombreCompleto AS nombreCompleto,
                u.correo AS correo,
                eg.codigoEstadoEstudiante AS codigoEstado,
                eg.nombreEstadoEstudiante AS nombreEstado
            FROM dbo.uv_estudiante_grupo eg
            INNER JOIN dbo.uv_estudiante_identidad ei
                    ON ei.id = eg.idEstudiante
            INNER JOIN dbo.uv_usuario u
                    ON u.id = ei.idUsuario
            WHERE eg.idGrupo = :idGrupo
            ORDER BY ei.nombreCompleto, eg.id
            """;

    private final CanonicalStoredProcedureExecutor procedureExecutor;
    private final NamedParameterJdbcOperations jdbcOperations;
    private final TransactionOperations transactionOperations;
    private final co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.GrupoJpaCommandPersistence jpaCommandPersistence;

    public GrupoRepositorySqlServerAdapter(
            final CanonicalStoredProcedureExecutor procedureExecutor,
            final NamedParameterJdbcOperations jdbcOperations,
            final TransactionOperations transactionOperations
    ) {
        this(procedureExecutor, jdbcOperations, transactionOperations, null);
    }

    public GrupoRepositorySqlServerAdapter(
            final CanonicalStoredProcedureExecutor procedureExecutor,
            final NamedParameterJdbcOperations jdbcOperations,
            final TransactionOperations transactionOperations,
            final co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.GrupoJpaCommandPersistence jpaCommandPersistence
    ) {
        this.procedureExecutor = Objects.requireNonNull(
                procedureExecutor,
                "El ejecutor canonico de procedimientos de grupo es obligatorio."
        );
        this.jdbcOperations = Objects.requireNonNull(
                jdbcOperations,
                "Las operaciones JDBC nombradas de grupo son obligatorias."
        );
        this.transactionOperations = Objects.requireNonNull(
                transactionOperations,
                "Las operaciones transaccionales de grupo son obligatorias."
        );
        this.jpaCommandPersistence = jpaCommandPersistence;
    }

    @Override
    public GrupoCommandRepositoryProjection crearGrupo(final CrearGrupoRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para crear grupo es obligatorio.");
        }
        return transactionOperations.execute(status -> {
            final CanonicalProcedureResult result = procedureExecutor.execute(
                    OPERATION_CREAR_GRUPO,
                    SQL_CREAR_GRUPO,
                    buildCrearGrupoParameters(dto, CorrelationIdContext.require())
            );
            return new GrupoCommandRepositoryProjection(dto.idGrupo(), result.getMensajeUsuarioResultado());
        });
    }

    @Override
    public GrupoCommandRepositoryProjection actualizarGrupo(final ActualizarGrupoRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para actualizar grupo es obligatorio.");
        }
        return transactionOperations.execute(status -> {
            final CanonicalProcedureResult result = procedureExecutor.execute(
                    OPERATION_ACTUALIZAR_GRUPO,
                    SQL_ACTUALIZAR_GRUPO,
                    buildActualizarGrupoParameters(dto, CorrelationIdContext.require())
            );
            return new GrupoCommandRepositoryProjection(dto.idGrupo(), result.getMensajeUsuarioResultado());
        });
    }

    @Override
    public RegistrarEstudianteRepositoryProjection registrarEstudianteEnGrupo(final RegistrarEstudianteRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para registrar estudiante en grupo es obligatorio.");
        }
        if (jpaCommandPersistence != null) {
            return jpaCommandPersistence.registrarEstudianteEnGrupo(dto);
        }
        return transactionOperations.execute(status -> {
            final CanonicalProcedureResult result = procedureExecutor.execute(
                    OPERATION_REGISTRAR_ESTUDIANTE,
                    SQL_REGISTRAR_ESTUDIANTE,
                    buildRegistrarEstudianteParameters(dto, CorrelationIdContext.require())
            );
            return new RegistrarEstudianteRepositoryProjection(result.getMensajeUsuarioResultado());
        });
    }

    @Override
    public List<GrupoRepositoryProjection> consultarGrupos() {
        try {
            return jdbcOperations.query(SQL_CONSULTAR_GRUPOS, GRUPO_ROW_MAPPER);
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_CONSULTAR_GRUPOS,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar los grupos.", exception);
        }
    }

    @Override
    public List<EstudianteGrupoRepositoryProjection> consultarEstudiantesGrupo(final UUID grupoId) {
        try {
            return jdbcOperations.query(
                    SQL_CONSULTAR_ESTUDIANTES_GRUPO,
                    new MapSqlParameterSource().addValue(PARAM_ID_GRUPO, grupoId),
                    ESTUDIANTE_GRUPO_ROW_MAPPER
            );
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_CONSULTAR_ESTUDIANTES,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar los estudiantes del grupo.", exception);
        }
    }

    private MapSqlParameterSource buildCrearGrupoParameters(final CrearGrupoRepositoryDTO dto, final UUID correlationId) {
        return new MapSqlParameterSource()
                .addValue(PARAM_ID_GRUPO, dto.idGrupo())
                .addValue(PARAM_ID_ASIGNATURA, dto.idAsignatura())
                .addValue(PARAM_ID_PERIODO_ACADEMICO, dto.idPeriodoAcademico())
                .addValue(PARAM_CODIGO, dto.codigo())
                .addValue(PARAM_NOMBRE, dto.nombre())
                .addValue(PARAM_ID_DOCENTE, dto.idDocente())
                .addValue(PARAM_ID_CORRELACION, correlationId)
                .addValue(PARAM_ID_USUARIO_EJECUTOR, dto.usuarioEjecutor());
    }

    private MapSqlParameterSource buildActualizarGrupoParameters(final ActualizarGrupoRepositoryDTO dto, final UUID correlationId) {
        return new MapSqlParameterSource()
                .addValue(PARAM_ID_GRUPO, dto.idGrupo())
                .addValue(PARAM_CODIGO, dto.codigo())
                .addValue(PARAM_NOMBRE, dto.nombre())
                .addValue(PARAM_ID_DOCENTE, dto.idDocente())
                .addValue(PARAM_CUPO_MAXIMO, dto.cupoMaximo())
                .addValue(PARAM_ID_CORRELACION, correlationId)
                .addValue(PARAM_ID_USUARIO_EJECUTOR, dto.usuarioEjecutor());
    }

    private MapSqlParameterSource buildRegistrarEstudianteParameters(
            final RegistrarEstudianteRepositoryDTO dto,
            final UUID correlationId
    ) {
        return new MapSqlParameterSource()
                .addValue(PARAM_ID_GRUPO, dto.getGrupoId())
                .addValue(PARAM_NUMERO_IDENTIFICACION, dto.getNumeroIdentificacion())
                .addValue(PARAM_PRIMER_NOMBRE, dto.getPrimerNombre())
                .addValue(PARAM_SEGUNDO_NOMBRE, dto.getSegundoNombre())
                .addValue(PARAM_PRIMER_APELLIDO, dto.getPrimerApellido())
                .addValue(PARAM_SEGUNDO_APELLIDO, dto.getSegundoApellido())
                .addValue(PARAM_CORREO, dto.getCorreo())
                .addValue(PARAM_PASSWORD, dto.getPassword())
                .addValue(PARAM_ID_CORRELACION, correlationId)
                .addValue(PARAM_ID_USUARIO_EJECUTOR, dto.getUsuarioEjecutor())
                .addValue(PARAM_ID_TIPO_ID_IDENTIFICACION, dto.getTipoIdentificacionId());
    }
}
