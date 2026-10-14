package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.controller;

import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.localsse.LocalSseRealtimeStreamGateway;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * FIX-001 (RED): un cliente SSE real (EventSource) envia solo {@code Accept: text/event-stream}.
 * Los errores del canal deben conservar el status contractual de
 * {@code /api/v1/realtime/stream} (400/403) con cuerpo {@code ApiErrorResponse} JSON, en lugar de
 * degradar a 500 o a un 400 sin cuerpo por fallo de negociacion de contenido del handler.
 */
class RealtimeStreamErrorHttpTest {

    private static final String STREAM = "/api/v1/realtime/stream";
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID GRUPO_PROPIO = UUID.randomUUID();
    private static final UUID GRUPO_AJENO = UUID.randomUUID();

    private final ReactorRealtimeAdapter realtimeAdapter = new ReactorRealtimeAdapter(new SimpleMeterRegistry());
    private final InstitutionalScopePort institutionalScopePort = mock(InstitutionalScopePort.class);
    private final ResolverMensajeUsuarioInputPort resolverMensajeUsuario = mock(ResolverMensajeUsuarioInputPort.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(resolverMensajeUsuario.execute(any())).thenReturn(Optional.empty());
        when(institutionalScopePort.canDocenteAccessGrupo(USUARIO, GRUPO_PROPIO)).thenReturn(true);
        when(institutionalScopePort.canDocenteAccessGrupo(USUARIO, GRUPO_AJENO)).thenReturn(false);
        final RealtimeEventsController controller = new RealtimeEventsController(
                new LocalSseRealtimeStreamGateway(realtimeAdapter, institutionalScopePort),
                () -> USUARIO
        );
        mockMvc = standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(resolverMensajeUsuario))
                .build();
    }

    @Test
    void grupo_ajeno_con_accept_event_stream_responde_403_forbidden_json_sin_abrir_stream() throws Exception {
        mockMvc.perform(get(STREAM).param("grupoId", GRUPO_AJENO.toString()).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncNotStarted())
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value(STREAM));

        assertEquals(0, realtimeAdapter.activeSubscribersCount());
    }

    @Test
    void grupoId_ausente_con_accept_event_stream_responde_400_con_cuerpo_de_error_y_campo() throws Exception {
        mockMvc.perform(get(STREAM).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncNotStarted())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("grupoId"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_REQUIRED"));
    }

    @Test
    void grupoId_no_uuid_con_accept_event_stream_responde_400_invalid_request() throws Exception {
        mockMvc.perform(get(STREAM).param("grupoId", "no-es-uuid").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details[0].field").value("grupoId"))
                .andExpect(jsonPath("$.details[0].code").value("FIELD_INVALID_UUID"));
    }

    @Test
    void grupo_propio_con_accept_event_stream_conserva_el_stream_sse_exitoso() throws Exception {
        final MvcResult result = mockMvc.perform(
                        get(STREAM).param("grupoId", GRUPO_PROPIO.toString()).accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        assertEquals(1, realtimeAdapter.activeSubscribersCount());
        result.getRequest().getAsyncContext().complete();
    }
}
