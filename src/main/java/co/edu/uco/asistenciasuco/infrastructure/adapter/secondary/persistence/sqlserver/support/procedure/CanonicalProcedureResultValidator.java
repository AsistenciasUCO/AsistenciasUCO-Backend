package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.procedure;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DbExceptionTranslator;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Valida el resultado canonico ya mapeado de un procedimiento almacenado publico con la MISMA semantica
 * que {@link CanonicalStoredProcedureExecutor} tras ejecutar la consulta, para las tecnologias que no
 * pasan por JDBC (LB-002.2): cardinalidad exacta 1, eco de {@code idCorrelacion} igual al de la peticion y
 * traduccion del fallo de negocio.
 *
 * <p>La clasificacion por {@code DBCODE} NO se reimplementa aqui: se delega exclusivamente en
 * {@link DbExceptionTranslator#throwIfFailed}, unica autoridad de clasificacion. El executor JDBC no se
 * modifica durante el piloto.</p>
 */
public final class CanonicalProcedureResultValidator {

    private CanonicalProcedureResultValidator() {
    }

    /**
     * @return el unico resultado canonico exitoso
     * @throws DatabaseOperationException {@code ERR_DB_CANONICAL_CONTRACT} si la cardinalidad no es 1 o la
     *                                    correlacion devuelta no coincide
     */
    public static CanonicalProcedureResult validate(
            final List<CanonicalProcedureResult> results,
            final UUID correlationId,
            final String operation
    ) {
        if (results.size() != 1) {
            throw new DatabaseOperationException(
                    DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                    "El procedimiento no retorno el contrato canonico esperado."
            );
        }
        final CanonicalProcedureResult result = results.getFirst();
        if (!Objects.equals(correlationId, result.getIdCorrelacion())) {
            throw new DatabaseOperationException(
                    DatabaseErrorCode.ERR_DB_CANONICAL_CONTRACT,
                    "La correlacion retornada por la DB no coincide con la peticion."
            );
        }
        DbExceptionTranslator.throwIfFailed(
                result.isEstadoResultado(),
                result.getMensajeUsuarioResultado(),
                result.getMensajeTecnicoResultado(),
                String.valueOf(correlationId),
                operation
        );
        return result;
    }
}
