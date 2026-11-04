package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.ConsultasTransversalesInputPort;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.AsignaturaInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.EstadoSistemaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.EstudianteInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.EventoAuditoriaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FichaUnificadaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FiltroAuditoriaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.MatriculaHistorialDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU143/HU157/HU172: endpoints GET de solo lectura del administrador. */
class AdminConsultasTransversalesControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();

    private final ConsultasTransversalesInputPort inputPort = mock(ConsultasTransversalesInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new AdminConsultasTransversalesController(inputPort, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void T_143_02_get_asignaturas_de_la_institucion_con_facultad() throws Exception {
        when(inputPort.buscarAsignaturas(ACTOR, "cal", ID, null, 2, 10)).thenReturn(new PaginaDTO<>(List.of(
                new AsignaturaInstitucionDTO(ID, "CAL1", "Calculo", 3, "Ciencias", "Basico", "S1", ID, 2024, ID,
                        "Sistemas", ID, "Ingenieria", true)), 15));

        mvc.perform(get("/api/v1/admin/asignaturas").param("busqueda", "cal").param("idFacultad", ID.toString())
                        .param("pagina", "2").param("tamano", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(15))
                .andExpect(jsonPath("$.datos[0].nombreFacultad").value("Ingenieria"));
    }

    @Test
    void T_172_02_get_auditoria_convierte_fechas_y_filtros() throws Exception {
        final OffsetDateTime desde = OffsetDateTime.parse("2026-11-01T05:00:00Z");
        final OffsetDateTime hasta = OffsetDateTime.parse("2026-11-04T05:00:00Z");
        when(inputPort.consultarAuditoria(ACTOR, new FiltroAuditoriaDTO(desde, hasta, "CREAR_SESION", null, null, null,
                null, false), 1, 25)).thenReturn(new PaginaDTO<>(List.of(new EventoAuditoriaDTO(ID, hasta,
                ACTOR.toString(), "USER", "Ana", "CREAR_SESION", "SESION", "S1", "SUCCESS", ID, "POST",
                "/api/v1/sesiones", 201, null, null, null, true)), 1));

        mvc.perform(get("/api/v1/admin/auditoria").param("desde", "2026-11-01T05:00:00Z")
                        .param("hasta", "2026-11-04T05:00:00Z").param("accion", "CREAR_SESION")
                        .param("soloAsistencia", "false").param("pagina", "1").param("tamano", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.datos[0].nombreActor").value("Ana"))
                .andExpect(jsonPath("$.datos[0].accionAsistencia").value(true));
    }

    @Test
    void T_157_03_get_estudiantes_de_la_institucion() throws Exception {
        when(inputPort.buscarEstudiantes(ACTOR, "101", null, true, null, null)).thenReturn(new PaginaDTO<>(List.of(
                new EstudianteInstitucionDTO(ID, "101", "Ana", "ana@uco.edu.co", true, "Ingenieria", "Sistemas", 2)),
                1));

        mvc.perform(get("/api/v1/admin/estudiantes").param("busqueda", "101").param("estaActivo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].facultades").value("Ingenieria"))
                .andExpect(jsonPath("$.datos[0].totalGrupos").value(2));
    }

    @Test
    void T_157_04_get_historial_sin_grupos() throws Exception {
        when(inputPort.consultarHistorial(ACTOR, ID)).thenReturn(new FichaUnificadaDTO(ID, "CC", "101", "Ana Ruiz",
                null, true, List.of(new MatriculaHistorialDTO(ID, "Ingenieria", ID, "Sistemas", 2024, null, null,
                null, null, null, null, null, null, null, null))));

        mvc.perform(get("/api/v1/admin/estudiantes/{id}/historial", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.matriculas[0].nombrePrograma").value("Sistemas"))
                .andExpect(jsonPath("$.datos.matriculas[0].idGrupo").doesNotExist());
    }

    @Test
    void T_157_05_sin_perfil_administrador_es_403() throws Exception {
        when(inputPort.consultarHistorial(ACTOR, ID))
                .thenThrow(new ForbiddenException("SEC_001", "No tiene el perfil requerido para esta consulta."));

        mvc.perform(get("/api/v1/admin/estudiantes/{id}/historial", ID)).andExpect(status().isForbidden());
    }

    @Test
    void T_155_02_get_estados_del_sistema() throws Exception {
        when(inputPort.consultarEstadosSistema()).thenReturn(List.of(
                new EstadoSistemaDTO("SOLICITUD", ID, "P", "pendiente"),
                new EstadoSistemaDTO("ESTUDIANTE_GRUPO", ID, "A", "activo")));

        mvc.perform(get("/api/v1/admin/estados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.datos[1].catalogo").value("ESTUDIANTE_GRUPO"));
    }
}
