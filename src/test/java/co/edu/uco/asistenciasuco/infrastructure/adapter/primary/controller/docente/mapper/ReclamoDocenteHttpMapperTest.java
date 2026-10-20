package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.response.ReclamoDocenteResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** UTC v2: precision de siete decimales y sufijo Z, independiente de la zona de la JVM. */
class ReclamoDocenteHttpMapperTest {

    private final TimeZone original = TimeZone.getDefault();

    @AfterEach
    void restaurarZona() {
        TimeZone.setDefault(original);
    }

    private static ReclamoDocenteDTO dto(final Instant sesion, final Instant subida, final boolean conSoporte) {
        return new ReclamoDocenteDTO(UUID.randomUUID(), "REC-1", LocalDate.of(2026, 10, 19), "PENDIENTE", "J", null,
                UUID.randomUUID(), "Ana", UUID.randomUUID(), 1, sesion, sesion == null ? null : "UTC_V2",
                UUID.randomUUID(), 1, "G1", UUID.randomUUID(), "M", "Mate",
                conSoporte ? new ReclamoDocenteDTO.SoporteAdjuntoDTO(UUID.randomUUID(), "a.pdf", "application/pdf", 5L, subida, "/u") : null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"America/Bogota", "Europe/London", "Europe/Berlin", "Asia/Kolkata", "UTC"})
    void siete_decimales_y_z_sin_importar_la_zona_del_host(final String zona) {
        TimeZone.setDefault(TimeZone.getTimeZone(zona));

        final ReclamoDocenteResponse response = ReclamoDocenteHttpMapper.toResponse(
                dto(Instant.parse("2026-10-19T15:30:45.1234567Z"), Instant.parse("2026-03-29T01:30:00.0000001Z"), true));

        assertEquals("2026-10-19T15:30:45.1234567Z", response.fechaSesionUtc());
        assertEquals("2026-03-29T01:30:00.0000001Z", response.soporteAdjunto().fechaSubidaUtc());
    }

    @Test
    void cruce_de_medianoche_y_segundos_exactos_no_agregan_decimales() {
        final ReclamoDocenteResponse response = ReclamoDocenteHttpMapper.toResponse(
                dto(Instant.parse("2026-12-31T23:59:59Z"), null, false));

        assertEquals("2026-12-31T23:59:59Z", response.fechaSesionUtc());
        assertNull(response.soporteAdjunto());
    }

    @Test
    void instante_nulo_se_conserva_nulo() {
        assertNull(ReclamoDocenteHttpMapper.toResponse(dto(null, null, false)).fechaSesionUtc());
    }
}
