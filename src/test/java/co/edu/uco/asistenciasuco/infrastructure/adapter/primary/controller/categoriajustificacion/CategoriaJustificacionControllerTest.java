package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.categoriajustificacion;

import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.ConsultasTransversalesInputPort;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.CategoriaJustificacionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU005/HU016: catalogo de categorias de justificacion de solo consulta. */
class CategoriaJustificacionControllerTest {

    private final ConsultasTransversalesInputPort inputPort = mock(ConsultasTransversalesInputPort.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new CategoriaJustificacionController(inputPort)).build();

    @Test
    void constructor_rechaza_puerto_nulo() {
        assertThrows(NullPointerException.class, () -> new CategoriaJustificacionController(null));
    }

    @Test
    void T_005_01_get_categorias_de_justificacion() throws Exception {
        when(inputPort.consultarCategoriasJustificacion()).thenReturn(List.of(
                new CategoriaJustificacionDTO(UUID.randomUUID(), "LAB", "Laboral", "Obligacion laboral"),
                new CategoriaJustificacionDTO(UUID.randomUUID(), "OTR", "Otro", null)));

        mvc.perform(get("/api/v1/categorias-justificacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.datos[0].codigo").value("LAB"))
                .andExpect(jsonPath("$.datos[0].descripcion").value("Obligacion laboral"))
                .andExpect(jsonPath("$.datos[1].nombre").value("Otro"));
    }

    @Test
    void T_005_02_catalogo_vacio_responde_lista_vacia() throws Exception {
        when(inputPort.consultarCategoriasJustificacion()).thenReturn(List.of());

        mvc.perform(get("/api/v1/categorias-justificacion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }
}
