package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarEstudiantesRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteContextoAcademicoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteDetalleRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudiantePaginaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteResumenRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.EstudianteContextoAcademicoRepositoryRowMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.EstudianteResumenRepositoryRowMapper;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;

public final class EstudianteRepositorySqlServerAdapter implements EstudianteRepositoryPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(EstudianteRepositorySqlServerAdapter.class);
    private static final RowMapper<EstudianteResumenRepositoryProjection> ESTUDIANTE_RESUMEN_ROW_MAPPER =
            new EstudianteResumenRepositoryRowMapper();
    private static final RowMapper<EstudianteContextoAcademicoRepositoryProjection> ESTUDIANTE_CONTEXTO_ROW_MAPPER =
            new EstudianteContextoAcademicoRepositoryRowMapper();
    private static final String OPERATION_CONSULTAR_ESTUDIANTES = "consultarEstudiantes";
    private static final String OPERATION_CONSULTAR_ESTUDIANTE_POR_ID = "consultarEstudiantePorId";

    static final String BASE_FROM = """
            FROM dbo.uv_estudiante_identidad e
            INNER JOIN dbo.uv_usuario u ON e.idUsuario = u.id
            """;

    static final String SELECT_RESUMEN = """
            SELECT
                e.id AS id,
                u.id AS idUsuario,
                u.idTipoIdentificacion AS tipoIdentificacionId,
                u.numeroIdentificacion AS numeroIdentificacion,
                u.primerApellido AS primerApellido,
                u.segundoApellido AS segundoApellido,
                u.primerNombre AS primerNombre,
                u.segundoNombre AS segundoNombre,
                CONCAT_WS(' ', u.primerNombre, NULLIF(u.segundoNombre, ''), u.primerApellido, NULLIF(u.segundoApellido, '')) AS nombreCompleto,
                u.correo AS correo,
                u.estaActivoUsuario AS estaActivoUsuario
            """;

    static final String ORDER_PAGINADO = """

            ORDER BY u.primerApellido, u.primerNombre, u.numeroIdentificacion, e.id
            OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
            """;

    static final String SQL_DETALLE_ACADEMICO = """
            SELECT DISTINCT
                idInstitucion,
                nombreInstitucion,
                idFacultad,
                nombreFacultad,
                idPrograma,
                nombrePrograma,
                idPlanEstudio,
                inpPlanEstudio,
                idAsignatura,
                nombreAsignatura,
                idGrupo,
                nombreGrupo
            FROM dbo.uv_estudiante
            WHERE id = :estudianteId
            ORDER BY
                nombreInstitucion,
                nombreFacultad,
                nombrePrograma,
                nombreAsignatura,
                nombreGrupo,
                idGrupo
            """;

    private final NamedParameterJdbcOperations jdbcOperations;

    public EstudianteRepositorySqlServerAdapter(final JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(jdbcTemplate, "El JdbcTemplate para estudiantes es obligatorio.");
        this.jdbcOperations = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    EstudianteRepositorySqlServerAdapter(final NamedParameterJdbcOperations jdbcOperations) {
        this.jdbcOperations = Objects.requireNonNull(
                jdbcOperations,
                "El ejecutor SQL para estudiantes es obligatorio."
        );
    }

    @Override
    public EstudiantePaginaRepositoryProjection consultarEstudiantes(final ConsultarEstudiantesRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar estudiantes es obligatorio.");
        }

        try {
            final MapSqlParameterSource parameters = new MapSqlParameterSource();
            final String where = buildWhere(dto, parameters);
            final long totalItems = consultarTotal(where, parameters);
            parameters.addValue("offset", dto.page() * dto.size());
            parameters.addValue("size", dto.size());

            final List<EstudianteResumenRepositoryProjection> items = jdbcOperations.query(
                    SELECT_RESUMEN + BASE_FROM + where + ORDER_PAGINADO,
                    parameters,
                    ESTUDIANTE_RESUMEN_ROW_MAPPER
            );

            return new EstudiantePaginaRepositoryProjection(
                    items,
                    totalItems,
                    calcularTotalPages(totalItems, dto.size()),
                    dto.page(),
                    dto.size()
            );
        } catch (DataAccessException exception) {
            logSqlFailure(OPERATION_CONSULTAR_ESTUDIANTES, exception);
            throw new DatabaseOperationException("No fue posible consultar los estudiantes.", exception);
        }
    }

    @Override
    public Optional<EstudianteDetalleRepositoryProjection> consultarEstudiantePorId(final UUID estudianteId) {
        if (ObjectHelper.isNull(estudianteId)) {
            throw new CrosscuttingException("El identificador del estudiante es obligatorio.");
        }

        try {
            final MapSqlParameterSource parameters = new MapSqlParameterSource()
                    .addValue("estudianteId", estudianteId.toString());
            final List<EstudianteResumenRepositoryProjection> datosPersonales = jdbcOperations.query(
                    SELECT_RESUMEN + BASE_FROM + " WHERE e.id = :estudianteId",
                    parameters,
                    ESTUDIANTE_RESUMEN_ROW_MAPPER
            );
            if (datosPersonales.isEmpty()) {
                return Optional.empty();
            }

            final List<EstudianteContextoAcademicoRepositoryProjection> contextos = jdbcOperations.query(
                    SQL_DETALLE_ACADEMICO,
                    parameters,
                    ESTUDIANTE_CONTEXTO_ROW_MAPPER
            );
            return Optional.of(new EstudianteDetalleRepositoryProjection(datosPersonales.get(0), contextos));
        } catch (DataAccessException exception) {
            logSqlFailure(OPERATION_CONSULTAR_ESTUDIANTE_POR_ID, exception);
            throw new DatabaseOperationException("No fue posible consultar el estudiante por ID.", exception);
        }
    }

    private long consultarTotal(final String where, final SqlParameterSource parameters) {
        final Number total = jdbcOperations.queryForObject(
                "SELECT COUNT(1) " + BASE_FROM + where,
                parameters,
                Number.class
        );
        return total == null ? 0L : total.longValue();
    }

    private static String buildWhere(
            final ConsultarEstudiantesRepositoryDTO dto,
            final MapSqlParameterSource parameters
    ) {
        final StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        addUuidFilter(where, parameters, "tipoIdentificacionId", "u.idTipoIdentificacion", dto.tipoIdentificacionId());
        if (dto.numeroIdentificacion() != null) {
            where.append(" AND u.numeroIdentificacion = :numeroIdentificacion");
            parameters.addValue("numeroIdentificacion", dto.numeroIdentificacion());
        }
        if (!TextHelper.isNullOrBlank(dto.nombre())) {
            where.append("""
                     AND UPPER(CONCAT_WS(' ', u.primerNombre, NULLIF(u.segundoNombre, ''), u.primerApellido, NULLIF(u.segundoApellido, '')))
                         LIKE :nombre
                    """);
            parameters.addValue("nombre", "%" + dto.nombre().toUpperCase(Locale.ROOT) + "%");
        }
        if (!TextHelper.isNullOrBlank(dto.correo())) {
            where.append(" AND LOWER(u.correo) LIKE :correo");
            parameters.addValue("correo", "%" + dto.correo().toLowerCase(Locale.ROOT) + "%");
        }
        if (dto.activo() != null) {
            where.append(" AND u.estaActivoUsuario = :activo");
            parameters.addValue("activo", dto.activo());
        }
        appendAcademicFilters(where, parameters, dto);
        return where.toString();
    }

    private static void appendAcademicFilters(
            final StringBuilder where,
            final MapSqlParameterSource parameters,
            final ConsultarEstudiantesRepositoryDTO dto
    ) {
        if (dto.institucionId() == null && dto.facultadId() == null && dto.programaId() == null && dto.grupoId() == null) {
            return;
        }

        where.append("""
                 AND EXISTS (
                    SELECT 1
                    FROM dbo.uv_estudiante academico
                    WHERE academico.id = e.id
                """);
        addUuidFilter(where, parameters, "institucionId", "academico.idInstitucion", dto.institucionId());
        addUuidFilter(where, parameters, "facultadId", "academico.idFacultad", dto.facultadId());
        addUuidFilter(where, parameters, "programaId", "academico.idPrograma", dto.programaId());
        addUuidFilter(where, parameters, "grupoId", "academico.idGrupo", dto.grupoId());
        where.append(")");
    }

    private static void addUuidFilter(
            final StringBuilder where,
            final MapSqlParameterSource parameters,
            final String parameterName,
            final String columnName,
            final UUID value
    ) {
        if (value == null) {
            return;
        }
        where.append(" AND ").append(columnName).append(" = :").append(parameterName);
        parameters.addValue(parameterName, value.toString());
    }

    private static int calcularTotalPages(final long totalItems, final int size) {
        if (totalItems == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalItems / (double) size);
    }

    private void logSqlFailure(final String operation, final DataAccessException exception) {
        LOGGER.error(
                "SQL operation failed. operation={}, correlationId={}",
                operation,
                CorrelationIdContext.getAsString(),
                exception
        );
    }

}
