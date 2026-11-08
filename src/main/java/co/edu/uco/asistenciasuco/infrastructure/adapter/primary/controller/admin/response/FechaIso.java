package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Formato de instantes y numeros de las respuestas de parametros: UTC con exactamente tres decimales de segundo. */
public final class FechaIso {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private FechaIso() {
    }

    public static String formatear(final Instant instante) {
        return instante == null ? null : FORMATO.format(instante);
    }

    /** Numero JSON sin ceros sobrantes: {@code 60.0000} -> {@code 60}; {@code 1.5000} -> {@code 1.5}. */
    public static Number numero(final BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        final BigDecimal limpio = valor.stripTrailingZeros();
        return limpio.scale() <= 0 ? (Number) limpio.longValue() : (Number) limpio.doubleValue();
    }
}
