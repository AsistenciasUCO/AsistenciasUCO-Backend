package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.SesionV2ConsultadaDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SesionV2ConsultaControllerTest {

    @Test
    void get_v2_serializa_instantes_confirmados_en_utc_y_conserva_procedencia() {
        final SesionV2Response response = SesionV2ConsultaController.toResponse(dto(
                LocalDateTime.parse("2042-07-15T14:00:00.123456700"),
                LocalDateTime.parse("2042-07-15T15:30:00.765432100"),
                "CONFIRMADA", "UTC_V2"
        ));

        assertEquals("2042-07-15T14:00:00.1234567Z", response.fechaHoraInicio());
        assertEquals("2042-07-15T15:30:00.7654321Z", response.fechaHoraFin());
        assertEquals("UTC_V2", response.procedenciaTemporal());
    }

    @Test
    void get_v2_no_infiere_horas_historicas_indeterminadas() {
        final SesionV2Response response = SesionV2ConsultaController.toResponse(dto(
                null, null, "INDETERMINADA", null
        ));

        assertNull(response.fechaHoraInicio());
        assertNull(response.fechaHoraFin());
        assertNull(response.procedenciaTemporal());
        assertEquals("INDETERMINADA", response.estadoTemporal());
    }

    private static SesionV2ConsultadaDTO dto(
            final LocalDateTime inicio, final LocalDateTime fin, final String estado, final String procedencia
    ) {
        return new SesionV2ConsultadaDTO(
                UUID.randomUUID(), UUID.randomUUID(), "Sesion", 1, "SES-01", 1,
                "1", "Grupo", inicio, fin, estado, procedencia
        );
    }
}
