package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpUtcInstantCodecTest {

    @Test
    void convierte_el_instante_de_berlin_en_verano_a_utc() {
        assertEquals(LocalDateTime.of(2026, 7, 15, 14, 0),
                HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T16:00:00+02:00"));
    }

    @Test
    void convierte_el_mismo_instante_de_londres_en_verano_a_utc() {
        assertEquals(LocalDateTime.of(2026, 7, 15, 14, 0),
                HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T15:00:00+01:00"));
    }

    @Test
    void acepta_un_utc_explicitamente_identificado_con_z() {
        assertEquals(LocalDateTime.of(2026, 7, 15, 14, 0),
                HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T14:00:00Z"));
    }

    @Test
    void rechaza_localdatetime_sin_zona_no_infiere_bogota() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T09:00:00"));
    }

    @Test
    void rechaza_texto_invalido_o_vacio() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpUtcInstantCodec.toUtcDatabaseDateTime("cualquier valor"));
        assertThrows(IllegalArgumentException.class,
                () -> HttpUtcInstantCodec.toUtcDatabaseDateTime(" "));
        assertThrows(IllegalArgumentException.class,
                () -> HttpUtcInstantCodec.toUtcDatabaseDateTime(null));
    }

    @Test
    void serializa_dato_utc_de_sql_server_con_marcador_z() {
        assertEquals("2026-07-15T14:00:00Z",
                HttpUtcInstantCodec.fromUtcDatabaseDateTime(
                        LocalDateTime.of(2026, 7, 15, 14, 0)));
    }

    @Test
    void exige_offset_normalizado_segundos_y_precision_sql_datetime2_siete() {
        for (String invalid : new String[] {
                "2026-07-15T16:00:00+02", "2026-07-15T16:00:00+02:00:30",
                "2026-07-15T16:00+02:00", "2026-07-15T14:00:00.12345678Z",
                "2026-07-15T14:00:00.123456789Z",
                "2026-07-15T14:00:00-00:00", "2026-07-15T14:00:00",
                " 2026-07-15T14:00:00Z ", "2026-02-29T14:00:00Z",
                "0000-01-01T00:00:00Z", "0001-01-01T00:00:00+01:00"
        }) {
            assertThrows(IllegalArgumentException.class,
                    () -> HttpUtcInstantCodec.toUtcDatabaseDateTime(invalid), invalid);
        }
    }

    @Test
    void no_pierde_precision_al_convertir_o_serializar_datetime2_7() {
        final LocalDateTime exact = LocalDateTime.of(2026, 7, 15, 14, 0, 0, 123456700);
        assertEquals(exact, HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-07-15T16:00:00.1234567+02:00"));
        assertEquals("2026-07-15T14:00:00.1234567Z",
                HttpUtcInstantCodec.fromUtcDatabaseDateTime(exact));
        final LocalDateTime invalidPrecision = LocalDateTime.of(2026, 7, 15, 14, 0, 0, 123456789);
        assertThrows(IllegalArgumentException.class,
                () -> HttpUtcInstantCodec.fromUtcDatabaseDateTime(invalidPrecision));
    }

    @Test
    void offsets_distintos_representan_instantes_distintos_en_ambiguedad_dst() {
        // Repeated Berlin 02:30 occurs with both summer/winter offsets.
        assertEquals(LocalDateTime.of(2026, 10, 25, 0, 30),
                HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-10-25T02:30:00+02:00"));
        assertEquals(LocalDateTime.of(2026, 10, 25, 1, 30),
                HttpUtcInstantCodec.toUtcDatabaseDateTime("2026-10-25T02:30:00+01:00"));
    }
}
