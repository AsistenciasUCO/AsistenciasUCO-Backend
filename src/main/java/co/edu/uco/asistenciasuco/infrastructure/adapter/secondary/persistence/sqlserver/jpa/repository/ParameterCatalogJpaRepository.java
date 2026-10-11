package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import jakarta.persistence.EntityManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import java.util.List;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador secundario para leer parametros desde SQL Server (vista uv_parametro, JPA-only desde LB-008 JPA-06).
 *
 * <p>Permite operar con los parametros institucionales persistidos en base de datos.
 * Incorpora cache en memoria para minimizar accesos repetitivos a la base de datos.
 * Solo se cachean valores positivos: parametros inexistentes o valor nulo no se cachean.</p>
 */
@Repository
@ConditionalOnProperty(prefix = "app.adapters.parameter-catalog", name = "provider", havingValue = "sqlserver")
public class ParameterCatalogJpaRepository implements ParameterCatalogPort {

    static final String HQL_PARAMETRO = "select p.valor from UvParametroEntity p where p.grupo = :grupo and p.clave = :clave";
    private final EntityManager entityManager;
    private final Map<ParameterKey, String> cache = new ConcurrentHashMap<>();

    /**
     * Structural key: concatenating group + "::" + key is ambiguous when either
     * identifier contains the separator (e.g., "a::b"/"c" vs "a"/"b::c").
     */
    private record ParameterKey(String group, String key) { }

    public ParameterCatalogJpaRepository(final EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "El EntityManager de parametros es obligatorio.");
    }

    @Override
    public Optional<String> getParameter(final String group, final String key) {
        if (group == null || group.isBlank() || key == null || key.isBlank()) {
            return Optional.empty();
        }

        final String normalizedGroup = group.trim();
        final String normalizedKey = key.trim();
        final ParameterKey cacheKey = new ParameterKey(normalizedGroup, normalizedKey);
        final String cached = cache.get(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }

        try {
            final Optional<String> valor = buscarParametro(normalizedGroup, normalizedKey);
            valor.ifPresent(contenido -> cache.put(cacheKey, contenido));
            return valor;
        } catch (final Exception e) {
            throw new ParameterCatalogException("Error al consultar parametro en base de datos [" + group + ":" + key + "].", e);
        }
    }

    @Override
    public String getRequiredParameter(final String group, final String key) {
        return getParameter(group, key)
                .orElseThrow(() -> new ParameterNotFoundException(group, key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getParameterAs(final String group, final String key, final Class<T> targetType) {
        final String rawValue = getRequiredParameter(group, key);
        if (targetType == String.class) {
            return (T) rawValue;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return (T) Integer.valueOf(rawValue.trim());
        }
        if (targetType == Long.class || targetType == long.class) {
            return (T) Long.valueOf(rawValue.trim());
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return (T) Boolean.valueOf(rawValue.trim());
        }
        if (targetType == Double.class || targetType == double.class) {
            return (T) Double.valueOf(rawValue.trim());
        }
        throw new IllegalArgumentException("Tipo no soportado: " + targetType.getName());
    }

    private Optional<String> buscarParametro(final String grupo, final String clave) {
        return primerContenido(entityManager.createQuery(HQL_PARAMETRO, String.class)
                .setParameter("grupo", grupo).setParameter("clave", clave).setMaxResults(1).getResultList());
    }

    private static Optional<String> primerContenido(final List<String> resultados) {
        return resultados.isEmpty() ? Optional.empty() : Optional.ofNullable(resultados.getFirst());
    }
}


