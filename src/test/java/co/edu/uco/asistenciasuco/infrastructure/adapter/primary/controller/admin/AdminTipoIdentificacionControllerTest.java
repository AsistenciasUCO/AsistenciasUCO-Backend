package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.ConsultarTiposIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.primaryports.GuardarTipoIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.primaryports.dto.GuardarTipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU153: alta y edicion del nombre de tipos de documento (ADMINISTRADOR). */
class AdminTipoIdentificacionControllerTest {

    private static final UUID ACTOR = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final String BASE = "/api/v1/admin/tipos-identificacion";
    private static final String JSON = "application/json";

    private final GuardarTipoIdentificacionInputPort guardar = mock(GuardarTipoIdentificacionInputPort.class);
    private final ConsultarTiposIdentificacionInputPort consultar = mock(ConsultarTiposIdentificacionInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new AdminTipoIdentificacionController(guardar, consultar, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty()))
            .build();

    @Test
    void T_153_02_post_crea_y_devuelve_201_con_el_tipo_creado() throws Exception {
        when(consultar.execute()).thenReturn(List.of(new TipoIdentificacionDTO(ID, "PEP", "Permiso especial")));

        mvc.perform(post(BASE).contentType(JSON).content("{\"codigo\":\" pep \",\"nombre\":\"Permiso especial\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.id").value(ID.toString()))
                .andExpect(jsonPath("$.datos.tipoIdentificacion").value("PEP"));

        verify(guardar).execute(new GuardarTipoIdentificacionDTO(null, " pep ", "Permiso especial", ACTOR));
    }

    @Test
    void T_153_03_put_edita_el_nombre_y_relee_el_tipo() throws Exception {
        when(consultar.execute()).thenReturn(List.of(new TipoIdentificacionDTO(ID, "PA", "Pasaporte vigente")));

        mvc.perform(put(BASE + "/" + ID).contentType(JSON).content("{\"nombre\":\"Pasaporte vigente\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.nombre").value("Pasaporte vigente"));

        verify(guardar).execute(new GuardarTipoIdentificacionDTO(ID, null, "Pasaporte vigente", ACTOR));
    }

    @Test
    void T_153_04_duplicado_es_409_y_tipo_no_releido_es_404() throws Exception {
        doThrow(new ConflictException("Ya existe")).when(guardar).execute(any());
        mvc.perform(post(BASE).contentType(JSON).content("{\"codigo\":\"CC\",\"nombre\":\"Cedula\"}"))
                .andExpect(status().isConflict());

        final GuardarTipoIdentificacionInputPort sinError = mock(GuardarTipoIdentificacionInputPort.class);
        when(consultar.execute()).thenReturn(List.of());
        MockMvcBuilders.standaloneSetup(new AdminTipoIdentificacionController(sinError, consultar, identity))
                .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build()
                .perform(put(BASE + "/" + ID).contentType(JSON).content("{\"nombre\":\"Pasaporte\"}"))
                .andExpect(status().isNotFound());
    }
}
