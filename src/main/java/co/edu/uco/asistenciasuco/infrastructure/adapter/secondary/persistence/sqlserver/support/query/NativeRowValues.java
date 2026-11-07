package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.support.query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Conversiones de columnas de una fila {@code Object[]} devuelta por una consulta nativa (EXEC de un SP).
 *
 * <p>El driver de SQL Server entrega {@code UNIQUEIDENTIFIER} como {@link String} (no como {@link UUID}) cuando el
 * resultado no pasa por un mapeo de entidad: un cast directo a {@code UUID} lanza {@link ClassCastException}.</p>
 */
public final class NativeRowValues {

    private NativeRowValues() {
    }

    /** Identificador de la columna: acepta {@link UUID} o su texto (con o sin mayusculas); {@code null} se conserva. */
    public static UUID uuid(final Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof UUID identificador) {
            return identificador;
        }
        if (valor instanceof CharSequence texto) {
            return UUID.fromString(texto.toString().strip());
        }
        throw new IllegalArgumentException("La columna no es un identificador UUID: " + valor.getClass().getName());
    }

    /** CULM-001 B.2: entero de una columna INT/SMALLINT/BIT; {@code null} vale 0. */
    public static int entero(final Object valor) {
        if (valor == null) {
            return 0;
        }
        if (valor instanceof Boolean bit) {
            return bit ? 1 : 0;
        }
        if (valor instanceof Number numero) {
            return numero.intValue();
        }
        return Integer.parseInt(valor.toString().strip());
    }

    /** CULM-001 B.2: columna BIT; {@code null} vale {@code false}. */
    public static boolean bit(final Object valor) {
        if (valor instanceof Boolean bit) {
            return bit;
        }
        if (valor instanceof Number numero) {
            return numero.intValue() != 0;
        }
        return valor != null && ("1".equals(valor.toString().strip()) || Boolean.parseBoolean(valor.toString().strip()));
    }

    /** CULM-001 B.2 (contrato 3.1): horas y porcentajes con 2 decimales y redondeo HALF_UP; {@code null} se conserva. */
    public static BigDecimal decimal2(final Object valor) {
        if (valor == null) {
            return null;
        }
        final BigDecimal numero = valor instanceof BigDecimal decimal ? decimal : new BigDecimal(valor.toString().strip());
        return numero.setScale(2, RoundingMode.HALF_UP);
    }

    /** Texto de la columna; {@code null} se conserva. */
    public static String texto(final Object valor) {
        return valor == null ? null : valor.toString();
    }

    /**
     * CULM-001 B.2: DATETIME2 guardado en UTC sin zona. Se interpreta siempre como UTC (un {@code java.sql.Timestamp}
     * conserva en sus campos locales el valor almacenado); {@code null} se conserva.
     */
    public static Instant instanteUtc(final Object valor) {
        return switch (valor) {
            case null -> null;
            case Instant instante -> instante;
            case LocalDateTime local -> local.toInstant(ZoneOffset.UTC);
            case OffsetDateTime offset -> offset.toInstant();
            case java.util.Date fecha -> LocalDateTime.ofInstant(fecha.toInstant(), ZoneId.systemDefault()).toInstant(ZoneOffset.UTC);
            default -> LocalDateTime.parse(valor.toString().strip().replace(' ', 'T')).toInstant(ZoneOffset.UTC);
        };
    }
}
