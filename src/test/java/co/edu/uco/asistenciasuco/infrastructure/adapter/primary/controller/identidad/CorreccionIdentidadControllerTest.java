package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.identidad;

import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.CorreccionIdentidadInputPort;
import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto.ConfiguracionCorreccionIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto.CorregirIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto.ResultadoCorreccionIdentidadDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU159: PUT de correccion de identidad (estudiante y decano) y GET de la configuracion. */
class CorreccionIdentidadControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID REGISTRO = UUID.randomUUID();
    private static final UUID TIPO = UUID.randomUUID();
    private static final String CUERPO = "{\"primerNombre\":\" Ana \",\"segundoNombre\":\"Maria\",\"primerApellido\":\"Gomez\","
            + "\"idTipoIdentificacion\":\"" + TIPO + "\",\"numeroIdentificacion\":915900011,\"motivo\":\" Digitacion \"}";

    private final CorreccionIdentidadInputPort inputPort = mock(CorreccionIdentidadInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new CorreccionIdentidadController(inputPort, identity))
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new CorreccionIdentidadController(null, identity));
        assertThrows(NullPointerException.class, () -> new CorreccionIdentidadController(inputPort, null));
    }

    @Test
    void T_159_API_01_coordinador_corrige_al_estudiante() throws Exception {
        when(inputPort.corregir(any())).thenReturn(new ResultadoCorreccionIdentidadDTO(REGISTRO, "OK", null));

        mvc.perform(put("/api/v1/coordinador/estudiantes/" + REGISTRO + "/identidad").contentType("application/json").content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.idRegistro").value(REGISTRO.toString()))
                .andExpect(jsonPath("$.datos.sincronizacionIdentidad").value("OK"));

        final ArgumentCaptor<CorregirIdentidadDTO> captor = ArgumentCaptor.forClass(CorregirIdentidadDTO.class);
        verify(inputPort).corregir(captor.capture());
        assertEquals("ESTUDIANTE", captor.getValue().tipoUsuario());
        assertEquals("Ana", captor.getValue().primerNombre());
        assertEquals(TIPO, captor.getValue().idTipoIdentificacion());
        assertEquals(915900011, captor.getValue().numeroIdentificacion());
        assertEquals("Digitacion", captor.getValue().motivo());
        assertNull(captor.getValue().segundoApellido());
        assertEquals(ACTOR, captor.getValue().actorUsuario());
    }

    @Test
    void T_159_API_02_administrador_corrige_al_decano_solo_nombres() throws Exception {
        when(inputPort.corregir(any())).thenReturn(new ResultadoCorreccionIdentidadDTO(REGISTRO, "PENDIENTE", "Pendiente"));

        mvc.perform(put("/api/v1/admin/decanos/" + REGISTRO + "/identidad").contentType("application/json")
                        .content("{\"primerNombre\":\"Luis\",\"primerApellido\":\"Ruiz\",\"motivo\":\"Tilde\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.mensajeUsuario").value("Pendiente"));

        final ArgumentCaptor<CorregirIdentidadDTO> captor = ArgumentCaptor.forClass(CorregirIdentidadDTO.class);
        verify(inputPort).corregir(captor.capture());
        assertEquals("DECANO", captor.getValue().tipoUsuario());
        assertNull(captor.getValue().numeroIdentificacion());
    }

    @Test
    void T_159_API_03_validaciones_de_forma_devuelven_400_sin_llamar_al_caso_de_uso() throws Exception {
        final String ruta = "/api/v1/coordinador/estudiantes/" + REGISTRO + "/identidad";
        // Sin motivo
        mvc.perform(put(ruta).contentType("application/json").content("{\"primerNombre\":\"Ana\",\"primerApellido\":\"Gomez\"}"))
                .andExpect(status().isBadRequest());
        // Tipo sin numero
        mvc.perform(put(ruta).contentType("application/json").content("{\"primerNombre\":\"Ana\",\"primerApellido\":\"Gomez\","
                        + "\"idTipoIdentificacion\":\"" + TIPO + "\",\"motivo\":\"x\"}"))
                .andExpect(status().isBadRequest());
        // Numero no positivo y nombre obligatorio vacio
        mvc.perform(put(ruta).contentType("application/json").content("{\"primerNombre\":\" \",\"primerApellido\":\"Gomez\","
                        + "\"idTipoIdentificacion\":\"" + TIPO + "\",\"numeroIdentificacion\":0,\"motivo\":\"x\"}"))
                .andExpect(status().isBadRequest());
        // Motivo de mas de 500 caracteres
        mvc.perform(put(ruta).contentType("application/json").content("{\"primerNombre\":\"Ana\",\"primerApellido\":\"Gomez\","
                        + "\"motivo\":\"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(inputPort);
    }

    @Test
    void T_159_API_04_configuracion_indica_si_el_documento_es_corregible() throws Exception {
        when(inputPort.configuracion()).thenReturn(new ConfiguracionCorreccionIdentidadDTO(false));

        mvc.perform(get("/api/v1/configuracion/identidad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.permitirCorregirDocumento").value(false));
    }
}
