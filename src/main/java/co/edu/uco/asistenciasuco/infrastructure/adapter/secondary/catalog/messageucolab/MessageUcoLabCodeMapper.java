package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.messageucolab;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Traduce el codigo canonico de Asistencias (ej. {@code VAL_001}) al codigo de MessageUcoLab ({@code MSG_VAL001}).
 *
 * <p>MessageUcoLab solo admite en la parte variable del codigo letras, numeros y espacios (patron
 * {@code ^[A-Za-z0-9 ]{1,10}$}, "sin guiones"): los codigos canonicos de Asistencias, que usan {@code _}, no son validos
 * tal cual. Se eliminan los caracteres no alfanumericos y se pasa a mayusculas (la pantalla de MessageUcoLab hace lo
 * mismo al guardar). Con los 78 codigos de usuario vigentes no hay colisiones al hacerlo.</p>
 *
 * <p>El limite es de 10 caracteres tras el prefijo. Un codigo mas largo solo se puede consultar si tiene alias
 * explicito; sin alias no se inventa un codigo truncado (podria colisionar con otro mensaje) y el codigo se considera
 * no resoluble en MessageUcoLab.</p>
 */
public final class MessageUcoLabCodeMapper {

    /** Limite de MessageUcoLab para la parte del codigo posterior a {@code MSG_} (guia de integracion, 6.4). */
    public static final int MAX_SUFFIX_LENGTH = 10;

    private final String prefix;
    private final Map<String, String> aliases;

    public MessageUcoLabCodeMapper(final String prefix, final Map<String, String> aliases) {
        this.prefix = prefix == null ? "" : prefix;
        this.aliases = aliases == null ? Map.of() : Map.copyOf(aliases);
    }

    public Optional<String> toRemoteCode(final String canonicalCode) {
        if (canonicalCode == null || canonicalCode.isBlank()) {
            return Optional.empty();
        }
        final String clean = canonicalCode.trim();
        final String suffix = normalize(aliases.getOrDefault(clean, clean));
        if (suffix.isEmpty() || suffix.length() > MAX_SUFFIX_LENGTH) {
            return Optional.empty();
        }
        return Optional.of(prefix + suffix);
    }

    /** Solo letras y digitos ASCII, en mayusculas: lo unico que MessageUcoLab acepta de forma estable. */
    private static String normalize(final String value) {
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }
}
