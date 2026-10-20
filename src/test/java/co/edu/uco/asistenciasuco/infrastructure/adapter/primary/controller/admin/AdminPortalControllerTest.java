package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.ConsultarAreasInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.dto.AreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.ConsultarDecanosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.dto.DecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.ConsultarFacultadesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.dto.FacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.ConsultarInstitucionesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.dto.InstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.ConsultarParametrosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.dto.ParametroDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.CrearDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.dto.CrearDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.CrearFacultadInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.dto.CrearFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.EjecutarCierreMasivoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.dto.EjecutarCierreMasivoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminPortalControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID FACULTAD = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final UUID DECANO = UUID.randomUUID();

    private final ConsultarDecanosInputPort consultarDecanos = mock(ConsultarDecanosInputPort.class);
    private final CrearDecanoInputPort crearDecano = mock(CrearDecanoInputPort.class);
    private final ConsultarParametrosInputPort consultarParametros = mock(ConsultarParametrosInputPort.class);
    private final EjecutarCierreMasivoInputPort ejecutarCierreMasivo = mock(EjecutarCierreMasivoInputPort.class);
    private final ConsultarInstitucionesInputPort consultarInstituciones = mock(ConsultarInstitucionesInputPort.class);
    private final ConsultarFacultadesInputPort consultarFacultades = mock(ConsultarFacultadesInputPort.class);
    private final CrearFacultadInputPort crearFacultad = mock(CrearFacultadInputPort.class);
    private final ConsultarAreasInputPort consultarAreas = mock(ConsultarAreasInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final AdminPortalController controller = new AdminPortalController(consultarDecanos, crearDecano,
            consultarParametros, ejecutarCierreMasivo, consultarInstituciones, consultarFacultades, crearFacultad, consultarAreas, identity);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    @Test
    void consultarDecanos_serializa_lista_del_puerto() throws Exception {
        when(consultarDecanos.execute()).thenReturn(List.of(new DecanoDTO(UUID.randomUUID(), UUID.randomUUID(),
                "123456", "Ana Perez", FACULTAD, "Ingenieria", true)));

        mvc.perform(get("/api/v1/admin/decanos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].nombreCompleto").value("Ana Perez"));
    }

    @Test
    void crearDecano_mapea_cuerpo_de_la_peticion_al_dto() throws Exception {
        final String body = "{\"tipoIdentificacionId\":\"" + UUID.randomUUID() + "\",\"numeroIdentificacion\":123456789,"
                + "\"primerNombre\":\"Ana\",\"segundoNombre\":\"Maria\",\"primerApellido\":\"Perez\","
                + "\"segundoApellido\":\"Gomez\",\"correo\":\"ana@uco.edu.co\",\"password\":\"Clave123!\","
                + "\"idFacultad\":\"" + FACULTAD + "\"}";

        mvc.perform(post("/api/v1/admin/decanos").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true));

        final ArgumentCaptor<CrearDecanoDTO> captor = ArgumentCaptor.forClass(CrearDecanoDTO.class);
        verify(crearDecano).execute(captor.capture());
        assertEquals("Ana", captor.getValue().primerNombre());
        assertEquals(FACULTAD, captor.getValue().idFacultad());
    }

    @Test
    void consultarParametros_serializa_lista_del_puerto() throws Exception {
        when(consultarParametros.execute()).thenReturn(List.of(new ParametroDTO(UUID.randomUUID(), "GRUPO",
                "clave", "valor", "STRING", "defecto", true)));

        mvc.perform(get("/api/v1/admin/parametros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].clave").value("clave"));
    }

    @Test
    void ejecutarCierreMasivo_usa_usuario_autenticado() throws Exception {
        final String body = "{\"idPeriodoAcademico\":\"" + PERIODO + "\"}";

        mvc.perform(post("/api/v1/admin/cierre-masivo").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true));

        final ArgumentCaptor<EjecutarCierreMasivoDTO> captor = ArgumentCaptor.forClass(EjecutarCierreMasivoDTO.class);
        verify(ejecutarCierreMasivo).execute(captor.capture());
        assertEquals(PERIODO, captor.getValue().idPeriodoAcademico());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
    }

    @Test
    void consultarInstituciones_serializa_lista_del_puerto() throws Exception {
        when(consultarInstituciones.execute()).thenReturn(List.of(new InstitucionDTO(UUID.randomUUID(), "UCO", true, "Activa")));

        mvc.perform(get("/api/v1/admin/instituciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].nombre").value("UCO"));
    }

    @Test
    void consultarFacultades_serializa_lista_del_puerto() throws Exception {
        when(consultarFacultades.execute()).thenReturn(List.of(new FacultadDTO(FACULTAD, "Ingenieria",
                UUID.randomUUID(), "UCO", UUID.randomUUID(), "Decano Uno", true, "Activa")));

        mvc.perform(get("/api/v1/admin/facultades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].nombreFacultad").value("Ingenieria"));
    }

    @Test
    void consultarAreas_serializa_lista_del_puerto() throws Exception {
        when(consultarAreas.execute()).thenReturn(List.of(new AreaDTO(UUID.randomUUID(), "Matematicas")));

        mvc.perform(get("/api/v1/admin/areas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].nombre").value("Matematicas"));
    }

    @Test
    void crearFacultad_mapea_cuerpo_y_delega_al_puerto() throws Exception {
        final String body = "{\"nombre\":\"Facultad de Ingenieria\",\"codigo\":\"FAC-ING\",\"idDecano\":\"" + DECANO + "\"}";

        mvc.perform(post("/api/v1/admin/facultades").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.nombreFacultad").value("Facultad de Ingenieria"));

        final ArgumentCaptor<CrearFacultadDTO> captor = ArgumentCaptor.forClass(CrearFacultadDTO.class);
        verify(crearFacultad).execute(captor.capture());
        assertEquals("Facultad de Ingenieria", captor.getValue().nombre());
        assertEquals(DECANO, captor.getValue().idDecano());
        assertEquals(ACTOR, captor.getValue().actorUsuarioId());
    }

    @Test
    void endpoints_no_implementados_lanzan_featureUnavailable() {
        final List<Executable> operations = List.of(
                () -> controller.toggleDecano(FACULTAD),
                controller::consultarSedes, controller::crearSede,
                () -> controller.actualizarSede(FACULTAD), () -> controller.cambiarEstadoSede(FACULTAD),
                controller::consultarEspaciosFisicos, controller::crearEspacioFisico,
                () -> controller.actualizarEspacioFisico(FACULTAD), () -> controller.cambiarEstadoEspacioFisico(FACULTAD),
                () -> controller.actualizarParametro(FACULTAD), controller::consultarAuditoria,
                controller::crearInstitucion, () -> controller.actualizarInstitucion(FACULTAD),
                () -> controller.cambiarEstadoInstitucion(FACULTAD),
                () -> controller.actualizarFacultad(FACULTAD),
                () -> controller.cambiarEstadoFacultad(FACULTAD),
                controller::crearArea, () -> controller.actualizarArea(FACULTAD), () -> controller.cambiarEstadoArea(FACULTAD)
        );

        for (final var operation : operations) {
            assertEquals("FEATURE_UNAVAILABLE",
                    assertThrows(FeatureUnavailableException.class, operation).getCode());
        }
    }
}
