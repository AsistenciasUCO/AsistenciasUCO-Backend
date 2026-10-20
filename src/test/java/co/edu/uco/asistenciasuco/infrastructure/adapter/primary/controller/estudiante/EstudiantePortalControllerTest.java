package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.ConsultarHorariosEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.dto.HorarioEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.ConsultarMateriasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.dto.MateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.ConsultarSesionesMateriaEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.SesionMateriaEstudianteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EstudiantePortalControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID MATERIA = UUID.randomUUID();

    private final ConsultarMateriasEstudianteInputPort materias = mock(ConsultarMateriasEstudianteInputPort.class);
    private final ConsultarHorariosEstudianteInputPort horarios = mock(ConsultarHorariosEstudianteInputPort.class);
    private final ConsultarSesionesMateriaEstudianteInputPort sesiones = mock(ConsultarSesionesMateriaEstudianteInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final EstudiantePortalController controller =
            new EstudiantePortalController(materias, horarios, sesiones, identity);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    @Test
    void consultarMaterias_usa_actor_autenticado() throws Exception {
        when(materias.execute(ACTOR)).thenReturn(List.of(new MateriaEstudianteDTO(
                UUID.randomUUID(), "Calculo", UUID.randomUUID(), "Grupo 1")));

        mvc.perform(get("/api/v1/estudiante/materias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].nombreAsignatura").value("Calculo"));
        verify(materias).execute(ACTOR);
    }

    @Test
    void consultarHorarios_usa_actor_autenticado() throws Exception {
        when(horarios.execute(ACTOR)).thenReturn(List.of(new HorarioEstudianteDTO(
                UUID.randomUUID(), ACTOR, UUID.randomUUID(), "MAT-01", "Calculo", "G1", "LUNES",
                java.time.LocalTime.of(8, 0), java.time.LocalTime.of(10, 0), "Docente Uno")));

        mvc.perform(get("/api/v1/estudiante/horarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].nombreMateria").value("Calculo"));
        verify(horarios).execute(ACTOR);
    }

    @Test
    void consultarSesionesMateria_usa_actor_y_materia() throws Exception {
        when(sesiones.execute(ACTOR, MATERIA)).thenReturn(List.of(new SesionMateriaEstudianteDTO(
                UUID.randomUUID(), "Sesion 1", 1, "SES-01", 1, UUID.randomUUID(), "G1", "Grupo 1",
                java.time.LocalDateTime.of(2026, 1, 20, 8, 0), java.time.LocalDateTime.of(2026, 1, 20, 10, 0),
                "AUSENTE", null, true)));

        mvc.perform(get("/api/v1/estudiante/materias/{materiaId}/sesiones", MATERIA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].nombre").value("Sesion 1"))
                .andExpect(jsonPath("$.datos[0].estadoAsistencia").value("AUSENTE"))
                .andExpect(jsonPath("$.datos[0].estadoSolicitudRevision").doesNotExist())
                .andExpect(jsonPath("$.datos[0].reclamable").value(true));
        verify(sesiones).execute(ACTOR, MATERIA);
    }

    @Test
    void endpoints_no_implementados_lanzan_featureUnavailable() {
        final List<Executable> operations = List.of(
                () -> controller.consultarPrerrequisitos(MATERIA),
                controller::consultarReclamos, controller::crearReclamo,
                () -> controller.eliminarReclamo(MATERIA), controller::crearSolicitudMatricula,
                controller::matricularGrupo
        );

        for (final var operation : operations) {
            assertEquals("FEATURE_UNAVAILABLE",
                    assertThrows(FeatureUnavailableException.class, operation).getCode());
        }
    }
}
