package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.ConsultarOfertaCreacionGruposDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.dto.AsignaturaOfertaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.dto.OfertaCreacionGruposDocenteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** F3: GET /api/v1/docente/oferta-creacion-grupos delega con el usuario autenticado y devuelve idDocente y asignaturas. */
class DocenteOfertaCreacionGruposControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();

    private final ConsultarOfertaCreacionGruposDocenteInputPort inputPort = mock(ConsultarOfertaCreacionGruposDocenteInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new DocenteOfertaCreacionGruposController(inputPort, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    @Test
    void consultarOferta_delega_con_el_usuario_autenticado_y_devuelve_idDocente_y_asignaturas() throws Exception {
        final UUID docente = UUID.randomUUID();
        final UUID asignatura = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        when(inputPort.execute(ACTOR)).thenReturn(new OfertaCreacionGruposDocenteDTO(docente, List.of(
                new AsignaturaOfertaDocenteDTO(asignatura, "MAT1", "Algebra", programa, "Ingenieria de Sistemas"))));

        mvc.perform(get("/api/v1/docente/oferta-creacion-grupos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.idDocente").value(docente.toString()))
                .andExpect(jsonPath("$.datos.asignaturas[0].idAsignatura").value(asignatura.toString()))
                .andExpect(jsonPath("$.datos.asignaturas[0].nombreAsignatura").value("Algebra"))
                .andExpect(jsonPath("$.datos.asignaturas[0].nombrePrograma").value("Ingenieria de Sistemas"));

        verify(inputPort).execute(ACTOR);
    }
}
