package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.ResolverSolicitudRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.dto.ResolverSolicitudRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.ConsultarAsignaturasDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.dto.AsignaturaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.ConsultarHorariosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.ConsultarDetalleReclamoDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto.ConsultarDetalleReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto.DetalleReclamoDocenteDTO;
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
    private final ConsultarDetalleReclamoDocenteInputPort detalleReclamo =
            mock(ConsultarDetalleReclamoDocenteInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final DocentePortalController controller =
            new DocentePortalController(horarios, asignaturas, resolverReclamo, reclamos, detalleReclamo, identity);
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

    // CT-01 (AC-05/06): 200 con ApiDataResponse, causal, soporte y UTC v2.
    @Test
    void consultarDetalleReclamo_devuelve_api_data_response_con_causal_y_soporte() throws Exception {
        final UUID soporte = UUID.randomUUID();
        final DetalleReclamoDocenteDTO detalle = new DetalleReclamoDocenteDTO(RECLAMO, "REV-1",
                java.time.LocalDate.of(2026, 10, 15), "P", "PENDIENTE", "pendiente", "Cita medica", "",
                UUID.randomUUID(), "Ana Perez", UUID.randomUUID(), 4,
                java.time.Instant.parse("2026-10-14T13:00:00.1234567Z"), "UTC_V2", UUID.randomUUID(), 1, "Grupo 1",
                UUID.randomUUID(), "MAT101", "Calculo I",
                new DetalleReclamoDocenteDTO.CausalInasistenciaDTO("SJC", "sin justa causa", Boolean.FALSE),
                new ReclamoDocenteDTO.SoporteAdjuntoDTO(soporte, "incapacidad.pdf", "application/pdf", 2048L,
                        java.time.Instant.parse("2026-10-15T10:00:00Z"), "/api/v1/archivos/" + soporte));
        when(detalleReclamo.execute(new ConsultarDetalleReclamoDocenteDTO(RECLAMO, ACTOR))).thenReturn(detalle);

        mvc.perform(get("/api/v1/docente/reclamos/{id}", RECLAMO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.id").value(RECLAMO.toString()))
                .andExpect(jsonPath("$.datos.codigoEstado").value("P"))
                .andExpect(jsonPath("$.datos.estadoSolicitud").value("PENDIENTE"))
                .andExpect(jsonPath("$.datos.nombreEstado").value("pendiente"))
                .andExpect(jsonPath("$.datos.estudianteNombre").value("Ana Perez"))
                .andExpect(jsonPath("$.datos.fechaSesionUtc").value("2026-10-14T13:00:00.1234567Z"))
                .andExpect(jsonPath("$.datos.causalInasistencia.codigo").value("SJC"))
                .andExpect(jsonPath("$.datos.causalInasistencia.nombre").value("sin justa causa"))
                .andExpect(jsonPath("$.datos.causalInasistencia.asistio").value(false))
                .andExpect(jsonPath("$.datos.soporteAdjunto.fileId").value(soporte.toString()))
                .andExpect(jsonPath("$.datos.soporteAdjunto.fechaSubidaUtc").value("2026-10-15T10:00:00Z"))
                .andExpect(jsonPath("$.datos.soporteAdjunto.url").value("/api/v1/archivos/" + soporte))
                .andExpect(jsonPath("$.total").doesNotExist());
    }

    @Test
    void consultarDetalleReclamo_sin_causal_ni_soporte_los_serializa_como_nulos() throws Exception {
        final DetalleReclamoDocenteDTO detalle = new DetalleReclamoDocenteDTO(RECLAMO, "REV-1",
                java.time.LocalDate.of(2026, 10, 15), "R", "RECHAZADA", "rechazada", "Cita medica", "No procede",
                UUID.randomUUID(), "Ana Perez", UUID.randomUUID(), 4, null, null, UUID.randomUUID(), 1, "Grupo 1",
                UUID.randomUUID(), "MAT101", "Calculo I", null, null);
        when(detalleReclamo.execute(new ConsultarDetalleReclamoDocenteDTO(RECLAMO, ACTOR))).thenReturn(detalle);

        mvc.perform(get("/api/v1/docente/reclamos/{id}", RECLAMO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.estadoSolicitud").value("RECHAZADA"))
                .andExpect(jsonPath("$.datos.justificacionRespuesta").value("No procede"))
                .andExpect(jsonPath("$.datos.fechaSesionUtc").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.datos.causalInasistencia").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.datos.soporteAdjunto").value(org.hamcrest.Matchers.nullValue()));
    }

    // CT-02 (AC-05): id con formato no UUID -> 400, sin invocar el caso de uso.
    @Test
    void consultarDetalleReclamo_con_id_no_uuid_responde_400_sin_invocar_el_caso_de_uso() throws Exception {
        mvc.perform(get("/api/v1/docente/reclamos/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        org.mockito.Mockito.verifyNoInteractions(detalleReclamo);
    }

    // CT-03 (AC-05): inexistente o ajena -> 404 RESOURCE_NOT_FOUND.
    @Test
    void consultarDetalleReclamo_no_encontrado_responde_404_resource_not_found() throws Exception {
        when(detalleReclamo.execute(new ConsultarDetalleReclamoDocenteDTO(RECLAMO, ACTOR)))
                .thenThrow(new ResourceNotFoundException("La solicitud de revision no existe."));

        mvc.perform(get("/api/v1/docente/reclamos/{id}", RECLAMO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    // CT-04 (AC-05): el actor sale del usuario autenticado y el id de la ruta, nunca del request.
    @Test
    void consultarDetalleReclamo_toma_el_actor_del_usuario_autenticado_y_el_id_de_la_ruta() throws Exception {
        when(detalleReclamo.execute(new ConsultarDetalleReclamoDocenteDTO(RECLAMO, ACTOR)))
                .thenThrow(new ResourceNotFoundException("La solicitud de revision no existe."));

        mvc.perform(get("/api/v1/docente/reclamos/{id}", RECLAMO)
                .param("actorUsuarioId", UUID.randomUUID().toString())
                .header("X-Usuario-Id", UUID.randomUUID().toString()));

        final ArgumentCaptor<ConsultarDetalleReclamoDocenteDTO> captor =
                ArgumentCaptor.forClass(ConsultarDetalleReclamoDocenteDTO.class);
        verify(detalleReclamo).execute(captor.capture());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
        assertEquals(RECLAMO, captor.getValue().idSolicitud());
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
