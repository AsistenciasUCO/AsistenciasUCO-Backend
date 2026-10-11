package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MAINT-003D D02: additional RED acceptance tests against the real public codec.
 * Expected RED before implementation: ISO_OFFSET_DATE_TIME is more permissive than D02.
 * No tests are disabled and no source code is modified by this package.
 */
class HttpUtcInstantCodecD02ContractRedTest {

    @Test
    void rejectsOffsetsWithoutMinutesOrWithSeconds() {
        for (String value : new String[] {
                "2026-07-15T16:00:00+02",
                "2026-07-15T16:00:00+02:00:30",
                "2026-07-15T16:00:00+0200"
        }) {
            assertThrows(IllegalArgumentException.class,
                    () -> HttpUtcInstantCodec.toUtcDatabaseDateTime(value), value);
        }
    }

    @Test
    void rejectsMissingSecondsAndTooMuchFractionalPrecision() {
        for (String value : new String[] {
                "2026-07-15T16:00+02:00",
                "2026-07-15T16:00:00.12345678Z",
                "2026-07-15T16:00:00.123456789Z"
        }) {
            assertThrows(IllegalArgumentException.class,
                    () -> HttpUtcInstantCodec.toUtcDatabaseDateTime(value), value);
        }
    }

    @Test
    void doesNotSilentlyTrimInputAndValidatesRealCalendarDates() {
        for (String value : new String[] {
                " 2026-07-15T16:00:00+02:00",
                "2026-07-15T16:00:00+02:00 ",
                "2026-02-29T16:00:00Z",
                "2026-07-15T24:00:00Z"
        }) {
            assertThrows(IllegalArgumentException.class,
                    () -> HttpUtcInstantCodec.toUtcDatabaseDateTime(value), value);
        }
    }

    @Test
    void retainsExactlySevenDecimalPlacesWithoutRounding() {
        assertEquals(LocalDateTime.of(2026, 7, 15, 14, 0, 0, 123456700),
                HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T16:00:00.1234567+02:00"));
        assertEquals("2026-07-15T14:00:00.1234567Z",
                HttpUtcInstantCodec.fromUtcDatabaseDateTime(
                        LocalDateTime.of(2026, 7, 15, 14, 0, 0, 123456700)));
    }

    @Test
    void sameInstantFromBogotaAndBerlinIsIdentical() {
        final LocalDateTime bogota =
                HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T20:00:00-05:00");
        final LocalDateTime berlin =
                HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-16T03:00:00+02:00");
        assertEquals(LocalDateTime.of(2026, 7, 16, 1, 0), bogota);
        assertEquals(bogota, berlin);
    }
}
