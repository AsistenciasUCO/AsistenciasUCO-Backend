package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.ConsultarEstudiantePorIdInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.dto.EstudianteContextoAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.dto.EstudianteDetalleDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.ConsultarEstudiantesInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.EstudiantePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.EstudianteResumenDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EstudianteControllerTest {

    private static final java.util.UUID ACTOR_DIRECTORIO = java.util.UUID.fromString("77777777-7777-7777-7777-777777777777");

    private static final UUID ESTUDIANTE = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID USUARIO = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("33641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID INSTITUCION = UUID.fromString("43641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID FACULTAD = UUID.fromString("53641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID PROGRAMA = UUID.fromString("63641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID PLAN = UUID.fromString("73641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID ASIGNATURA = UUID.fromString("83641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID GRUPO = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void consultar_estudiantes_responde_pagina_y_no_expone_password() throws Exception {
        mockMvc().perform(get("/api/v1/estudiantes")
                        .param("tipoIdentificacionId", TIPO_IDENTIFICACION.toString())
                        .param("numeroIdentificacion", "123456789")
                        .param("nombre", "ana")
                        .param("correo", "ana.perez@uco.edu.co")
                        .param("institucionId", INSTITUCION.toString())
                        .param("facultadId", FACULTAD.toString())
                        .param("programaId", PROGRAMA.toString())
                        .param("grupoId", GRUPO.toString())
                        .param("activo", "true")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(ESTUDIANTE.toString()))
                .andExpect(jsonPath("$.items[0].correo").value("ana.perez@uco.edu.co"))
                .andExpect(jsonPath("$.items[0].password").doesNotExist())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void consultar_estudiante_por_id_responde_datos_personales_y_contexto_sin_repetir_password() throws Exception {
        mockMvc().perform(get("/api/v1/estudiantes/{estudianteId}", ESTUDIANTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datosPersonales.id").value(ESTUDIANTE.toString()))
                .andExpect(jsonPath("$.datosPersonales.password").doesNotExist())
                .andExpect(jsonPath("$.contextosAcademicos.length()").value(1))
                .andExpect(jsonPath("$.contextosAcademicos[0].idGrupo").value(GRUPO.toString()));
    }

    private MockMvc mockMvc() {
        final ConsultarEstudiantesInputPort consultarEstudiantes =
                dto -> new EstudiantePaginaDTO(List.of(resumen()), 1, 1, dto.page(), dto.size());
        final ConsultarEstudiantePorIdInputPort consultarPorId =
                consulta -> new EstudianteDetalleDTO(resumen(), List.of(contexto()));

        return MockMvcBuilders
                .standaloneSetup(new EstudianteController(consultarEstudiantes, consultarPorId, () -> ACTOR_DIRECTORIO))
                .build();
    }

    private EstudianteResumenDTO resumen() {
        return new EstudianteResumenDTO(
                ESTUDIANTE,
                USUARIO,
                TIPO_IDENTIFICACION,
                123456789,
                "PEREZ",
                "GOMEZ",
                "ANA",
                "MARIA",
                "ANA MARIA PEREZ GOMEZ",
                "ana.perez@uco.edu.co",
                true
        );
    }

    private EstudianteContextoAcademicoDTO contexto() {
        return new EstudianteContextoAcademicoDTO(
                INSTITUCION,
                "UCO",
                FACULTAD,
                "Ingenieria",
                PROGRAMA,
                "Sistemas",
                PLAN,
                "2024",
                ASIGNATURA,
                "Backend",
                GRUPO,
                "G1"
        );
    }
}
