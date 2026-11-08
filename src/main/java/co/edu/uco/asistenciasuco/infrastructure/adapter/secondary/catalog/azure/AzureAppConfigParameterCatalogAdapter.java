package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.azure;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.data.appconfiguration.ConfigurationClient;
import com.azure.data.appconfiguration.models.ConfigurationSetting;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Adaptador secundario para Azure App Configuration.
 * Implementa {@link ParameterCatalogPort} utilizando el {@link ConfigurationClient} de Azure SDK.
 *
 * <p>Incorpora una cache acotada y con TTL en memoria (Caffeine) para optimizar
 * lecturas y respetar la cuota de peticiones del tier gratuito (F0).</p>
 *
 * <p>No se autoregistra con anotaciones de Spring (@Component/@Service). Es instanciado
 * y configurado exclusivamente por el Composition Root.</p>
 */
public class AzureAppConfigParameterCatalogAdapter implements ParameterCatalogPort,
        co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.InvalidableParameterCatalog {

    private final ConfigurationClient client;
    private final Cache<String, String> cache;

    public AzureAppConfigParameterCatalogAdapter(final ConfigurationClient client) {
        this(client, Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build());
    }

    public AzureAppConfigParameterCatalogAdapter(final ConfigurationClient client, final Cache<String, String> cache) {
        this.client = Objects.requireNonNull(client, "ConfigurationClient de Azure App Configuration es obligatorio.");
        this.cache = Objects.requireNonNull(cache, "Cache de Caffeine es obligatoria.");
    }

    @Override
    public Optional<String> getParameter(final String group, final String key) {
        if (group == null || group.isBlank() || key == null || key.isBlank()) {
            return Optional.empty();
        }

        final String normalizedKey = buildKey(group, key);

        // 1. Revisar cache en memoria
        final String cached = cache.getIfPresent(normalizedKey);
        if (cached != null) {
            return Optional.of(cached);
        }

        // 2. Consultar en Azure App Configuration si no esta en cache
        try {
            final ConfigurationSetting setting = client.getConfigurationSetting(normalizedKey, null);
            if (setting != null && setting.getValue() != null) {
                cache.put(normalizedKey, setting.getValue());
                return Optional.of(setting.getValue());
            }
            return Optional.empty();
        } catch (final ResourceNotFoundException e) {
            // Intentar con formato alternativo grupo/clave o clave simple si aplica
            return fallbackSearch(group, key, normalizedKey);
        } catch (final Exception e) {
            throw new ParameterCatalogException("Error al consultar el parametro [" + normalizedKey + "] en Azure App Configuration.", e);
        }
    }

    private Optional<String> fallbackSearch(final String group, final String key, final String originalNormalizedKey) {
        try {
            // Intento 2: formato plano "grupo:clave"
            final String colonKey = group + ":" + key;
            if (!colonKey.equals(originalNormalizedKey)) {
                final ConfigurationSetting setting = client.getConfigurationSetting(colonKey, null);
                if (setting != null && setting.getValue() != null) {
                    cache.put(originalNormalizedKey, setting.getValue());
                    return Optional.of(setting.getValue());
                }
            }
        } catch (final ResourceNotFoundException ignored) {
            // No existe
        }
        return Optional.empty();
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
        throw new IllegalArgumentException("Tipo destino no soportado para conversion directa: " + targetType.getName());
    }

    /**
     * Invalida la clave de un parámetro en la caché en memoria.
     */
    @Override
    public void invalidateParameter(final String group, final String key) {
        if (group != null && !group.isBlank() && key != null && !key.isBlank()) {
            cache.invalidate(buildKey(group, key));
        } else if (key != null && !key.isBlank()) {
            cache.invalidate(key.trim());
        }
    }

    /**
     * Invalida toda la cache local en memoria.
     */
    @Override
    public void invalidateAll() {
        cache.invalidateAll();
    }

    /**
     * Invalida la cache local en memoria (compatibilidad).
     */
    public void clearCache() {
        cache.invalidateAll();
    }

    private String buildKey(final String group, final String key) {
        // En Azure App Configuration la jerarquia comun usa delimitador ':' o '/'
        return group.trim() + ":" + key.trim();
    }
}
