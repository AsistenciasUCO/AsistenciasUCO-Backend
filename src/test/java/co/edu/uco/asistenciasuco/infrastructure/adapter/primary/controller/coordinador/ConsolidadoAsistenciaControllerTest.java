package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.ConsultasTransversalesInputPort;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.ComponenteFormacionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.ConsolidadoGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FiltroConsolidadoDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU061 (consolidado del coordinador y del decano) y HU147 (componentes de formacion). */
class ConsolidadoAsistenciaControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();

    private final ConsultasTransversalesInputPort inputPort = mock(ConsultasTransversalesInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new ConsolidadoAsistenciaController(inputPort, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void T_061_02_get_consolidado_del_coordinador_y_del_decano() throws Exception {
        when(inputPort.consultarConsolidadoAsistencia(ACTOR, new FiltroConsolidadoDTO(ID, null, null, ID), 1, 20))
                .thenReturn(new PaginaDTO<>(List.of(new ConsolidadoGrupoDTO(ID, 1, "G1", ID, "2026-2", ID,
                        "Sistemas", ID, "Calculo", ID, "Ana", 20, 8, 150, 10, 0, new BigDecimal("93.75"),
                        new BigDecimal("6.25"), 2, 1)), 1));

        for (final String ruta : List.of("/api/v1/coordinador/consolidado-asistencia",
                "/api/v1/decano/consolidado-asistencia")) {
            mvc.perform(get(ruta).param("idPeriodoAcademico", ID.toString()).param("idDocente", ID.toString())
                            .param("pagina", "1").param("tamano", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(1))
                    .andExpect(jsonPath("$.datos[0].porcentajeInasistencia").value(6.25))
                    .andExpect(jsonPath("$.datos[0].estudiantesRiesgo").value(2));
        }
    }

    @Test
    void T_061_03_paginacion_invalida_es_400() throws Exception {
        when(inputPort.consultarConsolidadoAsistencia(eq(ACTOR), any(), eq(0), any()))
                .thenThrow(new ValidationException("La pagina debe ser mayor o igual a 1."));

        mvc.perform(get("/api/v1/coordinador/consolidado-asistencia").param("pagina", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void T_147_02_get_componentes_de_formacion() throws Exception {
        when(inputPort.consultarComponentes(ACTOR)).thenReturn(List.of(
                new ComponenteFormacionDTO(ID, "BAS", "Basicas", 2),
                new ComponenteFormacionDTO(UUID.randomUUID(), "ESP", "Especificas", 5)));

        mvc.perform(get("/api/v1/coordinador/componentes-formacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.datos[1].codigo").value("ESP"));
    }
}
