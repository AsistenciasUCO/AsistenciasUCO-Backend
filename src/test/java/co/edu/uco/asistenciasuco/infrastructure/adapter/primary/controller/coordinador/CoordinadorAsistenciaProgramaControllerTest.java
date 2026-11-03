package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.ConsultarAsistenciaProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.AsistenciaSesionDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.GrupoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.SesionGrupoDTO;
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

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU064/HU060/HU061: endpoints GET de solo lectura del coordinador. */
class CoordinadorAsistenciaProgramaControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID SESION = UUID.randomUUID();

    private final ConsultarAsistenciaProgramaInputPort inputPort = mock(ConsultarAsistenciaProgramaInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new CoordinadorAsistenciaProgramaController(inputPort, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void T_064_01_get_grupos_usa_actor_y_filtros() throws Exception {
        when(inputPort.consultarGrupos(ACTOR, PERIODO, "calc", 2, 5)).thenReturn(new PaginaDTO<>(List.of(
                new GrupoProgramaDTO(GRUPO, 1, "Grupo 1", UUID.randomUUID(), "Calculo", UUID.randomUUID(), 30, 20, 10,
                        true, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 6, 5))), 9, 2, 5));

        mvc.perform(get("/api/v1/coordinador/grupos").param("idPeriodo", PERIODO.toString())
                        .param("busqueda", "calc").param("pagina", "2").param("tamano", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(9))
                .andExpect(jsonPath("$.datos[0].nombreAsignatura").value("Calculo"))
                .andExpect(jsonPath("$.datos[0].cuposDisponibles").value(10));
    }

    @Test
    void T_064_02_get_grupos_sin_parametros_delega_nulos() throws Exception {
        when(inputPort.consultarGrupos(eq(ACTOR), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(new PaginaDTO<>(List.of(), 0, 1, 10));

        mvc.perform(get("/api/v1/coordinador/grupos")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void T_064_03_sin_programa_es_403() throws Exception {
        when(inputPort.consultarGrupos(any(), any(), any(), any(), any()))
                .thenThrow(new ForbiddenException("No fue posible resolver el programa del coordinador autenticado."));

        mvc.perform(get("/api/v1/coordinador/grupos")).andExpect(status().isForbidden());
    }

    @Test
    void T_060_01_get_sesiones_del_grupo_con_rango() throws Exception {
        when(inputPort.consultarSesionesGrupo(ACTOR, GRUPO, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), null,
                null)).thenReturn(new PaginaDTO<>(List.of(new SesionGrupoDTO(SESION, GRUPO, 1, "Grupo 1", "Sesion 1", 1,
                "S-01", 2, LocalDateTime.of(2026, 2, 3, 7, 0), LocalDateTime.of(2026, 2, 3, 9, 0), null, null,
                "INDETERMINADA")), 1, 1, 10));

        mvc.perform(get("/api/v1/coordinador/grupos/{id}/sesiones", GRUPO)
                        .param("desde", "2026-02-01").param("hasta", "2026-02-28"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].codigo").value("S-01"))
                .andExpect(jsonPath("$.datos[0].numeroSemana").value(2));
    }

    @Test
    void T_APP04_01_sesion_confirmada_agrega_instantes_utc_sin_alterar_el_wire_v1() throws Exception {
        final LocalDateTime inicio = LocalDateTime.of(2026, 11, 3, 15, 0, 0, 123_456_700);
        final LocalDateTime fin = LocalDateTime.of(2026, 11, 3, 17, 0);
        when(inputPort.consultarSesionesGrupo(eq(ACTOR), eq(GRUPO), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(new PaginaDTO<>(List.of(new SesionGrupoDTO(SESION, GRUPO, 1, "Grupo 1", "Sesion 1", 1,
                        "S-01", 2, inicio, fin, inicio, fin, "CONFIRMADA")), 1, 1, 10));

        mvc.perform(get("/api/v1/coordinador/grupos/{id}/sesiones", GRUPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].fechaHoraInicio").value("2026-11-03T15:00:00.1234567"))
                .andExpect(jsonPath("$.datos[0].fechaHoraInicioUtc").value("2026-11-03T15:00:00.1234567Z"))
                .andExpect(jsonPath("$.datos[0].fechaHoraFinUtc").value("2026-11-03T17:00:00Z"))
                .andExpect(jsonPath("$.datos[0].estadoTemporal").value("CONFIRMADA"));
    }

    @Test
    void T_APP04_02_sesion_indeterminada_serializa_instantes_nulos_explicitos() throws Exception {
        when(inputPort.consultarSesionesGrupo(eq(ACTOR), eq(GRUPO), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(new PaginaDTO<>(List.of(new SesionGrupoDTO(SESION, GRUPO, 1, "Grupo 1", "Sesion 1", 1,
                        "S-01", 2, LocalDateTime.of(2026, 2, 3, 7, 0), LocalDateTime.of(2026, 2, 3, 9, 0), null, null,
                        "INDETERMINADA")), 1, 1, 10));

        mvc.perform(get("/api/v1/coordinador/grupos/{id}/sesiones", GRUPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].fechaHoraInicio").value("2026-02-03T07:00:00"))
                .andExpect(jsonPath("$.datos[0].fechaHoraInicioUtc").value(nullValue()))
                .andExpect(jsonPath("$.datos[0].fechaHoraFinUtc").value(nullValue()))
                .andExpect(jsonPath("$.datos[0].estadoTemporal").value("INDETERMINADA"));
    }

    @Test
    void T_060_02_rango_invalido_es_400() throws Exception {
        when(inputPort.consultarSesionesGrupo(any(), any(), any(), any(), any(), any()))
                .thenThrow(new ValidationException("La fecha desde no puede ser posterior a la fecha hasta."));

        mvc.perform(get("/api/v1/coordinador/grupos/{id}/sesiones", GRUPO)
                        .param("desde", "2026-03-01").param("hasta", "2026-02-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void T_061_01_get_asistencias_de_la_sesion_con_filtro() throws Exception {
        when(inputPort.consultarAsistenciasSesion(ACTOR, SESION, false, 1, 20)).thenReturn(new PaginaDTO<>(List.of(
                new AsistenciaSesionDTO(UUID.randomUUID(), SESION, "Sesion 1", UUID.randomUUID(), "1017000111",
                        "Ana Ruiz", false, "INJ", "Injustificada", LocalDateTime.of(2026, 2, 3, 7, 0),
                        LocalDateTime.of(2026, 2, 3, 9, 0))), 1, 1, 20));

        mvc.perform(get("/api/v1/coordinador/sesiones/{id}/asistencias", SESION)
                        .param("asistio", "false").param("pagina", "1").param("tamano", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos[0].nombreCompleto").value("Ana Ruiz"))
                .andExpect(jsonPath("$.datos[0].asistio").value(false))
                .andExpect(jsonPath("$.datos[0].codigoRazonCausa").value("INJ"));
        verify(inputPort).consultarAsistenciasSesion(ACTOR, SESION, false, 1, 20);
    }
}
