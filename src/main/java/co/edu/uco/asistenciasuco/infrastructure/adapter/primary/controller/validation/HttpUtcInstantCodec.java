package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Inactive adapter-side UTC codec for the future approved v2 session contract.
 * DATETIME2(7) stores UTC clock values without an offset. Do NOT wire into v1.
 */
public final class HttpUtcInstantCodec {
    // Separate the clock and offset to keep each regex simple while preserving strict RFC3339 input.
    private static final Pattern STRICT_DATE_TIME = Pattern.compile(
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,7})?");
    private static final Pattern STRICT_OFFSET = Pattern.compile("Z|[+-]\\d{2}:\\d{2}");
    private static final LocalDateTime MIN_SQL = LocalDateTime.of(1, 1, 1, 0, 0);
    private static final LocalDateTime MAX_SQL = LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999999900);

    private HttpUtcInstantCodec() { }

    public static LocalDateTime toUtcDatabaseDateTime(final String text) {
        validateFormat(text);
        try {
            final LocalDateTime utc = OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            validateDatabasePrecision(utc);
            return utc;
        } catch (java.time.DateTimeException _) {
            throw invalidFormat();
        }
    }

    public static String fromUtcDatabaseDateTime(final LocalDateTime utcValue) {
        Objects.requireNonNull(utcValue, "UTC database value must not be null");
        validateDatabasePrecision(utcValue);
        return utcValue.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private static void validateFormat(final String text) {
        // The shortest valid value has 19 clock characters plus "Z".
        if (text == null || text.length() < 20 || text.endsWith("-00:00")) {
            throw invalidFormat();
        }
        final int offsetLength = text.endsWith("Z") ? 1 : 6;
        final int offsetStart = text.length() - offsetLength;
        if (!STRICT_DATE_TIME.matcher(text.substring(0, offsetStart)).matches()
                || !STRICT_OFFSET.matcher(text.substring(offsetStart)).matches()) {
            throw invalidFormat();
        }
    }

    private static void validateDatabasePrecision(final LocalDateTime utcValue) {
        if (utcValue.isBefore(MIN_SQL) || utcValue.isAfter(MAX_SQL)
                || utcValue.getNano() % 100 != 0) {
            throw new IllegalArgumentException("UTC value cannot be represented by SQL DATETIME2(7).");
        }
    }

    private static IllegalArgumentException invalidFormat() {
        return new IllegalArgumentException("ISO date-time with seconds, max 7 fractional digits and explicit offset required.");
    }
}
