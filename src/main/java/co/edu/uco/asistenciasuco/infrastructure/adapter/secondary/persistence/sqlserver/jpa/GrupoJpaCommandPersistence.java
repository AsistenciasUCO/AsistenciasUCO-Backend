package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarEstudianteRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.RegistrarEstudianteRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import co.edu.uco.asistenciasuco.crosscutting.util.TextHelper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DbExceptionTranslator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.mapping.JdbcValueMapper;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure.CanonicalProcedureResult;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.StoredProcedureQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistencia JPA de commands de Grupo sobre el Stored Procedure canonico
 * {@code dbo.usp_registrar_estudiante_en_grupo} (11 parametros).
 */
public final class GrupoJpaCommandPersistence {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrupoJpaCommandPersistence.class);

    private static final String SP_REGISTRAR_ESTUDIANTE = "dbo.usp_registrar_estudiante_en_grupo";
    private static final String OPERATION_REGISTRAR_ESTUDIANTE = "registrarEstudianteEnGrupo";

    private static final String PARAM_ID_GRUPO = "idGrupo";
    private static final String PARAM_NUMERO_IDENTIFICACION = "numeroIdentificacion";
    private static final String PARAM_PRIMER_NOMBRE = "primerNombre";
    private static final String PARAM_SEGUNDO_NOMBRE = "segundoNombre";
    private static final String PARAM_PRIMER_APELLIDO = "primerApellido";
    private static final String PARAM_SEGUNDO_APELLIDO = "segundoApellido";
    private static final String PARAM_CORREO = "correo";
    private static final String PARAM_PASSWORD = "password";
    private static final String PARAM_ID_CORRELACION = "idCorrelacion";
    private static final String PARAM_ID_USUARIO_EJECUTOR = "idUsuarioEjecutor";
    private static final String PARAM_ID_TIPO_ID_IDENTIFICACION = "idTipoIdIdentificacion";

    private final EntityManagerFactory entityManagerFactory;

    public GrupoJpaCommandPersistence(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = Objects.requireNonNull(
                entityManagerFactory,
                "El EntityManagerFactory para commands de grupo es obligatorio."
        );
    }

    public RegistrarEstudianteRepositoryProjection registrarEstudianteEnGrupo(final RegistrarEstudianteRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para registrar estudiante en grupo es obligatorio.");
        }

        final UUID correlationId = CorrelationIdContext.require();

        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            final StoredProcedureQuery query = entityManager.createStoredProcedureQuery(SP_REGISTRAR_ESTUDIANTE);

            query.registerStoredProcedureParameter(PARAM_ID_GRUPO, UUID.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_NUMERO_IDENTIFICACION, Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_PRIMER_NOMBRE, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_SEGUNDO_NOMBRE, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_PRIMER_APELLIDO, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_SEGUNDO_APELLIDO, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_CORREO, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_PASSWORD, String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_ID_CORRELACION, UUID.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_ID_USUARIO_EJECUTOR, UUID.class, ParameterMode.IN);
            query.registerStoredProcedureParameter(PARAM_ID_TIPO_ID_IDENTIFICACION, UUID.class, ParameterMode.IN);

            query.setParameter(PARAM_ID_GRUPO, dto.getGrupoId());
            query.setParameter(PARAM_NUMERO_IDENTIFICACION, dto.getNumeroIdentificacion());
            query.setParameter(PARAM_PRIMER_NOMBRE, dto.getPrimerNombre());
            query.setParameter(PARAM_SEGUNDO_NOMBRE, dto.getSegundoNombre());
            query.setParameter(PARAM_PRIMER_APELLIDO, dto.getPrimerApellido());
            query.setParameter(PARAM_SEGUNDO_APELLIDO, dto.getSegundoApellido());
            query.setParameter(PARAM_CORREO, dto.getCorreo());
            query.setParameter(PARAM_PASSWORD, dto.getPassword());
            query.setParameter(PARAM_ID_CORRELACION, correlationId);
            query.setParameter(PARAM_ID_USUARIO_EJECUTOR, dto.getUsuarioEjecutor());
            query.setParameter(PARAM_ID_TIPO_ID_IDENTIFICACION, dto.getTipoIdentificacionId());

            final List<?> results = query.getResultList();
            if (results.size() != 1) {
                throw new DatabaseOperationException(
                        DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                        "El procedimiento no retorno el contrato canonico esperado."
                );
            }

            final Object resultRow = results.getFirst();
            final CanonicalProcedureResult canonicalResult = parseCanonicalResult(resultRow);
            validateCorrelationId(canonicalResult, correlationId);

            DbExceptionTranslator.throwIfFailed(
                    canonicalResult.isEstadoResultado(),
                    canonicalResult.getMensajeUsuarioResultado(),
                    canonicalResult.getMensajeTecnicoResultado(),
                    correlationId.toString(),
                    OPERATION_REGISTRAR_ESTUDIANTE
            );

            return new RegistrarEstudianteRepositoryProjection(canonicalResult.getMensajeUsuarioResultado());
        } catch (PersistenceException | IllegalStateException | IllegalArgumentException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    OPERATION_REGISTRAR_ESTUDIANTE,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException("No fue posible registrar el estudiante en el grupo.", exception);
        }
    }

    private CanonicalProcedureResult parseCanonicalResult(final Object resultRow) {
        if (resultRow instanceof Object[] columns) {
            if (columns.length < 4) {
                throw new DatabaseOperationException(
                        DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                        "El resultado canonico contiene menos de 4 columnas esperadas."
                );
            }
            return new CanonicalProcedureResult(
                    JdbcValueMapper.toUuid(columns[0]),
                    JdbcValueMapper.toString(columns[1]),
                    JdbcValueMapper.toString(columns[2]),
                    JdbcValueMapper.toBoolean(columns[3])
            );
        }
        throw new DatabaseOperationException(
                DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                "El formato de fila devuelto por el procedimiento no corresponde a un array de columnas."
        );
    }

    private void validateCorrelationId(final CanonicalProcedureResult result, final UUID contextCorrelationId) {
        final UUID resultCorrelationId = result.getIdCorrelacion();
        if (!Objects.equals(contextCorrelationId, resultCorrelationId)) {
            throw new DatabaseOperationException(
                    DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                    "La correlacion retornada por la DB no coincide con la peticion."
            );
        }
    }
}
