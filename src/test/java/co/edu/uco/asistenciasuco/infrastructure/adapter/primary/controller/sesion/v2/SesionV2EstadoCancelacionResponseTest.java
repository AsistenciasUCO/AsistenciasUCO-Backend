package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.SesionV2ConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.SesionProcedenciaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.common.mapper.SesionProcedenciaMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CancelacionSesionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** CULM-001 B.1 (CONTRACT 3.2.4): {@code SesionV2} lleva {@code estado} y {@code cancelacion} desde uv_sesion_estado. */
class SesionV2EstadoCancelacionResponseTest {

    private static final UUID SESION = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    @Test
    void una_sesion_cancelada_por_festivo_viaja_con_estado_y_cancelacion_utc() {
        final SesionProcedenciaEntity entidad = SesionProcedenciaMapper.toEntity(new SesionProcedenciaRepositoryProjection(
                SESION, GRUPO, "Sesion 1", 1, "S-1", 1, "1", "G01", LocalDateTime.of(2026, 3, 23, 13, 0),
                LocalDateTime.of(2026, 3, 23, 15, 0), "UTC_V2",
                new CancelacionSesionProjection("FESTIVO", "Día festivo: San José", "San José", LocalDateTime.of(2026, 1, 2, 5, 0))));
        assertEquals(EstadoSesion.CANCELADA, entidad.estado());

        final SesionV2Response respuesta = SesionV2ConsultaController.toResponse(SesionProcedenciaMapper.toDTO(entidad));

        assertEquals("CANCELADA", respuesta.estado());
        assertEquals("FESTIVO", respuesta.cancelacion().tipo());
        assertEquals("Día festivo: San José", respuesta.cancelacion().motivo());
        assertEquals("San José", respuesta.cancelacion().nombreFestivo());
        assertEquals("2026-01-02T05:00:00Z", respuesta.cancelacion().fechaCancelacionUtc());
        assertEquals("2026-03-23T13:00:00Z", respuesta.fechaHoraInicio());
    }

    @Test
    void una_sesion_programada_viaja_con_cancelacion_nula() {
        final SesionV2Response respuesta = SesionV2ConsultaController.toResponse(new SesionV2ConsultadaDTO(
                SESION, GRUPO, "Sesion 1", 1, "S-1", 1, "1", "G01", null, null, "INDETERMINADA", null));
        assertEquals("PROGRAMADA", respuesta.estado());
        assertNull(respuesta.cancelacion());

        final SesionProcedenciaEntity programada = SesionProcedenciaEntity.desdeAlmacenamiento(SESION, GRUPO, "Sesion 1", 1,
                "S-1", 1, "1", "G01", null, null, null);
        assertEquals(EstadoSesion.PROGRAMADA, programada.estado());
        assertNull(CancelacionSesionDTO.desde(null));
        assertNull(new CancelacionSesionDTO("DOCENTE", "m", null, null).fechaCancelacionUtc());
        assertNull(CancelacionSesionDTO.desde(new CancelacionSesionProjection("DOCENTE", "m", null, null)).fechaCancelacionUtc());
    }
}
