package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.ConsultarFacultadDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.EstudianteFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.FacultadNodoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.FichaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.MatriculaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.PlanEstudioNodoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.ProgramaNodoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.SesionFacultadDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU104/HU105/HU109/HU119/HU145: endpoints GET de solo lectura del decano. */
class DecanoFacultadConsultaControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    private final ConsultarFacultadDecanoInputPort inputPort = mock(ConsultarFacultadDecanoInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new DecanoFacultadConsultaController(inputPort, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void T_104_01_get_estudiantes_con_filtros() throws Exception {
        when(inputPort.consultarEstudiantes(ACTOR, "ana", PROGRAMA, true, 2, 5)).thenReturn(new PaginaDTO<>(List.of(
                new EstudianteFacultadDTO(ESTUDIANTE, "1017000111", "Ana Ruiz", "ana@uco.edu.co", true, "Sistemas", 3)),
                12));

        mvc.perform(get("/api/v1/decano/estudiantes").param("busqueda", "ana").param("idPrograma", PROGRAMA.toString())
                        .param("estaActivo", "true").param("pagina", "2").param("tamano", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(12))
                .andExpect(jsonPath("$.datos[0].programas").value("Sistemas"))
                .andExpect(jsonPath("$.datos[0].totalGrupos").value(3));
    }

    @Test
    void T_104_02_sin_facultad_es_403() throws Exception {
        when(inputPort.consultarEstudiantes(eq(ACTOR), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenThrow(new ForbiddenException("No fue posible resolver la facultad del decano autenticado."));

        mvc.perform(get("/api/v1/decano/estudiantes")).andExpect(status().isForbidden());
    }

    @Test
    void T_105_01_get_ficha_del_estudiante() throws Exception {
        when(inputPort.consultarFichaEstudiante(ACTOR, ESTUDIANTE)).thenReturn(new FichaEstudianteDTO(ESTUDIANTE,
                "1017000111", "Ana Ruiz", true, List.of(new MatriculaEstudianteDTO(PROGRAMA, "Sistemas", 20241,
                UUID.randomUUID(), "Calculo", UUID.randomUUID(), "Grupo 1"))));

        mvc.perform(get("/api/v1/decano/estudiantes/{id}", ESTUDIANTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.nombreCompleto").value("Ana Ruiz"))
                .andExpect(jsonPath("$.datos.matriculas[0].nombreAsignatura").value("Calculo"));
    }

    @Test
    void T_105_02_ficha_de_otra_facultad_es_404() throws Exception {
        when(inputPort.consultarFichaEstudiante(any(), any()))
                .thenThrow(new ResourceNotFoundException("El estudiante no existe en la facultad del decano."));

        mvc.perform(get("/api/v1/decano/estudiantes/{id}", ESTUDIANTE)).andExpect(status().isNotFound());
    }

    @Test
    void T_119_01_get_estructura_en_arbol() throws Exception {
        when(inputPort.consultarEstructura(ACTOR)).thenReturn(List.of(new FacultadNodoDTO(UUID.randomUUID(),
                "Ingenieria", "Decana Uno", true, List.of(new ProgramaNodoDTO(PROGRAMA, "Sistemas", "Coord", true,
                "Pregrado", "Presencial", "Diurna", List.of(new PlanEstudioNodoDTO(UUID.randomUUID(), 20241, true, 10)))))));

        mvc.perform(get("/api/v1/decano/estructura"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].programas[0].nombre").value("Sistemas"))
                .andExpect(jsonPath("$.datos[0].programas[0].planes[0].inp").value(20241));
    }

    @Test
    void T_145_01_get_sesiones_con_rango() throws Exception {
        when(inputPort.consultarSesiones(ACTOR, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), null, null, null))
                .thenReturn(new PaginaDTO<>(List.of(new SesionFacultadDTO(UUID.randomUUID(), "Sesion 1", "S-01", 2,
                        LocalDateTime.of(2026, 2, 3, 7, 0), LocalDateTime.of(2026, 2, 3, 9, 0), UUID.randomUUID(), 1,
                        "Grupo 1", UUID.randomUUID(), "Calculo", PROGRAMA, "Sistemas")), 1));

        mvc.perform(get("/api/v1/decano/sesiones").param("desde", "2026-02-01").param("hasta", "2026-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].codigo").value("S-01"))
                .andExpect(jsonPath("$.datos[0].nombrePrograma").value("Sistemas"))
                .andExpect(jsonPath("$.datos[0].estadoTemporal").value("INDETERMINADA"))
                .andExpect(jsonPath("$.datos[0].fechaHoraInicioUtc").doesNotExist());
    }

    @Test
    void T_145_01b_sesion_confirmada_expone_instantes_utc() throws Exception {
        when(inputPort.consultarSesiones(ACTOR, null, null, null, null, null))
                .thenReturn(new PaginaDTO<>(List.of(new SesionFacultadDTO(UUID.randomUUID(), "Sesion 1", "S-01", 2,
                        LocalDateTime.of(2026, 2, 3, 13, 0), LocalDateTime.of(2026, 2, 3, 15, 0), UUID.randomUUID(), 1,
                        "Grupo 1", UUID.randomUUID(), "Calculo", PROGRAMA, "Sistemas",
                        LocalDateTime.of(2026, 2, 3, 13, 0), LocalDateTime.of(2026, 2, 3, 15, 0), "CONFIRMADA")), 1));

        mvc.perform(get("/api/v1/decano/sesiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].estadoTemporal").value("CONFIRMADA"))
                .andExpect(jsonPath("$.datos[0].fechaHoraInicioUtc").value(org.hamcrest.Matchers.endsWith("Z")))
                .andExpect(jsonPath("$.datos[0].fechaHoraInicioUtc").value(org.hamcrest.Matchers.startsWith("2026-02-03T13:00:00")))
                .andExpect(jsonPath("$.datos[0].fechaHoraFinUtc").value(org.hamcrest.Matchers.endsWith("Z")));
    }

    @Test
    void T_145_02_rango_invalido_es_400() throws Exception {
        when(inputPort.consultarSesiones(any(), any(), any(), any(), any(), any()))
                .thenThrow(new ValidationException("La fecha desde no puede ser posterior a la fecha hasta."));

        mvc.perform(get("/api/v1/decano/sesiones").param("desde", "2026-03-01").param("hasta", "2026-02-01"))
                .andExpect(status().isBadRequest());
    }
}
