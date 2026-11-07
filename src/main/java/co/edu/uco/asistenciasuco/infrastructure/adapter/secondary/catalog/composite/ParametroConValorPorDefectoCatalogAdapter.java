package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.composite;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * CULM-001 A.2-6 (DEC-018): decorador del proveedor de parametros configurado (Azure App Configuration o {@code sqlserver}).
 *
 * <p>Delega siempre en el proveedor real. Si el proveedor no tiene la clave o lanza {@link ParameterCatalogException}
 * (p. ej. 401 de Azure en el laboratorio), devuelve el valor por defecto de {@link ParametrosPorDefecto} y escribe un WARN
 * con el {@code correlationId}, sin el valor. Sin valor por defecto: un parametro opcional queda vacio (o propaga el
 * fallo del proveedor) y uno obligatorio lanza {@link ParameterNotFoundException}. Asi un catalogo caido no convierte
 * cada lectura en un 500.</p>
 */
public final class ParametroConValorPorDefectoCatalogAdapter implements ParameterCatalogPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametroConValorPorDefectoCatalogAdapter.class);

    private final ParameterCatalogPort delegado;

    public ParametroConValorPorDefectoCatalogAdapter(final ParameterCatalogPort delegado) {
        this.delegado = Objects.requireNonNull(delegado, "El proveedor de parametros delegado es obligatorio.");
    }

    @Override
    public Optional<String> getParameter(final String group, final String key) {
        final Optional<String> porDefecto = ParametrosPorDefecto.valor(group, key);
        try {
            final Optional<String> valor = Objects.requireNonNullElse(delegado.getParameter(group, key), Optional.empty());
            if (valor.isPresent()) {
                return valor;
            }
            porDefecto.ifPresent(ignorado -> avisar(group, key, "sin valor en el proveedor"));
            return porDefecto;
        } catch (final ParameterCatalogException exception) {
            if (porDefecto.isEmpty()) {
                throw exception;
            }
            avisar(group, key, exception.getClass().getSimpleName());
            return porDefecto;
        }
    }

    @Override
    public String getRequiredParameter(final String group, final String key) {
        return getParameter(group, key).orElseThrow(() -> new ParameterNotFoundException(group, key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getParameterAs(final String group, final String key, final Class<T> targetType) {
        final String valor = getRequiredParameter(group, key).strip();
        if (targetType == String.class) {
            return (T) valor;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return (T) Integer.valueOf(valor);
        }
        if (targetType == Long.class || targetType == long.class) {
            return (T) Long.valueOf(valor);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return (T) Boolean.valueOf(valor);
        }
        if (targetType == Double.class || targetType == double.class) {
            return (T) Double.valueOf(valor);
        }
        throw new IllegalArgumentException("Tipo destino no soportado para conversion directa: " + targetType.getName());
    }

    /** El proveedor real (para invalidaciones de cache que dependen de su tipo concreto). */
    public ParameterCatalogPort delegado() {
        return delegado;
    }

    private static void avisar(final String group, final String key, final String causa) {
        LOGGER.warn("Parametro resuelto con el valor por defecto en codigo (DEC-018). parametro={}:{}, causa={}, correlationId={}",
                group, key, causa, CorrelationIdContext.getAsString());
    }
}
