package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.AsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.EstudianteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.PeriodoAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.PlanEstudioDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturas.primaryports.ConsultarAsignaturasInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasignaturasplan.primaryports.ConsultarAsignaturasPlanInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.ConsultarEstudiantesProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports.ConsultarPeriodosAcademicosInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.primaryports.ConsultarPlanesEstudioInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.GestionarAsignaturaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.dto.GuardarAsignaturaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.GestionarPlanEstudioInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.dto.GuardarPlanEstudioDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CoordinadorPortalControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PLAN = UUID.randomUUID();
    private static final UUID SUBJECT = UUID.randomUUID();

    private final ConsultarPlanesEstudioInputPort plans = mock(ConsultarPlanesEstudioInputPort.class);
    private final ConsultarAsignaturasPlanInputPort planSubjects = mock(ConsultarAsignaturasPlanInputPort.class);
    private final ConsultarAsignaturasInputPort subjects = mock(ConsultarAsignaturasInputPort.class);
    private final ConsultarPeriodosAcademicosInputPort periods = mock(ConsultarPeriodosAcademicosInputPort.class);
    private final ConsultarEstudiantesProgramaInputPort students = mock(ConsultarEstudiantesProgramaInputPort.class);
    private final GestionarPlanEstudioInputPort managePlans = mock(GestionarPlanEstudioInputPort.class);
    private final GestionarAsignaturaInputPort manageSubjects = mock(GestionarAsignaturaInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final CoordinadorPortalController controller = new CoordinadorPortalController(plans, planSubjects,
            subjects, periods, students, managePlans, manageSubjects, identity);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty())).build();

    @Test
    void queryEndpointsPassActorAndSerializePortResults() throws Exception {
        when(plans.execute(ACTOR)).thenReturn(List.of(new PlanEstudioDTO(PLAN, UUID.randomUUID(), "Ingeniería", "INP",
                true, "Activo", null)));
        when(planSubjects.execute(ACTOR, PLAN)).thenReturn(List.of(subject()));
        when(subjects.execute(ACTOR)).thenReturn(List.of(subject()));
        when(periods.execute()).thenReturn(List.of(new PeriodoAcademicoDTO(UUID.randomUUID(), UUID.randomUUID(),
                "UCO", "2026-1", "2026-1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1), 2026)));
        when(students.execute(ACTOR)).thenReturn(List.of(new EstudianteProgramaDTO(UUID.randomUUID(), UUID.randomUUID(),
                "123456", "Ana", "ana@example.com", UUID.randomUUID(), "Ingeniería")));

        mvc.perform(get("/api/v1/coordinador/planes-estudio"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].id").value(PLAN.toString()));
        mvc.perform(get("/api/v1/coordinador/planes-estudio/{id}/asignaturas", PLAN))
                .andExpect(status().isOk()).andExpect(jsonPath("$.datos[0].id").value(SUBJECT.toString()));
        mvc.perform(get("/api/v1/coordinador/asignaturas"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/v1/coordinador/periodos-academicos"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.datos[0].nombre").value("2026-1"));
        mvc.perform(get("/api/v1/coordinador/estudiantes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.datos[0].nombreCompleto").value("Ana"));
        verify(planSubjects).execute(ACTOR, PLAN);
        verify(subjects).execute(ACTOR);
        verify(students).execute(ACTOR);
    }

    @Test
    void commandsMapBodyAndPathToInputPorts() throws Exception {
        mvc.perform(post("/api/v1/coordinador/planes-estudio").contentType("application/json")
                        .content("{\"inp\":2026}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.exitoso").value(true));
        mvc.perform(put("/api/v1/coordinador/planes-estudio/{id}", PLAN).contentType("application/json")
                        .content("{\"inp\":2027}"))
                .andExpect(status().isOk());
        final var planCaptor = ArgumentCaptor.forClass(GuardarPlanEstudioDTO.class);
        verify(managePlans, org.mockito.Mockito.times(2)).guardar(planCaptor.capture());
        assertNull(planCaptor.getAllValues().get(0).idPlanEstudio());
        assertEquals(2026, planCaptor.getAllValues().get(0).inp());
        assertEquals(ACTOR, planCaptor.getAllValues().get(0).usuario());
        assertEquals(PLAN, planCaptor.getAllValues().get(1).idPlanEstudio());
        assertEquals(2027, planCaptor.getAllValues().get(1).inp());

        final String body = "{\"codigo\":\"A01\",\"nombre\":\"Álgebra\",\"creditos\":3,"
                + "\"semestreNumero\":2,\"nombreArea\":\"Matemáticas\",\"nombreComponente\":\"Básico\"}";
        mvc.perform(post("/api/v1/coordinador/planes-estudio/{id}/asignaturas", PLAN)
                        .contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(put("/api/v1/coordinador/planes-estudio/{planId}/asignaturas/{asigId}", PLAN, SUBJECT)
                        .contentType("application/json").content(body)).andExpect(status().isOk());
        final var subjectCaptor = ArgumentCaptor.forClass(GuardarAsignaturaDTO.class);
        verify(manageSubjects).crear(subjectCaptor.capture());
        assertNull(subjectCaptor.getValue().idAsignatura());
        assertEquals(PLAN, subjectCaptor.getValue().idPlanEstudio());
        assertEquals("A01", subjectCaptor.getValue().codigo());
        assertEquals(3, subjectCaptor.getValue().creditos());
        assertEquals("Matemáticas", subjectCaptor.getValue().nombreArea());
        verify(manageSubjects).actualizar(subjectCaptor.capture());
        assertEquals(SUBJECT, subjectCaptor.getValue().idAsignatura());
        assertEquals(ACTOR, subjectCaptor.getValue().usuarioEjecutor());

        mvc.perform(patch("/api/v1/coordinador/asignaturas/{id}/estado", SUBJECT))
                .andExpect(status().isOk());
        verify(manageSubjects).toggleEstado(SUBJECT, ACTOR);
    }

    @Test
    void missingBodyAndPortFailurePreserveHttpErrorContract() throws Exception {
        mvc.perform(post("/api/v1/coordinador/planes-estudio").contentType("application/json"))
                .andExpect(status().isBadRequest());
        doThrow(new FeatureUnavailableException("Sin capacidad pública"))
                .when(manageSubjects).toggleEstado(SUBJECT, ACTOR);
        mvc.perform(patch("/api/v1/coordinador/asignaturas/{id}/estado", SUBJECT))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void unavailableEndpointsRemainExplicitlyUnavailable() throws Exception {
        final List<org.junit.jupiter.api.function.Executable> operations = List.of(
                controller::consultarDocentesPrograma, controller::crearDocente,
                () -> controller.toggleEstadoDocente(SUBJECT),
                () -> controller.toggleEstadoPlanEstudio(PLAN),
                () -> controller.agregarSemestrePlan(PLAN),
                () -> controller.eliminarSemestrePlan(PLAN, 1),
                () -> controller.eliminarAsignaturaPlan(PLAN, SUBJECT),
                controller::crearPeriodoAcademico, () -> controller.actualizarPeriodoAcademico(PLAN),
                () -> controller.toggleEstadoPeriodoAcademico(PLAN),
                controller::consultarSolicitudesMatricula, () -> controller.resolverSolicitudMatricula(PLAN),
                () -> controller.matricularEstudianteEnGrupo(PLAN),
                () -> controller.retirarEstudianteDeGrupo(PLAN, SUBJECT)
        );
        for (final var operation : operations) {
            assertEquals("FEATURE_UNAVAILABLE",
                    assertThrows(FeatureUnavailableException.class, operation).getCode());
        }
    }

    private static AsignaturaDTO subject() {
        return new AsignaturaDTO(SUBJECT, "A01", "Álgebra", 3, UUID.randomUUID(), "Matemáticas",
                UUID.randomUUID(), "Básico", UUID.randomUUID(), PLAN, UUID.randomUUID(), "Ingeniería",
                "2", true, "Activa");
    }
}
