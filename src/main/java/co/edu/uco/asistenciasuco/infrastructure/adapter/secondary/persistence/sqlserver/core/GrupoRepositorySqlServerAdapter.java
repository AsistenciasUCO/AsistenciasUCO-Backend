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
            (resultSet, rowNum) -> {
                final Object idObj = obtenerValorColumna(resultSet, "idEstudianteGrupo", "id");
                final Object docObj = obtenerValorColumna(resultSet, "numeroIdentificacion", "documento");
                final Object codEstadoObj = obtenerValorColumna(resultSet, "codigoEstadoEstudiante", "codigoEstado");
                final Object nomEstadoObj = obtenerValorColumna(resultSet, "nombreEstadoEstudiante", "nombreEstado");
                return new EstudianteGrupoRepositoryProjection(
                        JdbcValueMapper.toUuid(idObj),
                        JdbcValueMapper.toUuid(resultSet.getObject("idEstudiante")),
                        JdbcValueMapper.toString(docObj),
                        JdbcValueMapper.toString(resultSet.getObject("nombreCompleto")),
                        JdbcValueMapper.toString(resultSet.getObject("correo")),
                        JdbcValueMapper.toString(codEstadoObj),
                        JdbcValueMapper.toString(nomEstadoObj)
                );
            };

    private static Object obtenerValorColumna(final java.sql.ResultSet rs, final String columnaPrincipal, final String columnaFallback) {
        try {
            return rs.getObject(columnaPrincipal);
        } catch (java.sql.SQLException ignored) {
            try {
                return rs.getObject(columnaFallback);
            } catch (java.sql.SQLException e) {
                return null;
            }
        }
    }

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
    static final String PARAM_FILTRO_TEXTO = "filtroTexto";
    static final String PARAM_ID_ESTADO_ESTUDIANTE = "idEstadoEstudiante";
    static final String PARAM_NUMERO_PAGINA = "numeroPagina";
    static final String PARAM_TAMANO_PAGINA = "tamanoPagina";

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
            EXEC dbo.usp_consultar_grupos_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idPeriodoAcademico = :idPeriodoAcademico,
                 @idAsignatura = :idAsignatura,
                 @filtroTexto = :filtroTexto,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina
            """;
    static final String SQL_CONSULTAR_ESTUDIANTES_GRUPO = """
            EXEC dbo.usp_consultar_estudiantes_grupo_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idGrupo = :idGrupo,
                 @filtroTexto = :filtroTexto,
                 @idEstadoEstudiante = :idEstadoEstudiante,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina
            """;

    private final CanonicalStoredProcedureExecutor procedureExecutor;
    private final NamedParameterJdbcOperations jdbcOperations;
    private final TransactionOperations transactionOperations;
    private final co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.GrupoJpaCommandPersistence jpaCommandPersistence;
    private final co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.GrupoJpaQueryPersistence jpaQueryPersistence;

    public GrupoRepositorySqlServerAdapter(
            final CanonicalStoredProcedureExecutor procedureExecutor,
            final NamedParameterJdbcOperations jdbcOperations,
            final TransactionOperations transactionOperations
    ) {
        this(procedureExecutor, jdbcOperations, transactionOperations, null, null);
    }

    public GrupoRepositorySqlServerAdapter(
            final CanonicalStoredProcedureExecutor procedureExecutor,
            final NamedParameterJdbcOperations jdbcOperations,
            final TransactionOperations transactionOperations,
            final co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.GrupoJpaCommandPersistence jpaCommandPersistence
    ) {
        this(procedureExecutor, jdbcOperations, transactionOperations, jpaCommandPersistence, null);
    }

    public GrupoRepositorySqlServerAdapter(
            final CanonicalStoredProcedureExecutor procedureExecutor,
            final NamedParameterJdbcOperations jdbcOperations,
            final TransactionOperations transactionOperations,
            final co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.GrupoJpaCommandPersistence jpaCommandPersistence,
            final co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.GrupoJpaQueryPersistence jpaQueryPersistence
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
        this.jpaQueryPersistence = jpaQueryPersistence;
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
        return consultarGrupos(null);
    }

    @Override
    public List<GrupoRepositoryProjection> consultarGrupos(final UUID usuarioEjecutor) {
        if (jpaQueryPersistence != null) {
            return jpaQueryPersistence.consultarGrupos(usuarioEjecutor);
        }
        try {
            final UUID correlationId = CorrelationIdContext.get();
            final MapSqlParameterSource parameters = new MapSqlParameterSource()
                    .addValue(PARAM_ID_USUARIO_EJECUTOR, usuarioEjecutor)
                    .addValue(PARAM_ID_CORRELACION, correlationId)
                    .addValue(PARAM_ID_PERIODO_ACADEMICO, null)
                    .addValue(PARAM_ID_ASIGNATURA, null)
                    .addValue(PARAM_FILTRO_TEXTO, null)
                    .addValue(PARAM_NUMERO_PAGINA, 1)
                    .addValue(PARAM_TAMANO_PAGINA, 100);

            return jdbcOperations.query(SQL_CONSULTAR_GRUPOS, parameters, GRUPO_ROW_MAPPER);
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
        return consultarEstudiantesGrupo(grupoId, null);
    }

    @Override
    public List<EstudianteGrupoRepositoryProjection> consultarEstudiantesGrupo(final UUID grupoId, final UUID usuarioEjecutor) {
        if (jpaQueryPersistence != null) {
            return jpaQueryPersistence.consultarEstudiantesGrupo(grupoId, usuarioEjecutor);
        }
        try {
            final UUID correlationId = CorrelationIdContext.get();
            final MapSqlParameterSource parameters = new MapSqlParameterSource()
                    .addValue(PARAM_ID_USUARIO_EJECUTOR, usuarioEjecutor)
                    .addValue(PARAM_ID_CORRELACION, correlationId)
                    .addValue(PARAM_ID_GRUPO, grupoId)
                    .addValue(PARAM_FILTRO_TEXTO, null)
                    .addValue(PARAM_ID_ESTADO_ESTUDIANTE, null)
                    .addValue(PARAM_NUMERO_PAGINA, 1)
                    .addValue(PARAM_TAMANO_PAGINA, 100);

            return jdbcOperations.query(
                    SQL_CONSULTAR_ESTUDIANTES_GRUPO,
                    parameters,
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
