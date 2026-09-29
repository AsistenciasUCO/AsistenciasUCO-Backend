package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core.AsistenciaCommandPersistence;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalProcedureResultValidator;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.StoredProcedureQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Candidato JPA (LB-002.2) del command {@code registrarAsistenciasSesion}: invoca EL MISMO
 * {@code dbo.usp_registrar_asistencias_sesion} que el baseline JDBC mediante
 * {@link StoredProcedureQuery}. No duplica reglas de negocio: el estado del lote se envia tal cual y la
 * DB conserva la validacion, la atomicidad y la autorizacion de titularidad.
 *
 * <p><b>Binding POSICIONAL (decision U-03, evidencia SQL Server real de LB-002.2B):</b> Hibernate emite
 * {@code {call dbo.usp_registrar_asistencias_sesion(?,?,?,?)}}; los nombres registrados NO gobiernan el
 * binding, gobierna el ORDEN DE REGISTRO. El orden es el de {@code sys.parameters.parameter_id} y se
 * protege por IT (CMD-PAR-013); no se consulta metadata DB en runtime:
 * {@code 1 idSesion (UUID)}, {@code 2 asistenciaJSON (String, nvarchar(max))},
 * {@code 3 idCorrelacion (UUID)}, {@code 4 idUsuarioEjecutor (UUID, sin sustituir su {@code null})}.</p>
 *
 * <p><b>Sin transaccion JPA (D4, validada en 2.2B):</b> un {@link EntityManager} por invocacion, sin
 * {@code @Transactional}, sin {@code EntityTransaction} y sin {@code joinTransaction}; el SP gestiona su
 * propia transaccion y la conexion corre en autocommit como en el baseline. El
 * {@code EntityManager} se cierra siempre. Solo {@code StoredProcedureQuery}: sin {@code Session.doWork}
 * ni {@code unwrap} JDBC.</p>
 *
 * <p>El resultado canonico (una fila de 4 columnas) se consume por estado
 * ({@code execute/getResultList/getUpdateCount/hasMoreResults}), tolerando update counts previos; su
 * ausencia son 0 filas y la evalua {@link CanonicalProcedureResultValidator} como
 * {@code ERR_DB_CANONICAL_CONTRACT}, no como fallo tecnico.</p>
 */
public final class AsistenciaJpaCommandPersistence implements AsistenciaCommandPersistence {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsistenciaJpaCommandPersistence.class);
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    static final String PROCEDURE = "dbo.usp_registrar_asistencias_sesion";
    static final String OPERATION = "registrarAsistenciasSesion";

    private static final int CANONICAL_COLUMNS = 4;
    /** Cota defensiva del recorrido de resultados del SP (result sets + update counts). */
    private static final int MAX_RESULT_STEPS = 64;

    private final EntityManagerFactory entityManagerFactory;

    public AsistenciaJpaCommandPersistence(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = Objects.requireNonNull(
                entityManagerFactory,
                "El EntityManagerFactory del command de asistencia es obligatorio."
        );
    }

    @Override
    public void registrarAsistenciasSesion(final RegistrarAsistenciasSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para registrar asistencias por sesion es obligatorio.");
        }

        final UUID correlationId = CorrelationIdContext.require();
        final String asistenciaJson = serializarRegistros(dto);

        final List<CanonicalProcedureResult> results = ejecutar(dto, asistenciaJson, correlationId);

        CanonicalProcedureResultValidator.validate(results, correlationId, OPERATION);
    }

    /**
     * Ejecuta el SP y mapea el canal canonico. Solo aqui se traducen fallos tecnicos de JPA/Hibernate: la
     * validacion semantica ocurre despues y sus excepciones nunca se degradan a fallo tecnico.
     */
    private List<CanonicalProcedureResult> ejecutar(
            final RegistrarAsistenciasSesionRepositoryDTO dto,
            final String asistenciaJson,
            final UUID correlationId
    ) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            final StoredProcedureQuery query = entityManager.createStoredProcedureQuery(PROCEDURE);
            query.registerStoredProcedureParameter(1, UUID.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(3, UUID.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(4, UUID.class, ParameterMode.IN);
            query.setParameter(1, dto.sesion());
            query.setParameter(2, asistenciaJson);
            query.setParameter(3, correlationId);
            query.setParameter(4, dto.usuarioEjecutor());

            final List<CanonicalProcedureResult> results = new ArrayList<>();
            for (final Object[] row : leerFilasCanonicas(query)) {
                results.add(new CanonicalProcedureResult(
                        JdbcValueMapper.toUuid(row[0]),
                        JdbcValueMapper.toString(row[1]),
                        JdbcValueMapper.toString(row[2]),
                        JdbcValueMapper.toBoolean(row[3])
                ));
            }
            return results;
        } catch (PersistenceException | IllegalStateException | IllegalArgumentException exception) {
            LOGGER.error("SQL operation failed. operation={}, correlationId={}", OPERATION, correlationId);
            throw new DatabaseOperationException(
                    DatabaseErrorCode.DATABASE_OPERATION_ERROR,
                    "No fue posible ejecutar el procedimiento almacenado.",
                    exception
            );
        }
    }

    /** Recorre result sets y update counts; retorna las filas de 4 columnas (0 si no hay canal canonico). */
    private static List<Object[]> leerFilasCanonicas(final StoredProcedureQuery query) {
        final List<Object[]> rows = new ArrayList<>();
        boolean hasResultSet = query.execute();
        for (int step = 0; step < MAX_RESULT_STEPS; step++) {
            if (hasResultSet) {
                for (final Object item : query.getResultList()) {
                    if (item instanceof Object[] columns && columns.length == CANONICAL_COLUMNS) {
                        rows.add(columns);
                    }
                }
            } else if (query.getUpdateCount() == -1) {
                break;
            }
            hasResultSet = query.hasMoreResults();
        }
        return rows;
    }

    /**
     * Serializa exactamente {@code [{"idEstudiante":"<UUID>","estado":"<valor exacto>"}]}, en el orden del
     * DTO y con orden de claves fijo. Sin normalizar ni validar el estado; un valor nulo falla igual que en
     * el baseline (serializacion), no se sustituye.
     */
    private static String serializarRegistros(final RegistrarAsistenciasSesionRepositoryDTO dto) {
        try {
            final List<Map<String, String>> registros = new ArrayList<>();
            for (final var registro : dto.registros()) {
                final Map<String, String> item = new LinkedHashMap<>();
                item.put("idEstudiante", registro.estudiante().toString());
                item.put("estado", Objects.requireNonNull(registro.estado()));
                registros.add(item);
            }
            return OBJECT_MAPPER.writeValueAsString(registros);
        } catch (RuntimeException exception) {
            throw new CrosscuttingException("No fue posible serializar los registros de asistencia.", exception);
        }
    }
}
