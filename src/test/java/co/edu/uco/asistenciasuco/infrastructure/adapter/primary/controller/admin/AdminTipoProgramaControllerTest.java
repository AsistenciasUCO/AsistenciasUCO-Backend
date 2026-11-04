package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.ConsultarTiposProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.dto.TipoProgramaDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU154: consulta del catalogo de tipos de programa; los catalogos base son de solo consulta (John, 2026-11-04). */
class AdminTipoProgramaControllerTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final String BASE = "/api/v1/admin/tipos-programa";

    private final ConsultarTiposProgramaInputPort consultar = mock(ConsultarTiposProgramaInputPort.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new AdminTipoProgramaController(consultar))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty()))
            .build();

    private static TipoProgramaDTO tipo(final String nombre, final boolean activo) {
        return new TipoProgramaDTO(ID, "PRE", nombre, activo);
    }

    @Test
    void constructor_rechaza_input_port_nulo() {
        assertThrows(NullPointerException.class, () -> new AdminTipoProgramaController(null));
    }

    @Test
    void listar_devuelve_todos_incluidos_inactivos() throws Exception {
        when(consultar.consultarTodos()).thenReturn(List.of(tipo("pregrado", true), tipo("otro", false)));

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.length()").value(2))
                .andExpect(jsonPath("$.datos[1].activo").value(false));
    }

    @Test
    void no_expone_escritura() throws Exception {
        mvc.perform(post(BASE).contentType("application/json").content("{\"codigo\":\"PRE\",\"nombre\":\"pregrado\"}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
