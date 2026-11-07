package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.SesionGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.SesionFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.SesionMateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.mapper.ConsultarSesionMapper;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.mapper.ConsultarSesionRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CancelacionSesionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.SesionGrupoCoordinadorResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano.SesionFacultadDecanoResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.SesionMateriaEstudianteResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.CancelacionSesionResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CULM-001 B.1 (CONTRACT 3.2.4): todas las respuestas de sesion exponen {@code estado} y {@code cancelacion}; la del
 * estudiante deja de exponer el {@code codigo} de la sesion.
 */
class SesionEstadoCancelacionWireTest {

    private static final CancelacionSesionDTO CANCELACION =
            new CancelacionSesionDTO("DOCENTE", "Calamidad domestica", null, Instant.parse("2026-11-07T14:00:00Z"));
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 10, 13, 0);

    @Test
    void la_respuesta_del_estudiante_no_expone_el_codigo_y_si_el_estado() {
        final SesionMateriaEstudianteResponse respuesta = SesionMateriaEstudianteResponse.from(new SesionMateriaEstudianteDTO(
                UUID.randomUUID(), "Sesion 1", 1, "SES-01", 1, UUID.randomUUID(), "1", "G01", INICIO, INICIO.plusHours(2),
                INICIO, INICIO.plusHours(2), "CONFIRMADA", "NO_DISPONIBLE", null, false, "CANCELADA", CANCELACION));

        assertFalse(Arrays.stream(SesionMateriaEstudianteResponse.class.getRecordComponents())
                .anyMatch(componente -> componente.getName().equals("codigo")));
        assertEquals("CANCELADA", respuesta.estado());
        assertEquals("2026-11-07T14:00:00Z", respuesta.cancelacion().fechaCancelacionUtc());

        final SesionMateriaEstudianteResponse programada = SesionMateriaEstudianteResponse.from(new SesionMateriaEstudianteDTO(
                UUID.randomUUID(), "Sesion 1", 1, "SES-01", 1, UUID.randomUUID(), "1", "G01", INICIO, INICIO.plusHours(2),
                null, null, "INDETERMINADA", "PRESENTE", null, false));
        assertEquals("PROGRAMADA", programada.estado());
        assertNull(programada.cancelacion());
    }

    @Test
    void las_respuestas_del_coordinador_y_del_decano_llevan_estado_y_cancelacion() {
        final SesionGrupoCoordinadorResponse coordinador = SesionGrupoCoordinadorResponse.from(new SesionGrupoDTO(
                UUID.randomUUID(), UUID.randomUUID(), 1, "G01", "Sesion 1", 1, "S-01", 1, INICIO, INICIO.plusHours(2),
                INICIO, INICIO.plusHours(2), "CONFIRMADA", "CANCELADA", CANCELACION));
        assertEquals("CANCELADA", coordinador.estado());
        assertEquals("DOCENTE", coordinador.cancelacion().tipo());
        assertEquals("PROGRAMADA", SesionGrupoCoordinadorResponse.from(new SesionGrupoDTO(UUID.randomUUID(), UUID.randomUUID(),
                1, "G01", "Sesion 1", 1, "S-01", 1, INICIO, INICIO.plusHours(2), null, null, "INDETERMINADA")).estado());

        final SesionFacultadDecanoResponse decano = SesionFacultadDecanoResponse.from(new SesionFacultadDTO(UUID.randomUUID(),
                "Sesion 1", "S-01", 1, INICIO, INICIO.plusHours(2), UUID.randomUUID(), 1, "G01", UUID.randomUUID(), "Calculo",
                UUID.randomUUID(), "Sistemas", INICIO, INICIO.plusHours(2), "CONFIRMADA", "CANCELADA", CANCELACION));
        assertEquals("CANCELADA", decano.estado());
        assertEquals("Calamidad domestica", decano.cancelacion().motivo());
        final SesionFacultadDTO compatible = new SesionFacultadDTO(UUID.randomUUID(), "Sesion 1", "S-01", 1, INICIO,
                INICIO.plusHours(2), UUID.randomUUID(), 1, "G01", UUID.randomUUID(), "Calculo", UUID.randomUUID(), "Sistemas",
                null, null, "INDETERMINADA");
        assertEquals("PROGRAMADA", compatible.estado());
        assertNull(SesionFacultadDecanoResponse.from(compatible).cancelacion());
    }

    @Test
    void la_sesion_v1_lleva_estado_y_cancelacion_desde_la_proyeccion() {
        final SesionConsultadaDTO cancelada = ConsultarSesionMapper.toDTO(ConsultarSesionRepositoryMapper.toUseCaseEntity(
                new SesionRepositoryProjection(UUID.randomUUID(), UUID.randomUUID(), "Sesion 1", 1, "S-01", 1, "1", "G01",
                        INICIO, INICIO.plusHours(2),
                        new CancelacionSesionProjection("FESTIVO", "Día festivo: Navidad", "Navidad", LocalDateTime.of(2026, 1, 2, 5, 0)))));
        assertEquals("CANCELADA", cancelada.getEstado());
        assertEquals("Navidad", cancelada.getCancelacion().nombreFestivo());
        assertEquals("S-01", cancelada.getCodigo());

        final SesionConsultadaDTO programada = ConsultarSesionMapper.toDTO(ConsultarSesionRepositoryMapper.toUseCaseEntity(
                new SesionRepositoryProjection(UUID.randomUUID(), UUID.randomUUID(), "Sesion 1", 1, "S-01", 1, "1", "G01",
                        INICIO, INICIO.plusHours(2))));
        assertEquals("PROGRAMADA", programada.getEstado());
        assertNull(programada.getCancelacion());
        assertEquals("PROGRAMADA", new SesionConsultadaDTO().getEstado());
    }

    @Test
    void las_respuestas_comunes_de_cancelacion_y_paginacion() {
        assertNull(CancelacionSesionResponse.from(null));
        assertNull(CancelacionSesionResponse.from(new CancelacionSesionDTO("DOCENTE", "m", null, null)).fechaCancelacionUtc());
        final ApiPageResponse<String> pagina = ApiPageResponse.of(List.of("a"), 1, 20, 41);
        assertTrue(pagina.exitoso());
        assertEquals(3, pagina.totalPaginas());
        assertEquals(0, ApiPageResponse.of(List.of(), 1, 0, 5).totalPaginas());
        assertEquals(0, ApiPageResponse.of(null, 1, 20, 0).totalPaginas());
        assertTrue(ApiPageResponse.of(null, 1, 20, 0).elementos().isEmpty());
    }
}
