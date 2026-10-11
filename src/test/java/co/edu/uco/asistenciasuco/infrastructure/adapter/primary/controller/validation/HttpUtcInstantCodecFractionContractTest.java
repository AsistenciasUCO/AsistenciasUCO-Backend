package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** MAINT-003E: D02 .5Z is VALID; no truncation or rounding beyond datetime2(7). */
class HttpUtcInstantCodecFractionContractTest {
    @Test
    void oneDecimalIsValidNotAnInvalidFormat() {
        assertEquals(LocalDateTime.of(2026, 7, 15, 14, 0, 0, 500_000_000),
                HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T14:00:00.5Z"));
    }
    @Test
    void oneHundredNanosecondsFitsSqlServerDatetime2Seven() {
        assertEquals(LocalDateTime.of(2026, 7, 15, 14, 0, 0, 100),
                HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T14:00:00.0000001Z"));
    }
    @Test
    void eightAndNineDigitsNeverRoundIntoDatetime2Seven() {
        assertThrows(IllegalArgumentException.class, () ->
                HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T14:00:00.12345678Z"));
        assertThrows(IllegalArgumentException.class, () ->
                HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T14:00:00.123456789Z"));
    }
}