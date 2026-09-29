package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo;

import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.ActualizarGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto.ActualizarGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.dto.ActualizarGrupoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.ConsultarEstudiantesGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.ConsultarGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.CrearGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto.CrearGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.dto.CrearGrupoResultadoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.RegistrarEstudianteInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoints de escritura de {@link GrupoController}: el usuario ejecutor sale siempre de la
 * identidad autenticada (nunca del body) y las capacidades sin command de DB responden como
 * no disponibles en lugar de fingir exito.
 */
class GrupoControllerWriteEndpointsTest {

    private static final UUID GRUPO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID ESTUDIANTE = UUID.fromString("63641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID ASIGNATURA = UUID.fromString("33641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID PERIODO = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID DOCENTE = UUID.fromString("43641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID USUARIO_AUTENTICADO = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("53641bab-e3cd-485c-b275-47e7b731e18c");

    private CrearGrupoInputPort crearGrupoPort;
    private ActualizarGrupoInputPort actualizarGrupoPort;
    private RegistrarEstudianteInputPort registrarEstudiantePort;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        crearGrupoPort = mock(CrearGrupoInputPort.class);
        actualizarGrupoPort = mock(ActualizarGrupoInputPort.class);
        registrarEstudiantePort = mock(RegistrarEstudianteInputPort.class);
        final AuthenticatedUserResolver resolver = () -> USUARIO_AUTENTICADO;
        mockMvc = MockMvcBuilders.standaloneSetup(new GrupoController(
                        crearGrupoPort,
                        actualizarGrupoPort,
                        registrarEstudiantePort,
                        mock(ConsultarGruposInputPort.class),
                        mock(ConsultarEstudiantesGrupoInputPort.class),
                        resolver
                ))
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty()))
                .build();
    }

    @Test
    void crearGrupo_responde_201_y_usa_el_usuario_autenticado_como_ejecutor() throws Exception {
        final UUID nuevoGrupo = UUID.randomUUID();
        when(crearGrupoPort.execute(any())).thenReturn(new CrearGrupoResultadoDTO(nuevoGrupo, "Grupo creado."));

        mockMvc.perform(post("/api/v1/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idAsignatura": "%s",
                                  "idPeriodoAcademico": "%s",
                                  "codigo": 7,
                                  "nombre": "  Grupo A  ",
                                  "idDocente": "%s",
                                  "generarSesionesAutomaticas": true
                                }
                                """.formatted(ASIGNATURA, PERIODO, DOCENTE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.id").value(nuevoGrupo.toString()))
                .andExpect(jsonPath("$.datos.mensajeUsuario").value("Grupo creado."));

        final ArgumentCaptor<CrearGrupoDTO> dto = ArgumentCaptor.forClass(CrearGrupoDTO.class);
        verify(crearGrupoPort).execute(dto.capture());
        assertEquals(ASIGNATURA, dto.getValue().idAsignatura());
        assertEquals(PERIODO, dto.getValue().idPeriodoAcademico());
        assertEquals(7, dto.getValue().codigo());
        assertEquals("Grupo A", dto.getValue().nombre());
        assertEquals(DOCENTE, dto.getValue().idDocente());
        assertEquals(Boolean.TRUE, dto.getValue().generarSesionesAutomaticas());
        assertEquals(USUARIO_AUTENTICADO, dto.getValue().usuarioEjecutor());
    }

    @Test
    void crearGrupo_acepta_los_alias_de_campos_y_sin_docente() throws Exception {
        when(crearGrupoPort.execute(any())).thenReturn(new CrearGrupoResultadoDTO(UUID.randomUUID(), "ok"));

        mockMvc.perform(post("/api/v1/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "asignaturaId": "%s",
                                  "periodoAcademicoId": "%s",
                                  "codigo": 8,
                                  "nombre": "Grupo B",
                                  "crearSesionesAutomaticamente": false
                                }
                                """.formatted(ASIGNATURA, PERIODO)))
                .andExpect(status().isCreated());

        final ArgumentCaptor<CrearGrupoDTO> dto = ArgumentCaptor.forClass(CrearGrupoDTO.class);
        verify(crearGrupoPort).execute(dto.capture());
        assertEquals(ASIGNATURA, dto.getValue().idAsignatura());
        assertEquals(PERIODO, dto.getValue().idPeriodoAcademico());
        assertNull(dto.getValue().idDocente());
        assertEquals(Boolean.FALSE, dto.getValue().generarSesionesAutomaticas());
    }

    @Test
    void crearGrupo_sin_codigo_responde_400_sin_invocar_el_caso_de_uso() throws Exception {
        mockMvc.perform(post("/api/v1/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idAsignatura": "%s",
                                  "idPeriodoAcademico": "%s",
                                  "nombre": "Grupo C"
                                }
                                """.formatted(ASIGNATURA, PERIODO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ERR_CAMPO_OBLIGATORIO"));

        verifyNoInteractions(crearGrupoPort);
    }

    @Test
    void crearGrupo_con_horarios_responde_como_capacidad_no_disponible() throws Exception {
        mockMvc.perform(post("/api/v1/grupos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idAsignatura": "%s",
                                  "idPeriodoAcademico": "%s",
                                  "codigo": 9,
                                  "nombre": "Grupo D",
                                  "horaInicio": "08:00",
                                  "horaFin": "10:00"
                                }
                                """.formatted(ASIGNATURA, PERIODO)))
                .andExpect(jsonPath("$.code").value("FEATURE_UNAVAILABLE"));

        verifyNoInteractions(crearGrupoPort);
    }

    @Test
    void actualizarGrupo_responde_200_toma_el_id_del_path_y_el_ejecutor_autenticado() throws Exception {
        when(actualizarGrupoPort.execute(any())).thenReturn(new ActualizarGrupoResultadoDTO(GRUPO, "Grupo actualizado."));

        mockMvc.perform(put("/api/v1/grupos/{id}", GRUPO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo": 12,
                                  "nombre": "  Grupo Z ",
                                  "idDocente": "%s",
                                  "cupoMaximo": 35
                                }
                                """.formatted(DOCENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.id").value(GRUPO.toString()))
                .andExpect(jsonPath("$.datos.mensajeUsuario").value("Grupo actualizado."));

        final ArgumentCaptor<ActualizarGrupoDTO> dto = ArgumentCaptor.forClass(ActualizarGrupoDTO.class);
        verify(actualizarGrupoPort).execute(dto.capture());
        assertEquals(GRUPO, dto.getValue().idGrupo());
        assertEquals(12, dto.getValue().codigo());
        assertEquals("Grupo Z", dto.getValue().nombre());
        assertEquals(DOCENTE, dto.getValue().idDocente());
        assertEquals(35, dto.getValue().cupoMaximo());
        assertEquals(USUARIO_AUTENTICADO, dto.getValue().usuarioEjecutor());
    }

    @Test
    void actualizarGrupo_con_id_que_no_es_uuid_responde_400() throws Exception {
        mockMvc.perform(put("/api/v1/grupos/no-es-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(actualizarGrupoPort);
    }

    @Test
    void matricular_estudiante_existente_responde_201_y_delega_al_caso_de_uso() throws Exception {
        when(registrarEstudiantePort.execute(any())).thenReturn(
                new co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteResultadoDTO(true, "Estudiante matriculado.")
        );

        mockMvc.perform(post("/api/v1/grupos/{grupoId}/estudiantes", GRUPO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "estudianteId": "%s",
                                  "tipoIdentificacionId": "%s",
                                  "numeroIdentificacion": 123456789,
                                  "primerApellido": "Perez",
                                  "segundoApellido": "Gomez",
                                  "primerNombre": "Ana",
                                  "segundoNombre": "Maria",
                                  "correo": "ana.perez@uco.edu.co",
                                  "password": "Clave123!"
                                }
                                """.formatted(ESTUDIANTE, TIPO_IDENTIFICACION)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.mensajeUsuario").value("Estudiante matriculado."));

        verify(registrarEstudiantePort).execute(any());
    }

    @Test
    void retirar_estudiante_responde_como_capacidad_no_disponible() throws Exception {
        mockMvc.perform(delete("/api/v1/grupos/{grupoId}/estudiantes/{estudianteId}", GRUPO, ESTUDIANTE))
                .andExpect(jsonPath("$.code").value("FEATURE_UNAVAILABLE"));
    }
}
