package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
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
 * Persistencia JPA de queries de Sesion sobre el Stored Procedure paginado
 * {@code dbo.usp_consultar_sesiones_grupo_paginado} (8 parametros).
 *
 * <p>Usa {@link StoredProcedureQuery} con binding posicional (Hibernate emite
 * {@code {call dbo.usp_xxx(?,?,?,...)}}) — el orden de
 * {@code registerStoredProcedureParameter} debe coincidir con
 * {@code sys.parameters.parameter_id} del SP.</p>
 *
 * <p>El resultado es un {@code READ_QUERY_RESULTSET}: filas de datos + {@code totalRegistros}
 * (no el contrato canonico de 4 columnas usado por commands).</p>
 */
public final class SesionJpaQueryPersistence {

    private static final Logger LOGGER = LoggerFactory.getLogger(SesionJpaQueryPersistence.class);

    private static final String SP_CONSULTAR_SESIONES = "dbo.usp_consultar_sesiones_grupo_paginado";
    private static final String OPERATION_CONSULTAR_SESIONES = "consultarSesionesPorGrupo";

    private static final String PARAM_ID_USUARIO_EJECUTOR = "idUsuarioEjecutor";
    private static final String PARAM_ID_CORRELACION = "idCorrelacion";
    private static final String PARAM_ID_GRUPO = "idGrupo";
    private static final String PARAM_FECHA_DESDE = "fechaDesde";
    private static final String PARAM_FECHA_HASTA = "fechaHasta";
    private static final String PARAM_NUMERO_SEMANA = "numeroSemana";
    private static final String PARAM_NUMERO_PAGINA = "numeroPagina";
    private static final String PARAM_TAMANO_PAGINA = "tamanoPagina";

    private final EntityManagerFactory entityManagerFactory;

    public SesionJpaQueryPersistence(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = Objects.requireNonNull(
                entityManagerFactory,
                "El EntityManagerFactory para queries de sesion es obligatorio."
        );
    }

    /**
     * Consulta paginada de sesiones de un grupo invocando
     * {@code dbo.usp_consultar_sesiones_grupo_paginado}.
     *
     * <p>Parametros del SP (orden posicional segun {@code sys.parameters}):
     * <ol>
     *     <li>{@code @idUsuarioEjecutor} — UNIQUEIDENTIFIER</li>
     *     <li>{@code @idCorrelacion} — UNIQUEIDENTIFIER</li>
     *     <li>{@code @idGrupo} — UNIQUEIDENTIFIER</li>
     *     <li>{@code @fechaDesde} — DATETIME2 = NULL</li>
     *     <li>{@code @fechaHasta} — DATETIME2 = NULL</li>
     *     <li>{@code @numeroSemana} — INT = NULL</li>
     *     <li>{@code @numeroPagina} — INT = 1</li>
     *     <li>{@code @tamanoPagina} — INT = 10</li>
     * </ol>
     * </p>
     *
     * <p>Columnas del resultado (orden posicional, 11 columnas):
     * id(0), idGrupo(1), codigoGrupo(2), nombreGrupo(3), nombre(4), numero(5),
     * codigo(6), numeroSemana(7), fechaHoraInicio(8), fechaHoraFin(9),
     * totalRegistros(10)</p>
     */
    public List<SesionRepositoryProjection> consultarSesionesPorGrupo(final UUID grupoId) {
        return consultarSesionesPorGrupo(grupoId, null);
    }

    public List<SesionRepositoryProjection> consultarSesionesPorGrupo(final UUID grupoId, final UUID usuarioEjecutor) {
        final UUID correlationId = CorrelationIdContext.get();

        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            final StoredProcedureQuery query = entityManager.createStoredProcedureQuery(SP_CONSULTAR_SESIONES);

            // Registro posicional (debe coincidir con sys.parameters.parameter_id)
            query.registerStoredProcedureParameter(PARAM_ID_USUARIO_EJECUTOR, UUID.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_ID_CORRELACION, UUID.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_ID_GRUPO, UUID.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_FECHA_DESDE, Timestamp.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_FECHA_HASTA, Timestamp.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_NUMERO_SEMANA, Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_NUMERO_PAGINA, Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_TAMANO_PAGINA, Integer.class, ParameterMode.IN);

            query.setParameter(PARAM_ID_USUARIO_EJECUTOR, usuarioEjecutor);
            query.setParameter(PARAM_ID_CORRELACION, correlationId);
            query.setParameter(PARAM_ID_GRUPO, grupoId);
            query.setParameter(PARAM_FECHA_DESDE, (Timestamp) null);
            query.setParameter(PARAM_FECHA_HASTA, (Timestamp) null);
            query.setParameter(PARAM_NUMERO_SEMANA, (Integer) null);
            query.setParameter(PARAM_NUMERO_PAGINA, 1);
            query.setParameter(PARAM_TAMANO_PAGINA, 100);

            final List<?> results = query.getResultList();
            final List<SesionRepositoryProjection> projections = new ArrayList<>(results.size());
            for (final Object row : results) {
                projections.add(mapSesionRow(row));
            }
            return projections;
        } catch (PersistenceException | IllegalStateException | IllegalArgumentException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_CONSULTAR_SESIONES,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible consultar las sesiones del grupo.", exception);
        }
    }

    /**
     * Mapea una fila {@code Object[]} del SP {@code usp_consultar_sesiones_grupo_paginado}
     * a {@link SesionRepositoryProjection}.
     *
     * <p>Columnas posicionales (0-based):
     * 0=id, 1=idGrupo, 2=codigoGrupo, 3=nombreGrupo, 4=nombre, 5=numero,
     * 6=codigo, 7=numeroSemana, 8=fechaHoraInicio, 9=fechaHoraFin,
     * 10=totalRegistros (ignorado)</p>
     */
    private SesionRepositoryProjection mapSesionRow(final Object resultRow) {
        if (!(resultRow instanceof Object[] columns) || columns.length < 10) {
            throw new DatabaseOperationException(
                    DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                    "El SP de sesiones retorno un formato de fila inesperado (se esperaban al menos 10 columnas)."
            );
        }
        return new SesionRepositoryProjection(
                JdbcValueMapper.toUuid(columns[0]),              // id -> sesion
                JdbcValueMapper.toUuid(columns[1]),              // idGrupo -> grupo
                JdbcValueMapper.toString(columns[4]),            // nombre
                JdbcValueMapper.toInteger(columns[5]),           // numero
                JdbcValueMapper.toString(columns[6]),            // codigo
                JdbcValueMapper.toInteger(columns[7]),           // numeroSemana
                JdbcValueMapper.toString(columns[2]),            // codigoGrupo
                JdbcValueMapper.toString(columns[3]),            // nombreGrupo
                JdbcValueMapper.toLocalDateTimeUtc(columns[8]),  // fechaHoraInicio
                JdbcValueMapper.toLocalDateTimeUtc(columns[9])   // fechaHoraFin
        );
    }
}
