package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.primaryports.AdministrarNotificacionesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.EstadoCanalNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.MarcadorPlantillaProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.NotificacionSalienteAdminProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.PaginaColaNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.ResumenColaNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.TipoPlantillaAdminProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VersionPlantillaProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VistaPreviaPlantillaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminNotificacionControllerTest {

    private static final UUID ADMIN_ID = UUID.randomUUID();

    private AdministrarNotificacionesInputPort administracionPort;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        administracionPort = mock(AdministrarNotificacionesInputPort.class);
        final AuthenticatedUserResolver identity = () -> ADMIN_ID;
        final AdminNotificacionController controller = new AdminNotificacionController(administracionPort, identity);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty()))
                .build();
    }

    @Test
    @DisplayName("GET /estado-canal: retorna estado del canal con datos estructurados")
    void consultarEstadoCanal_retornaOk() throws Exception {
        final EstadoCanalNotificacionProjection estado = new EstadoCanalNotificacionProjection(
                "outbox",
                "smtp",
                true,
                true,
                "smtp.uco.edu.co",
                587,
                "asistencias@uco.edu.co",
                true,
                "no-reply@uco.edu.co",
                "Asistencias UCO",
                true,
                false,
                50,
                60,
                500,
                false,
                "https://asistencias.uco.edu.co",
                false,
                0
        );
        when(administracionPort.consultarEstadoCanal()).thenReturn(estado);

        mockMvc.perform(get("/api/v1/admin/notificaciones/estado-canal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.provider").value("outbox"))
                .andExpect(jsonPath("$.datos.sender").value("smtp"))
                .andExpect(jsonPath("$.datos.dispatcherEnabled").value(true))
                .andExpect(jsonPath("$.datos.disponible").value(true))
                .andExpect(jsonPath("$.datos.host").value("smtp.uco.edu.co"))
                .andExpect(jsonPath("$.datos.tienePasswordConfigurado").value(true));
    }

    @Test
    @DisplayName("GET /plantillas: retorna lista de plantillas con marcadores y versiones")
    void consultarPlantillas_retornaLista() throws Exception {
        final TipoPlantillaAdminProjection tipo = new TipoPlantillaAdminProjection(
                "RETIRO_ESTUDIANTE_GRUPO",
                "es",
                1,
                "Retiro {grupo}",
                "Cuerpo {estudiante}",
                Instant.now(),
                List.of(new MarcadorPlantillaProjection("grupo", "Grupo", "G01", true)),
                List.of(new VersionPlantillaProjection(UUID.randomUUID(), "RETIRO_ESTUDIANTE_GRUPO", "es", 1, "Retiro {grupo}", "Cuerpo {estudiante}", true, Instant.now()))
        );
        when(administracionPort.consultarPlantillas("es", ADMIN_ID)).thenReturn(List.of(tipo));

        mockMvc.perform(get("/api/v1/admin/notificaciones/plantillas?idioma=es"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].tipo").value("RETIRO_ESTUDIANTE_GRUPO"))
                .andExpect(jsonPath("$.datos[0].versionActiva").value(1))
                .andExpect(jsonPath("$.datos[0].marcadores[0].marcador").value("grupo"));
    }

    @Test
    @DisplayName("POST /plantillas/{tipo}/versiones: crea version de plantilla")
    void crearVersionPlantilla_retornaNuevaVersion() throws Exception {
        final UUID versionId = UUID.randomUUID();
        final VersionPlantillaProjection version = new VersionPlantillaProjection(
                versionId,
                "RETIRO_ESTUDIANTE_GRUPO",
                "es",
                2,
                "Nuevo asunto {grupo}",
                "Nuevo cuerpo {estudiante}",
                false,
                Instant.now()
        );
        when(administracionPort.crearVersionPlantilla(eq("RETIRO_ESTUDIANTE_GRUPO"), eq("es"), anyString(), anyString(), eq(ADMIN_ID)))
                .thenReturn(version);

        mockMvc.perform(post("/api/v1/admin/notificaciones/plantillas/RETIRO_ESTUDIANTE_GRUPO/versiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asunto\":\"Nuevo asunto {grupo}\",\"cuerpo\":\"Nuevo cuerpo {estudiante}\",\"idioma\":\"es\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.version").value(2))
                .andExpect(jsonPath("$.datos.asunto").value("Nuevo asunto {grupo}"));

        verify(administracionPort).crearVersionPlantilla(
                eq("RETIRO_ESTUDIANTE_GRUPO"),
                eq("es"),
                eq("Nuevo asunto {grupo}"),
                eq("Nuevo cuerpo {estudiante}"),
                eq(ADMIN_ID)
        );
    }

    @Test
    @DisplayName("PATCH /plantillas/{tipo}/versiones/{version}/activar: activa version de plantilla")
    void activarVersionPlantilla_retornaExito() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/notificaciones/plantillas/RETIRO_ESTUDIANTE_GRUPO/versiones/2/activar?idioma=es"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.mensaje").isNotEmpty());

        verify(administracionPort).activarVersionPlantilla("RETIRO_ESTUDIANTE_GRUPO", "es", 2, ADMIN_ID);
    }

    @Test
    @DisplayName("POST /plantillas/{tipo}/vista-previa: genera vista previa renderizada")
    void generarVistaPrevia_retornaHtmlYTexto() throws Exception {
        when(administracionPort.generarVistaPrevia(eq("RETIRO_ESTUDIANTE_GRUPO"), eq("es"), anyString(), anyString()))
                .thenReturn(new VistaPreviaPlantillaProjection("Asunto Vista Previa", "Texto plano", "<p>HTML</p>", "preview"));

        mockMvc.perform(post("/api/v1/admin/notificaciones/plantillas/RETIRO_ESTUDIANTE_GRUPO/vista-previa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asunto\":\"Retiro {grupo}\",\"cuerpo\":\"Cuerpo {estudiante}\",\"idioma\":\"es\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.asunto").value("Asunto Vista Previa"))
                .andExpect(jsonPath("$.datos.textoPlano").value("Texto plano"))
                .andExpect(jsonPath("$.datos.html").value("<p>HTML</p>"));
    }

    @Test
    @DisplayName("GET /cola: consulta cola paginada con resumen")
    void consultarCola_retornaPaginaYResumen() throws Exception {
        final UUID idNotif = UUID.randomUUID();
        final UUID idTraza = UUID.randomUUID();
        final PaginaColaNotificacionProjection pagina = new PaginaColaNotificacionProjection(
                List.of(new NotificacionSalienteAdminProjection(
                        idNotif,
                        "RETIRO_ESTUDIANTE_GRUPO",
                        "Retiro",
                        "PENDIENTE",
                        "Pendiente",
                        false,
                        2,
                        0,
                        Instant.now(),
                        null,
                        null,
                        null,
                        null,
                        idTraza
                )),
                0,
                20,
                1L,
                1,
                new ResumenColaNotificacionProjection(1, 0, 5, 0, 0)
        );
        when(administracionPort.consultarCola(any(), any(), anyInt(), anyInt(), any())).thenReturn(pagina);

        mockMvc.perform(get("/api/v1/admin/notificaciones/cola?pagina=0&tamano=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.totalElementos").value(1))
                .andExpect(jsonPath("$.datos.resumen.pendientes").value(1))
                .andExpect(jsonPath("$.datos.elementos[0].id").value(idNotif.toString()))
                .andExpect(jsonPath("$.datos.elementos[0].idCorrelacion").value(idTraza.toString()));
    }

    @Test
    @DisplayName("POST /cola/{id}/reintentar: reintenta notificacion saliente")
    void reintentarNotificacion_retornaExito() throws Exception {
        final UUID idNotif = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/admin/notificaciones/cola/{id}/reintentar", idNotif))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true));

        verify(administracionPort).reintentarNotificacion(idNotif, ADMIN_ID);
    }

    @Test
    @DisplayName("POST /prueba: envia correo de prueba")
    void enviarCorreoPrueba_retornaExito() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notificaciones/prueba")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correoDestino\":\"test@uco.edu.co\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true));

        verify(administracionPort).enviarCorreoPrueba(ADMIN_ID, null, "test@uco.edu.co");
    }

    @Test
    @DisplayName("CULM-001 A.2-6 (A2-B-11): POST /versiones con {} responde 400 VALIDATION_ERROR sin llegar al adaptador")
    void crearVersionPlantilla_sin_asunto_ni_cuerpo_responde_400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notificaciones/plantillas/RETIRO_ESTUDIANTE_GRUPO/versiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("asunto"))
                .andExpect(jsonPath("$.details[1].field").value("cuerpo"));
        mockMvc.perform(post("/api/v1/admin/notificaciones/plantillas/RETIRO_ESTUDIANTE_GRUPO/versiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asunto\":\"  \",\"cuerpo\":\"Cuerpo\"}"))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verifyNoInteractions(administracionPort);
    }
}
