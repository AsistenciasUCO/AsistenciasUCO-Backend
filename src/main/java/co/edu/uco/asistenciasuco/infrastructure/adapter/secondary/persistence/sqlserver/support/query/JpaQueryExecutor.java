package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query;

import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.error.DatabaseOperationException;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Manejo tecnico comun de las queries JPA de los {@code @Repository} (LB-008 JPA-only).
 *
 * <p>Centraliza el {@code try/catch} + logging que cada repository repetia alrededor de su query. No conoce
 * JPQL, entities ni projections: el HQL, los parametros, el mapping y la paginacion siguen en el repository.
 * Solo traduce fallos tecnicos de persistencia; no captura {@code RuntimeException} generica para no convertir
 * errores funcionales o de dominio en errores tecnicos.</p>
 */
public final class JpaQueryExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaQueryExecutor.class);

    private JpaQueryExecutor() {
    }

    /**
     * Ejecuta {@code query} y traduce los fallos tecnicos de JPA/Hibernate a {@link DatabaseOperationException}.
     *
     * @param operation    nombre de la operacion para logging
     * @param errorMessage mensaje tecnico de la excepcion resultante
     * @param query        ejecucion de la query y su mapping
     */
    public static <T> T execute(final String operation, final String errorMessage, final Supplier<T> query) {
        try {
            return query.get();
        } catch (PersistenceException | IllegalStateException | IllegalArgumentException | ArithmeticException exception) {
            LOGGER.error(
                    "SQL operation failed. operation={}, correlationId={}",
                    operation,
                    CorrelationIdContext.getAsString()
            );
            throw new DatabaseOperationException(errorMessage, exception);
        }
    }

    /**
     * CULM-001 A.2-9: como {@link #execute(String, String, Supplier)}, pero una {@link PersistenceException} pasa antes por
     * {@code traductor} (p. ej. {@code DbCodeTranslator::traducir}): si devuelve otra excepcion (403, 404, 400...) se lanza
     * esa; si la devuelve sin traducir, se trata como fallo tecnico.
     */
    public static <T> T execute(
            final String operation,
            final String errorMessage,
            final Supplier<T> query,
            final Function<PersistenceException, RuntimeException> traductor
    ) {
        try {
            return execute(operation, errorMessage, query);
        } catch (final DatabaseOperationException exception) {
            if (exception.getCause() instanceof PersistenceException persistence) {
                final RuntimeException traducida = traductor.apply(persistence);
                if (traducida != null && traducida != persistence) {
                    throw traducida;
                }
            }
            throw exception;
        }
    }
}
