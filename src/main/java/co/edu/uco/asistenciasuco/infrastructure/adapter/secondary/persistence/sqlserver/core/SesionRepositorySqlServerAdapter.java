package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.GenerarSesionesGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalStoredProcedureExecutor;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador SQL Server para persistencia y consulta de sesiones de clase.
 */
public final class SesionRepositorySqlServerAdapter implements SesionRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(SesionRepositorySqlServerAdapter.class);

    private static final String PARAM_ID_GRUPO = "idGrupo";
    private static final String PARAM_ID_DOCENTE = "idDocente";
    private static final String PARAM_NOMBRE = "nombre";
    private static final String PARAM_FECHA_HORA_INICIO = "fechaHoraInicio";
    private static final String PARAM_FECHA_HORA_FIN = "fechaHoraFin";
    private static final String PARAM_ID_SESION = "idSesion";
    private static final String PARAM_ID_CORRELACION = "idCorrelacion";
    private static final String PARAM_ID_USUARIO_EJECUTOR = "idUsuarioEjecutor";
    private static final String PARAM_FECHA_DESDE = "fechaDesde";
    private static final String PARAM_FECHA_HASTA = "fechaHasta";
    private static final String PARAM_NUMERO_SEMANA = "numeroSemana";
    private static final String PARAM_NUMERO_PAGINA = "numeroPagina";
    private static final String PARAM_TAMANO_PAGINA = "tamanoPagina";

    static final String SQL_CREAR_SESION = """
            EXEC dbo.usp_crear_sesion
                 @idGrupo = :idGrupo,
                 @nombre = :nombre,
                 @fechaHoraInicio = :fechaHoraInicio,
                 @fechaHoraFin = :fechaHoraFin,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_CERRAR_SESION = """
            EXEC dbo.usp_cerrar_sesion
                 @idSesion = :idSesion,
                 @idDocente = :idDocente,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_ACTUALIZAR_SESION = """
            EXEC dbo.usp_actualizar_sesion
                 @idSesion = :idSesion,
                 @nombre = :nombre,
                 @fechaHoraInicio = :fechaHoraInicio,
                 @fechaHoraFin = :fechaHoraFin,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_GENERAR_SESIONES_GRUPO = """
            EXEC dbo.usp_generar_sesiones_grupo
                 @idGrupo = :idGrupo,
                 @idCorrelacion = :idCorrelacion,
                 @idUsuarioEjecutor = :idUsuarioEjecutor
            """;

    static final String SQL_CONSULTAR_POR_ID = """
            SELECT TOP 1
                id,
                idGrupo,
                nombre,
                numero,
                codigo,
                numeroSemana,
                codigoGrupo,
                nombreGrupo,
                fechaHoraInicio,
                fechaHoraFin
            FROM dbo.uv_sesion
            WHERE id = :idSesion
            """;

    static final String SQL_CONSULTAR_POR_GRUPO = """
            EXEC dbo.usp_consultar_sesiones_grupo_paginado
                 @idUsuarioEjecutor = :idUsuarioEjecutor,
                 @idCorrelacion = :idCorrelacion,
                 @idGrupo = :idGrupo,
                 @fechaDesde = :fechaDesde,
                 @fechaHasta = :fechaHasta,
                 @numeroSemana = :numeroSemana,
                 @numeroPagina = :numeroPagina,
                 @tamanoPagina = :tamanoPagina
            """;

    private final CanonicalStoredProcedureExecutor procedureExecutor;
    private final NamedParameterJdbcOperations jdbcOperations;
    private final co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.SesionJpaQueryPersistence jpaQueryPersistence;

    public SesionRepositorySqlServerAdapter(
            final CanonicalStoredProcedureExecutor procedureExecutor,
            final NamedParameterJdbcOperations jdbcOperations
    ) {
        this(procedureExecutor, jdbcOperations, null);
    }

    public SesionRepositorySqlServerAdapter(
            final CanonicalStoredProcedureExecutor procedureExecutor,
            final NamedParameterJdbcOperations jdbcOperations,
            final co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.SesionJpaQueryPersistence jpaQueryPersistence
    ) {
        this.procedureExecutor = Objects.requireNonNull(procedureExecutor, "El ejecutor canonico de sesiones es obligatorio.");
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "Las operaciones JDBC nombradas de sesiones son obligatorias.");
        this.jpaQueryPersistence = jpaQueryPersistence;
    }

    @Override
    public void crearSesion(final CrearSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para crear sesion es obligatorio.");
        }

        procedureExecutor.execute(
                "crearSesion",
                SQL_CREAR_SESION,
                new MapSqlParameterSource()
                        .addValue(PARAM_ID_GRUPO, dto.getGrupo())
                        .addValue(PARAM_NOMBRE, dto.getNombre())
                        .addValue(PARAM_FECHA_HORA_INICIO, dto.getFechaHoraInicio())
                        .addValue(PARAM_FECHA_HORA_FIN, dto.getFechaHoraFin())
                        .addValue(PARAM_ID_CORRELACION, CorrelationIdContext.require())
                        .addValue(PARAM_ID_USUARIO_EJECUTOR, dto.getUsuarioEjecutor())
        );
    }

    @Override
    public void actualizarSesion(final ActualizarSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para actualizar sesion es obligatorio.");
        }

        procedureExecutor.execute(
                "actualizarSesion",
                SQL_ACTUALIZAR_SESION,
                new MapSqlParameterSource()
                        .addValue(PARAM_ID_SESION, dto.sesion())
                        .addValue(PARAM_NOMBRE, dto.nombre())
                        .addValue(PARAM_FECHA_HORA_INICIO, dto.fechaHoraInicio())
                        .addValue(PARAM_FECHA_HORA_FIN, dto.fechaHoraFin())
                        .addValue(PARAM_ID_CORRELACION, CorrelationIdContext.require())
                        .addValue(PARAM_ID_USUARIO_EJECUTOR, dto.usuarioEjecutor())
        );
    }

    @Override
    public SesionRepositoryProjection consultarSesion(final ConsultarSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para consultar sesion es obligatorio.");
        }

        try {
            return jdbcOperations.query(SQL_CONSULTAR_POR_ID, new MapSqlParameterSource(PARAM_ID_SESION, dto.getSesion()), rs -> {
                if (!rs.next()) {
                    return null;
                }
                return new SesionRepositoryProjection(
                        JdbcValueMapper.toUuid(rs.getObject("id")),
                        JdbcValueMapper.toUuid(rs.getObject("idGrupo")),
                        JdbcValueMapper.toString(rs.getObject("nombre")),
                        JdbcValueMapper.toInteger(rs.getObject("numero")),
                        JdbcValueMapper.toString(rs.getObject("codigo")),
                        JdbcValueMapper.toInteger(rs.getObject("numeroSemana")),
                        JdbcValueMapper.toString(rs.getObject("codigoGrupo")),
                        JdbcValueMapper.toString(rs.getObject("nombreGrupo")),
                        JdbcValueMapper.toLocalDateTimeUtc(rs.getObject("fechaHoraInicio")),
                        JdbcValueMapper.toLocalDateTimeUtc(rs.getObject("fechaHoraFin"))
                );
            });
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation=consultarSesion, correlationId={}",
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar la sesion desde base de datos.", exception);
        }
    }

    @Override
    public List<SesionRepositoryProjection> consultarSesionesPorGrupo(final UUID grupoId) {
        return consultarSesionesPorGrupo(grupoId, null);
    }

    @Override
    public List<SesionRepositoryProjection> consultarSesionesPorGrupo(final UUID grupoId, final UUID usuarioEjecutor) {
        if (ObjectHelper.isNull(grupoId)) {
            throw new CrosscuttingException("El identificador del grupo para consultar sesiones es obligatorio.");
        }

        if (jpaQueryPersistence != null) {
            return jpaQueryPersistence.consultarSesionesPorGrupo(grupoId, usuarioEjecutor);
        }

        try {
            final UUID correlationId = CorrelationIdContext.get();
            final MapSqlParameterSource parameters = new MapSqlParameterSource()
                    .addValue(PARAM_ID_USUARIO_EJECUTOR, usuarioEjecutor)
                    .addValue(PARAM_ID_CORRELACION, correlationId)
                    .addValue(PARAM_ID_GRUPO, grupoId)
                    .addValue(PARAM_FECHA_DESDE, null)
                    .addValue(PARAM_FECHA_HASTA, null)
                    .addValue(PARAM_NUMERO_SEMANA, null)
                    .addValue(PARAM_NUMERO_PAGINA, 1)
                    .addValue(PARAM_TAMANO_PAGINA, 100);

            return jdbcOperations.query(SQL_CONSULTAR_POR_GRUPO, parameters, (rs, rowNum) ->
                    new SesionRepositoryProjection(
                            JdbcValueMapper.toUuid(rs.getObject("id")),
                            JdbcValueMapper.toUuid(rs.getObject("idGrupo")),
                            JdbcValueMapper.toString(rs.getObject("nombre")),
                            JdbcValueMapper.toInteger(rs.getObject("numero")),
                            JdbcValueMapper.toString(rs.getObject("codigo")),
                            JdbcValueMapper.toInteger(rs.getObject("numeroSemana")),
                            JdbcValueMapper.toString(rs.getObject("codigoGrupo")),
                            JdbcValueMapper.toString(rs.getObject("nombreGrupo")),
                            JdbcValueMapper.toLocalDateTimeUtc(rs.getObject("fechaHoraInicio")),
                            JdbcValueMapper.toLocalDateTimeUtc(rs.getObject("fechaHoraFin"))
                    )
            );
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "SQL operation failed. operation=consultarSesionesPorGrupo, correlationId={}",
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar las sesiones del grupo desde base de datos.", exception);
        }
    }

    @Override
    public void cerrarSesion(final CerrarSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para cerrar sesion es obligatorio.");
        }

        procedureExecutor.execute(
                "cerrarSesion",
                SQL_CERRAR_SESION,
                new MapSqlParameterSource()
                        .addValue(PARAM_ID_SESION, dto.getSesion())
                        .addValue(PARAM_ID_DOCENTE, dto.getDocente())
                        .addValue(PARAM_ID_CORRELACION, CorrelationIdContext.require())
                        .addValue(PARAM_ID_USUARIO_EJECUTOR, dto.getUsuarioEjecutor())
        );
    }

    @Override
    public void generarSesionesGrupo(final GenerarSesionesGrupoRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para generar sesiones de grupo es obligatorio.");
        }

        procedureExecutor.execute(
                "generarSesionesGrupo",
                SQL_GENERAR_SESIONES_GRUPO,
                new MapSqlParameterSource()
                        .addValue(PARAM_ID_GRUPO, dto.grupo())
                        .addValue(PARAM_ID_CORRELACION, CorrelationIdContext.require())
                        .addValue(PARAM_ID_USUARIO_EJECUTOR, dto.usuarioEjecutor())
        );
    }
}
