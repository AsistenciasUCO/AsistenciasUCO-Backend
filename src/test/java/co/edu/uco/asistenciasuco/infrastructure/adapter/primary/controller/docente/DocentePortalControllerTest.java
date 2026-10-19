package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.ResolverSolicitudRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.dto.ResolverSolicitudRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.ConsultarAsignaturasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.dto.AsignaturaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.ConsultarHorariosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.ConsultarReclamosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ConsultarReclamosDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ReclamosDocentePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.dto.HorarioDocenteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.request.ResolverReclamoRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocentePortalControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID RECLAMO = UUID.randomUUID();

    private final ConsultarHorariosDocenteInputPort horarios = mock(ConsultarHorariosDocenteInputPort.class);
    private final ConsultarAsignaturasDocenteInputPort asignaturas = mock(ConsultarAsignaturasDocenteInputPort.class);
    private final ResolverSolicitudRevisionAsistenciaInputPort resolverReclamo =
            mock(ResolverSolicitudRevisionAsistenciaInputPort.class);
    private final ConsultarReclamosDocenteInputPort reclamos = mock(ConsultarReclamosDocenteInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final DocentePortalController controller =
            new DocentePortalController(horarios, asignaturas, resolverReclamo, reclamos, identity);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    @Test
    void consultarReclamos_delega_con_el_usuario_autenticado_y_devuelve_lista_con_total_y_soporte() throws Exception {
        final UUID soporte = UUID.randomUUID();
        final ReclamoDocenteDTO reclamo = new ReclamoDocenteDTO(RECLAMO, "REC-1", java.time.LocalDate.of(2026, 10, 19), "PENDIENTE",
                "Justificacion", null, UUID.randomUUID(), "Ana Perez", UUID.randomUUID(), 3,
                java.time.Instant.parse("2026-10-19T15:30:45.1234567Z"), "UTC_V2", UUID.randomUUID(), 1, "G1",
                UUID.randomUUID(), "MAT1", "Matematicas",
                new ReclamoDocenteDTO.SoporteAdjuntoDTO(soporte, "evidencia.pdf", "application/pdf", 1234L,
                        java.time.Instant.parse("2026-10-19T15:00:00Z"), "/api/v1/archivos/" + soporte));
        when(reclamos.execute(new ConsultarReclamosDocenteDTO(ACTOR, "P", 2, 5)))
                .thenReturn(new ReclamosDocentePaginaDTO(List.of(reclamo), 11));

        mvc.perform(get("/api/v1/docente/reclamos").param("estado", "P").param("pagina", "2").param("tamano", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(11))
                .andExpect(jsonPath("$.datos[0].estadoSolicitud").value("PENDIENTE"))
                .andExpect(jsonPath("$.datos[0].fechaSesionUtc").value("2026-10-19T15:30:45.1234567Z"))
                .andExpect(jsonPath("$.datos[0].soporteAdjunto.fechaSubidaUtc").value("2026-10-19T15:00:00Z"))
                .andExpect(jsonPath("$.datos[0].soporteAdjunto.fileId").value(soporte.toString()))
                .andExpect(jsonPath("$.datos[0].soporteAdjunto.url").value("/api/v1/archivos/" + soporte));
    }

    @Test
    void consultarReclamos_sin_parametros_usa_valores_nulos_y_deja_la_normalizacion_a_application() throws Exception {
        when(reclamos.execute(new ConsultarReclamosDocenteDTO(ACTOR, null, null, null)))
                .thenReturn(new ReclamosDocentePaginaDTO(List.of(), 0));

        mvc.perform(get("/api/v1/docente/reclamos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.datos").isEmpty());
    }

    @Test
    void resolverReclamo_mapea_cuerpo_de_la_peticion() throws Exception {
        final String body = "{\"accion\":\"APROBAR\",\"respuestaDocente\":\"Aceptada\"}";

        mvc.perform(patch("/api/v1/docente/reclamos/{id}", RECLAMO).contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true));

        final ArgumentCaptor<ResolverSolicitudRevisionAsistenciaDTO> captor =
                ArgumentCaptor.forClass(ResolverSolicitudRevisionAsistenciaDTO.class);
        verify(resolverReclamo).execute(captor.capture());
        assertEquals(RECLAMO, captor.getValue().getSolicitud());
        assertEquals("APROBAR", captor.getValue().getAccion());
        assertEquals("Aceptada", captor.getValue().getRespuestaDocente());
        assertEquals(ACTOR, captor.getValue().getUsuario());
    }

    @Test
    void resolverReclamo_con_peticion_nula_mapea_campos_nulos() {
        controller.resolverReclamo(RECLAMO, null);

        final ArgumentCaptor<ResolverSolicitudRevisionAsistenciaDTO> captor =
                ArgumentCaptor.forClass(ResolverSolicitudRevisionAsistenciaDTO.class);
        verify(resolverReclamo).execute(captor.capture());
        assertNull(captor.getValue().getAccion());
        assertEquals(ACTOR, captor.getValue().getUsuario());
    }

    @Test
    void consultarHorarios_usa_actor_autenticado() throws Exception {
        when(horarios.execute(ACTOR)).thenReturn(List.of(new HorarioDocenteDTO(
                UUID.randomUUID(), ACTOR, UUID.randomUUID(), "MAT-01", "Calculo", "G1", "LUNES",
                LocalTime.of(8, 0), LocalTime.of(10, 0), 25)));

        mvc.perform(get("/api/v1/docente/horarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].nombreMateria").value("Calculo"));
        verify(horarios).execute(ACTOR);
    }

    @Test
    void cancelarSesion_no_implementado_lanza_featureUnavailable() {
        assertEquals("FEATURE_UNAVAILABLE",
                assertThrows(FeatureUnavailableException.class, () -> controller.cancelarSesion(RECLAMO)).getCode());
    }

    @Test
    void consultarAsignaturas_usa_actor_autenticado() throws Exception {
        when(asignaturas.execute(ACTOR)).thenReturn(List.of(new AsignaturaDocenteDTO(
                UUID.randomUUID(), "Calculo", UUID.randomUUID(), "Grupo 1", UUID.randomUUID(), "Ingenieria")));

        mvc.perform(get("/api/v1/docente/asignaturas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].nombreAsignatura").value("Calculo"));
        verify(asignaturas).execute(ACTOR);
    }

    @Test
    void resolverReclamoRequest_usa_respuesta_como_fallback_de_respuestaDocente() {
        final ResolverReclamoRequest request = new ResolverReclamoRequest();
        request.setRespuesta("Fallback");

        assertEquals("Fallback", request.getRespuestaDocente());

        request.setRespuestaDocente("Directa");
        assertEquals("Directa", request.getRespuestaDocente());
    }
}
