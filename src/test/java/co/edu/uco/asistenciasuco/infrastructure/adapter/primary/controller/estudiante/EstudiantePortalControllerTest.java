package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.exception.business.FeatureUnavailableException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.ConsultarReclamosEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ConsultarReclamosEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ReclamoEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ReclamosEstudiantePaginaDTO;
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
    private final ConsultarReclamosEstudianteInputPort reclamos = mock(ConsultarReclamosEstudianteInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final EstudiantePortalController controller =
            new EstudiantePortalController(materias, horarios, sesiones, reclamos, identity);
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

    /** HU010/HU011 (consulta): CT-01 el estudiante ve sus solicitudes con los mismos campos que la bandeja docente. */
    @Test
    void CT01_consultarReclamos_usa_actor_filtros_y_serializa_soporte() throws Exception {
        final UUID solicitud = UUID.randomUUID();
        final UUID archivo = UUID.randomUUID();
        when(reclamos.execute(new ConsultarReclamosEstudianteDTO(ACTOR, "P", 2, 5))).thenReturn(
                new ReclamosEstudiantePaginaDTO(List.of(new ReclamoEstudianteDTO(solicitud, "RAD-1",
                        java.time.LocalDate.of(2026, 10, 20), "PENDIENTE", "Cita medica", "", UUID.randomUUID(), 3,
                        java.time.Instant.parse("2026-10-19T15:30:45Z"), "UTC_V2", UUID.randomUUID(), 1, "Grupo 1",
                        UUID.randomUUID(), "MAT-1", "Calculo",
                        new ReclamoEstudianteDTO.SoporteAdjuntoDTO(archivo, "incapacidad.pdf", "application/pdf", 2048L,
                                java.time.Instant.parse("2026-10-20T10:00:00Z"), "/api/v1/archivos/" + archivo))), 9));

        mvc.perform(get("/api/v1/estudiante/reclamos").param("estado", "P").param("pagina", "2").param("tamano", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(9))
                .andExpect(jsonPath("$.datos[0].id").value(solicitud.toString()))
                .andExpect(jsonPath("$.datos[0].estadoSolicitud").value("PENDIENTE"))
                .andExpect(jsonPath("$.datos[0].fechaSolicitud").value("2026-10-20"))
                .andExpect(jsonPath("$.datos[0].fechaSesionUtc").value("2026-10-19T15:30:45Z"))
                .andExpect(jsonPath("$.datos[0].materiaNombre").value("Calculo"))
                .andExpect(jsonPath("$.datos[0].soporteAdjunto.fileId").value(archivo.toString()))
                .andExpect(jsonPath("$.datos[0].soporteAdjunto.fechaSubidaUtc").value("2026-10-20T10:00:00Z"))
                .andExpect(jsonPath("$.datos[0].estudianteId").doesNotExist());
    }

    /** CT-02: sin parametros delega nulos (el caso de uso aplica los valores por defecto). */
    @Test
    void CT02_consultarReclamos_sin_parametros() throws Exception {
        when(reclamos.execute(new ConsultarReclamosEstudianteDTO(ACTOR, null, null, null)))
                .thenReturn(new ReclamosEstudiantePaginaDTO(List.of(), 0));

        mvc.perform(get("/api/v1/estudiante/reclamos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    /** CT-03: usuario sin estudiante institucional => 403. */
    @Test
    void CT03_consultarReclamos_sin_estudiante_es_403() throws Exception {
        when(reclamos.execute(new ConsultarReclamosEstudianteDTO(ACTOR, null, null, null)))
                .thenThrow(new ForbiddenException("No fue posible resolver el estudiante autenticado."));

        mvc.perform(get("/api/v1/estudiante/reclamos")).andExpect(status().isForbidden());
    }

    @Test
    void endpoints_no_implementados_lanzan_featureUnavailable() {
        final List<Executable> operations = List.of(
                controller::crearReclamo,
                () -> controller.eliminarReclamo(MATERIA)
        );
        // HU176B (CONTRACT §3.5): POST /estudiante/matricular-grupo sale de 501; ver EstudianteMatriculaQrControllerTest.

        for (final var operation : operations) {
            assertEquals("FEATURE_UNAVAILABLE",
                    assertThrows(FeatureUnavailableException.class, operation).getCode());
        }
    }
}
