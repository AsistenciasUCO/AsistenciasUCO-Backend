package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.ConsultarOfertaGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.dto.ConsultarOfertaGruposDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.dto.GrupoOfertaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.dto.OfertaGruposPaginaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU008: GET /api/v1/estudiante/oferta-academica (OC-01..OC-05). */
class EstudianteOfertaAcademicaControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final String RUTA = "/api/v1/estudiante/oferta-academica";

    private final ConsultarOfertaGruposInputPort consultar = mock(ConsultarOfertaGruposInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new EstudianteOfertaAcademicaController(consultar, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    private static GrupoOfertaDTO grupo() {
        return new GrupoOfertaDTO(UUID.randomUUID(), 7, "Grupo 7", UUID.randomUUID(), "MAT1", "Algebra", "Ana Perez", UUID.randomUUID(),
                "2026-2", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 12, 10), "PROXIMO", "Lunes 08:00-10:00", 30, 25, 5, false,
                "INSTITUCIONAL", true);
    }

    @Test
    void OC01_get_usa_el_actor_autenticado_y_serializa_la_lista() throws Exception {
        when(consultar.execute(any())).thenReturn(new OfertaGruposPaginaDTO(List.of(grupo()), 9));

        mvc.perform(get(RUTA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.total").value(9))
                .andExpect(jsonPath("$.datos[0].nombreAsignatura").value("Algebra"))
                .andExpect(jsonPath("$.datos[0].cuposDisponibles").value(5))
                .andExpect(jsonPath("$.datos[0].estadoPeriodo").value("PROXIMO"))
                // CULM-001 A.1 (B-RED-08): sin idSolicitudAbierta ni estadoSolicitud (HU053-HU055 descartadas).
                .andExpect(jsonPath("$.datos[0].idSolicitudAbierta").doesNotExist())
                .andExpect(jsonPath("$.datos[0].estadoSolicitud").doesNotExist())
                .andExpect(jsonPath("$.datos[0].matriculado").value(false))
                .andExpect(jsonPath("$.datos[0].tipoAsignatura").value("INSTITUCIONAL"))
                .andExpect(jsonPath("$.datos[0].transversal").value(true));

        final ArgumentCaptor<ConsultarOfertaGruposDTO> dto = ArgumentCaptor.forClass(ConsultarOfertaGruposDTO.class);
        verify(consultar).execute(dto.capture());
        assertEquals(ACTOR, dto.getValue().actorUsuarioId());
        assertNull(dto.getValue().busqueda());
        assertNull(dto.getValue().pagina());
    }

    @Test
    void OC02_propaga_busqueda_y_paginacion() throws Exception {
        when(consultar.execute(any())).thenReturn(new OfertaGruposPaginaDTO(List.of(), 0));

        mvc.perform(get(RUTA).param("busqueda", "algebra").param("pagina", "2").param("tamano", "20")).andExpect(status().isOk());

        final ArgumentCaptor<ConsultarOfertaGruposDTO> dto = ArgumentCaptor.forClass(ConsultarOfertaGruposDTO.class);
        verify(consultar).execute(dto.capture());
        assertEquals("algebra", dto.getValue().busqueda());
        assertEquals(2, dto.getValue().pagina());
        assertEquals(20, dto.getValue().tamano());
    }

    @Test
    void OC03_validacion_del_caso_de_uso_responde_400() throws Exception {
        when(consultar.execute(any())).thenThrow(new ValidationException("El tamano de pagina debe estar entre 1 y 100."));

        mvc.perform(get(RUTA).param("tamano", "500")).andExpect(status().isBadRequest());
    }

    @Test
    void OC04_usuario_sin_estudiante_responde_403() throws Exception {
        when(consultar.execute(any())).thenThrow(new ForbiddenException("No fue posible resolver el estudiante autenticado."));

        mvc.perform(get(RUTA)).andExpect(status().isForbidden());
    }

    @Test
    void OC05_constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new EstudianteOfertaAcademicaController(null, identity));
        assertThrows(NullPointerException.class, () -> new EstudianteOfertaAcademicaController(consultar, null));
    }
}
