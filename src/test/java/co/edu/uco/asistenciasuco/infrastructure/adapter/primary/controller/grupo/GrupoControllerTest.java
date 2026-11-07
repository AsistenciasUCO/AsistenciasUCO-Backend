package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo;

import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.ActualizarGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.ConsultarEstudiantesGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.dto.EstudianteGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.ConsultarGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.dto.GrupoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.CrearGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.RegistrarEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteResultadoDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GrupoControllerTest {

    private static final UUID GRUPO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID ASIGNATURA = UUID.fromString("33641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID DOCENTE = UUID.fromString("43641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO_BODY_IGNORADO = UUID.fromString("53641bab-e3cd-485c-b275-47e7b731e18c");
    private static final AuthenticatedUserResolver AUTHENTICATED_USER_RESOLVER = () -> UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void consultarGrupos_responde_lista_publica_sin_correlacion() throws Exception {
        final RegistrarEstudianteInputPort registrarPort = mock(RegistrarEstudianteInputPort.class);
        final ConsultarGruposInputPort consultarPort = mock(ConsultarGruposInputPort.class);
        when(consultarPort.execute(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(new GrupoDTO(
                GRUPO,
                "G1",
                "Grupo 1",
                ASIGNATURA,
                "Backend",
                DOCENTE,
                30,
                12,
                18,
                true,
                LocalDate.of(2026, 1, 20),
                LocalDate.of(2026, 5, 30),
                List.of()
        )));
        final MockMvc mockMvc = mockMvc(registrarPort, consultarPort);

        mockMvc.perform(get("/api/v1/grupos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos", hasSize(1)))
                .andExpect(jsonPath("$.datos[0].id").value(GRUPO.toString()))
                .andExpect(jsonPath("$.datos[0].nombreAsignatura").value("Backend"))
                .andExpect(jsonPath("$.datos[0].grupoHabilitado").value(true))
                .andExpect(jsonPath("$.datos[0].idCorrelacion").doesNotExist())
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void registrarEstudiante_toma_grupo_desde_path_y_no_expone_correlacion() throws Exception {
        final AtomicReference<RegistrarEstudianteDTO> dtoCapturado = new AtomicReference<>();
        final RegistrarEstudianteInputPort registrarPort = dto -> {
            dtoCapturado.set(dto);
            return new RegistrarEstudianteResultadoDTO(true, "Estudiante registrado.");
        };
        final ConsultarGruposInputPort consultarPort = actor -> List.of();
        final MockMvc mockMvc = mockMvc(registrarPort, consultarPort);

        mockMvc.perform(post("/api/v1/grupos/{grupoId}/estudiantes", GRUPO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.mensajeUsuario").value("Estudiante registrado."))
                .andExpect(jsonPath("$.idCorrelacion").doesNotExist());

        assertEquals(GRUPO, dtoCapturado.get().getGrupoId());
    }

    @Test
    void registrarEstudiante_ignora_grupoId_en_body_y_usa_path() throws Exception {
        final AtomicReference<RegistrarEstudianteDTO> dtoCapturado = new AtomicReference<>();
        final RegistrarEstudianteInputPort registrarPort = dto -> {
            dtoCapturado.set(dto);
            return new RegistrarEstudianteResultadoDTO(true, "Estudiante registrado.");
        };
        final MockMvc mockMvc = mockMvc(registrarPort, actor -> List.of());

        mockMvc.perform(post("/api/v1/grupos/{grupoId}/estudiantes", GRUPO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValidoConGrupoId()))
                .andExpect(status().isCreated());

        assertEquals(GRUPO, dtoCapturado.get().getGrupoId());
    }

    @Test
    void listarEstudiantesGrupo_delega_en_puerto_actual_y_no_expone_correlacion() throws Exception {
        final AtomicReference<UUID> grupoCapturado = new AtomicReference<>();
        final RegistrarEstudianteInputPort registrarPort = dto -> new RegistrarEstudianteResultadoDTO(true, "Estudiante registrado.");
        final ConsultarEstudiantesGrupoInputPort consultarEstudiantesPort = dto -> {
            grupoCapturado.set(dto.grupoId());
            return List.of(new EstudianteGrupoDTO(
                    UUID.fromString("63641bab-e3cd-485c-b275-47e7b731e18c"),
                    UUID.fromString("73641bab-e3cd-485c-b275-47e7b731e18c"),
                    "123456789",
                    "Ana Perez",
                    "ana.perez@uco.edu.co",
                    "ACTIVO",
                    "Activo"
            ));
        };

        final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new GrupoController(
                        mock(CrearGrupoInputPort.class),
                        mock(ActualizarGrupoInputPort.class),
                        registrarPort,
                        actor -> List.of(),
                        consultarEstudiantesPort,
                        AUTHENTICATED_USER_RESOLVER
                ))
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty()))
                .build();

        mockMvc.perform(get("/api/v1/grupos/{grupoId}/estudiantes", GRUPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos", hasSize(1)))
                .andExpect(jsonPath("$.datos[0].correo").value("ana.perez@uco.edu.co"))
                .andExpect(jsonPath("$.datos[0].idCorrelacion").doesNotExist())
                .andExpect(jsonPath("$.total").value(1));

        assertEquals(GRUPO, grupoCapturado.get());
    }

    @Test
    void registrarEstudiante_retorna_badRequest_cuando_grupoId_no_es_uuid() throws Exception {
        mockMvc(dto -> new RegistrarEstudianteResultadoDTO(true, "ok"), actor -> List.of())
                .perform(post("/api/v1/grupos/no-es-uuid/estudiantes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("No fue posible interpretar la solicitud. Revise el formato y el tipo de los campos enviados."));
    }

    @Test
    void registrarEstudiante_retorna_badRequest_cuando_json_es_ilegible() throws Exception {
        mockMvc(dto -> new RegistrarEstudianteResultadoDTO(true, "ok"), actor -> List.of())
                .perform(post("/api/v1/grupos/{grupoId}/estudiantes", GRUPO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo":
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No fue posible interpretar la solicitud. Revise el formato y el tipo de los campos enviados."))
                .andExpect(jsonPath("$.message", not(containsString("password"))))
                .andExpect(jsonPath("$.message", not(containsString("SQLException"))))
                .andExpect(jsonPath("$.message", not(containsString("stacktrace"))));
    }

    @Test
    void registrarEstudiante_retorna_badRequest_cuando_tipo_json_no_coincide() throws Exception {
        mockMvc(dto -> new RegistrarEstudianteResultadoDTO(true, "ok"), actor -> List.of())
                .perform(post("/api/v1/grupos/{grupoId}/estudiantes", GRUPO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoIdentificacionId": "%s",
                                  "numeroIdentificacion": "abc",
                                  "primerApellido": "Perez",
                                  "segundoApellido": "Gomez",
                                  "primerNombre": "Ana",
                                  "segundoNombre": "Maria",
                                  "correo": "ana.perez@uco.edu.co",
                                  "password": "Clave123!"
                }
                """.formatted(TIPO_IDENTIFICACION)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No fue posible interpretar la solicitud. Revise el formato y el tipo de los campos enviados."));
    }

    @Test
    void registrarEstudiante_retorna_badRequest_cuando_email_es_invalido() throws Exception {
        mockMvc(dto -> new RegistrarEstudianteResultadoDTO(true, "ok"), actor -> List.of())
                .perform(post("/api/v1/grupos/{grupoId}/estudiantes", GRUPO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyValido().replace("ana.perez@uco.edu.co", "correo-invalido")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Hay campos con informacion invalida. Revise los datos enviados."));
    }

    @Test
    void registrarEstudiante_retorna_badRequest_cuando_campo_requerido_esta_ausente() throws Exception {
        mockMvc(dto -> new RegistrarEstudianteResultadoDTO(true, "ok"), actor -> List.of())
                .perform(post("/api/v1/grupos/{grupoId}/estudiantes", GRUPO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tipoIdentificacionId": "%s",
                                  "numeroIdentificacion": 123456789,
                                  "primerApellido": "Perez",
                                  "segundoApellido": "Gomez",
                                  "segundoNombre": "Maria",
                                  "correo": "ana.perez@uco.edu.co",
                                  "password": "Clave123!"
                                }
                                """.formatted(TIPO_IDENTIFICACION)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Hay campos con informacion invalida. Revise los datos enviados."));
    }

    private MockMvc mockMvc(
            final RegistrarEstudianteInputPort registrarPort,
            final ConsultarGruposInputPort consultarPort
    ) {
        return MockMvcBuilders.standaloneSetup(new GrupoController(
                        mock(CrearGrupoInputPort.class),
                        mock(ActualizarGrupoInputPort.class),
                        registrarPort,
                        consultarPort,
                        dto -> List.of(),
                        AUTHENTICATED_USER_RESOLVER
                ))
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty()))
                .build();
    }

    private String bodyValido() {
        return """
                {
                  "tipoIdentificacionId": "%s",
                  "numeroIdentificacion": 123456789,
                  "primerApellido": "Perez",
                  "segundoApellido": "Gomez",
                  "primerNombre": "Ana",
                  "segundoNombre": "Maria",
                  "correo": "ana.perez@uco.edu.co",
                  "password": "Clave123!"
                }
                """.formatted(TIPO_IDENTIFICACION);
    }

    private String bodyValidoConGrupoId() {
        return """
                {
                  "tipoIdentificacionId": "%s",
                  "numeroIdentificacion": 123456789,
                  "primerApellido": "Perez",
                  "segundoApellido": "Gomez",
                  "primerNombre": "Ana",
                  "segundoNombre": "Maria",
                  "correo": "ana.perez@uco.edu.co",
                  "password": "Clave123!",
                  "grupoId": "%s"
                }
                """.formatted(TIPO_IDENTIFICACION, GRUPO_BODY_IGNORADO);
    }

    // CULM-001 A.1 (B-RED-03): GrupoController no expone DELETE (el stub 501 de retiro se elimina, F-COO-004).
    @Test
    void grupo_controller_no_tiene_delete_mapping() {
        final java.util.List<String> metodos = java.util.Arrays.stream(GrupoController.class.getDeclaredMethods())
                .filter(metodo -> metodo.isAnnotationPresent(org.springframework.web.bind.annotation.DeleteMapping.class))
                .map(java.lang.reflect.Method::getName)
                .toList();

        org.junit.jupiter.api.Assertions.assertTrue(metodos.isEmpty(), () -> "DeleteMapping retirado aun presente: " + metodos);
    }

    // CULM-001 A.2-2 (A2-B-04): el SP valida el ambito; un grupo ajeno o inexistente responde 403 en GET /estudiantes.
    @Test
    void estudiantes_de_un_grupo_fuera_de_ambito_responde_403() throws Exception {
        final org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders.standaloneSetup(new GrupoController(
                        mock(CrearGrupoInputPort.class),
                        mock(ActualizarGrupoInputPort.class),
                        mock(RegistrarEstudianteInputPort.class),
                        actor -> List.of(),
                        dto -> {
                            throw new co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException(
                                    "SEC_002", "No tiene ambito sobre el grupo.");
                        },
                        AUTHENTICATED_USER_RESOLVER
                ))
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> java.util.Optional.empty()))
                .build();

        mvc.perform(get("/api/v1/grupos/{grupoId}/estudiantes", GRUPO)).andExpect(status().isForbidden());
    }

    @Test
    void listar_grupos_pasa_el_ejecutor_autenticado() throws Exception {
        final AtomicReference<UUID> actor = new AtomicReference<>();
        final ConsultarGruposInputPort consultar = usuario -> {
            actor.set(usuario);
            return List.of();
        };

        mockMvc(dto -> new RegistrarEstudianteResultadoDTO(true, "ok"), consultar)
                .perform(get("/api/v1/grupos"))
                .andExpect(status().isOk());

        assertEquals(AUTHENTICATED_USER_RESOLVER.requireAuthenticatedUserId(), actor.get());
    }
}
