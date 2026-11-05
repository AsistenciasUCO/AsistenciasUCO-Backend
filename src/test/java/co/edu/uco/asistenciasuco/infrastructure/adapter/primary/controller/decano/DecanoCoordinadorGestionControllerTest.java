package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.decano;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.ActualizarCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.dto.ActualizarCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports.CambiarEstadoCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports.dto.CambiarEstadoCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ResultadoGestionCoordinadorDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU121/HU122 (CONTRACT HU121-HU122 §3.3): {@code PUT /{id}} y {@code PATCH /{id}/estado} de
 * {@code /api/v1/decano/coordinadores}, en un controlador propio (D10).
 */
class DecanoCoordinadorGestionControllerTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID COORDINADOR = UUID.randomUUID();
    private static final String RUTA = "/api/v1/decano/coordinadores/";
    private static final String CUERPO_VALIDO = "{\"primerNombre\":\"Ana\",\"segundoNombre\":\"Maria\","
            + "\"primerApellido\":\"Perez\",\"segundoApellido\":\"Gomez\",\"correo\":\"ana@uco.edu.co\"}";

    private final ActualizarCoordinadorInputPort actualizar = mock(ActualizarCoordinadorInputPort.class);
    private final CambiarEstadoCoordinadorInputPort cambiarEstado = mock(CambiarEstadoCoordinadorInputPort.class);
    private final AuthenticatedUserResolver identity = () -> ACTOR;
    private final DecanoCoordinadorGestionController controller =
            new DecanoCoordinadorGestionController(actualizar, cambiarEstado, identity);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler(codigo -> Optional.empty())).build();

    @Test
    void actualizar_mapea_cuerpo_ruta_y_actor_al_dto_y_serializa_el_resultado() throws Exception {
        when(actualizar.execute(any(ActualizarCoordinadorDTO.class)))
                .thenReturn(new ResultadoGestionCoordinadorDTO(COORDINADOR, "OK", null));

        mvc.perform(put(RUTA + COORDINADOR).contentType("application/json").content(CUERPO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.idCoordinador").value(COORDINADOR.toString()))
                .andExpect(jsonPath("$.datos.sincronizacionIdentidad").value("OK"));

        final ArgumentCaptor<ActualizarCoordinadorDTO> captor = ArgumentCaptor.forClass(ActualizarCoordinadorDTO.class);
        verify(actualizar).execute(captor.capture());
        assertEquals(COORDINADOR, captor.getValue().idCoordinador());
        assertEquals("Ana", captor.getValue().primerNombre());
        assertEquals("Maria", captor.getValue().segundoNombre());
        assertEquals("Perez", captor.getValue().primerApellido());
        assertEquals("Gomez", captor.getValue().segundoApellido());
        assertEquals("ana@uco.edu.co", captor.getValue().correo());
        assertEquals(ACTOR, captor.getValue().usuario());
    }

    @Test
    void actualizar_con_identidad_pendiente_expone_la_advertencia_del_backend() throws Exception {
        when(actualizar.execute(any(ActualizarCoordinadorDTO.class)))
                .thenReturn(new ResultadoGestionCoordinadorDTO(COORDINADOR, "PENDIENTE", "Se sincronizara pronto."));

        mvc.perform(put(RUTA + COORDINADOR).contentType("application/json").content(CUERPO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.sincronizacionIdentidad").value("PENDIENTE"))
                .andExpect(jsonPath("$.datos.mensajeUsuario").value("Se sincronizara pronto."));
    }

    @Test
    void actualizar_acepta_segundos_nombre_y_apellido_ausentes() throws Exception {
        when(actualizar.execute(any(ActualizarCoordinadorDTO.class)))
                .thenReturn(new ResultadoGestionCoordinadorDTO(COORDINADOR, "OK", null));
        final String cuerpo = "{\"primerNombre\":\"Ana\",\"primerApellido\":\"Perez\",\"correo\":\"ana@uco.edu.co\"}";

        mvc.perform(put(RUTA + COORDINADOR).contentType("application/json").content(cuerpo))
                .andExpect(status().isOk());

        final ArgumentCaptor<ActualizarCoordinadorDTO> captor = ArgumentCaptor.forClass(ActualizarCoordinadorDTO.class);
        verify(actualizar).execute(captor.capture());
        assertNull(captor.getValue().segundoNombre());
        assertNull(captor.getValue().segundoApellido());
    }

    @Test
    void actualizar_rechaza_con_400_sin_llamar_al_caso_de_uso_los_datos_invalidos() throws Exception {
        final String[] cuerpos = {
                "{\"primerNombre\":\"   \",\"primerApellido\":\"Perez\",\"correo\":\"ana@uco.edu.co\"}",
                "{\"primerNombre\":\"Ana\",\"primerApellido\":\"\",\"correo\":\"ana@uco.edu.co\"}",
                "{\"primerNombre\":\"Ana\",\"primerApellido\":\"Perez\",\"correo\":\"no-es-correo\"}",
                "{\"primerNombre\":\"Ana\",\"primerApellido\":\"Perez\"}",
                "{\"primerNombre\":\"" + "A".repeat(51) + "\",\"primerApellido\":\"Perez\",\"correo\":\"ana@uco.edu.co\"}"
        };
        for (final String cuerpo : cuerpos) {
            mvc.perform(put(RUTA + COORDINADOR).contentType("application/json").content(cuerpo))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(actualizar);
    }

    @Test
    void actualizar_rechaza_con_400_un_id_que_no_es_uuid() throws Exception {
        mvc.perform(put(RUTA + "no-es-uuid").contentType("application/json").content(CUERPO_VALIDO))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(actualizar);
    }

    @Test
    void actualizar_traduce_los_errores_de_negocio_a_404_409_y_403() throws Exception {
        when(actualizar.execute(any(ActualizarCoordinadorDTO.class)))
                .thenThrow(new ResourceNotFoundException("Coordinador no existe."))
                .thenThrow(new ConflictException("Correo ya registrado."))
                .thenThrow(new ForbiddenException("Sin facultad de decano."));

        mvc.perform(put(RUTA + COORDINADOR).contentType("application/json").content(CUERPO_VALIDO))
                .andExpect(status().isNotFound());
        mvc.perform(put(RUTA + COORDINADOR).contentType("application/json").content(CUERPO_VALIDO))
                .andExpect(status().isConflict());
        mvc.perform(put(RUTA + COORDINADOR).contentType("application/json").content(CUERPO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    void cambiarEstado_desactiva_con_estado_explicito_y_actor_autenticado() throws Exception {
        when(cambiarEstado.execute(any(CambiarEstadoCoordinadorDTO.class)))
                .thenReturn(new ResultadoGestionCoordinadorDTO(COORDINADOR, "OK", null));

        mvc.perform(patch(RUTA + COORDINADOR + "/estado").contentType("application/json").content("{\"estaActivo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.idCoordinador").value(COORDINADOR.toString()));

        final ArgumentCaptor<CambiarEstadoCoordinadorDTO> captor = ArgumentCaptor.forClass(CambiarEstadoCoordinadorDTO.class);
        verify(cambiarEstado).execute(captor.capture());
        assertEquals(COORDINADOR, captor.getValue().idCoordinador());
        assertEquals(false, captor.getValue().estaActivo());
        assertEquals(ACTOR, captor.getValue().usuario());
    }

    @Test
    void cambiarEstado_reactiva_con_estaActivo_true() throws Exception {
        when(cambiarEstado.execute(any(CambiarEstadoCoordinadorDTO.class)))
                .thenReturn(new ResultadoGestionCoordinadorDTO(COORDINADOR, "OK", null));

        mvc.perform(patch(RUTA + COORDINADOR + "/estado").contentType("application/json").content("{\"estaActivo\":true}"))
                .andExpect(status().isOk());

        final ArgumentCaptor<CambiarEstadoCoordinadorDTO> captor = ArgumentCaptor.forClass(CambiarEstadoCoordinadorDTO.class);
        verify(cambiarEstado).execute(captor.capture());
        assertTrue(captor.getValue().estaActivo());
    }

    @Test
    void cambiarEstado_no_asume_toggle_cuando_falta_estaActivo() throws Exception {
        mvc.perform(patch(RUTA + COORDINADOR + "/estado").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch(RUTA + COORDINADOR + "/estado").contentType("application/json").content("{\"estaActivo\":null}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cambiarEstado);
    }

    @Test
    void cambiarEstado_rechaza_con_400_un_cuerpo_json_null() throws Exception {
        mvc.perform(patch(RUTA + COORDINADOR + "/estado")
                        .contentType("application/json")
                        .content("null"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cambiarEstado);
    }

    @Test
    void cambiarEstado_responde_409_cuando_el_coordinador_tiene_programa_activo() throws Exception {
        when(cambiarEstado.execute(any(CambiarEstadoCoordinadorDTO.class)))
                .thenThrow(new ConflictException("COO_001"));

        mvc.perform(patch(RUTA + COORDINADOR + "/estado").contentType("application/json").content("{\"estaActivo\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    void la_ruta_toggle_ya_no_existe() {
        final boolean existeToggle = java.util.Arrays.stream(DecanoCoordinadorGestionController.class.getDeclaredMethods())
                .map(metodo -> metodo.getAnnotation(org.springframework.web.bind.annotation.PatchMapping.class))
                .filter(java.util.Objects::nonNull)
                .flatMap(anotacion -> java.util.Arrays.stream(anotacion.value()))
                .anyMatch(ruta -> ruta.contains("toggle"));
        final boolean portalTienePatch = java.util.Arrays.stream(DecanoPortalController.class.getDeclaredMethods())
                .anyMatch(metodo -> metodo.isAnnotationPresent(org.springframework.web.bind.annotation.PatchMapping.class));

        // El estado se cambia con PATCH .../estado y estaActivo explicito; el stub 501 del portal tambien se retiro.
        assertFalse(existeToggle);
        assertFalse(portalTienePatch);
        verifyNoInteractions(cambiarEstado);
    }

    @Test
    void HU159_actualizar_con_documento_exige_motivo_y_lo_envia_al_caso_de_uso() throws Exception {
        final UUID tipo = UUID.randomUUID();
        final String sinMotivo = "{\"primerNombre\":\"Ana\",\"primerApellido\":\"Perez\",\"correo\":\"ana@uco.edu.co\","
                + "\"idTipoIdentificacion\":\"" + tipo + "\",\"numeroIdentificacion\":915900021}";
        mvc.perform(put(RUTA + COORDINADOR).contentType("application/json").content(sinMotivo))
                .andExpect(status().isBadRequest());

        when(actualizar.execute(any(ActualizarCoordinadorDTO.class)))
                .thenReturn(new ResultadoGestionCoordinadorDTO(COORDINADOR, "OK", null));
        mvc.perform(put(RUTA + COORDINADOR).contentType("application/json")
                        .content(sinMotivo.replace("}", ",\"motivo\":\"Digitacion\"}")))
                .andExpect(status().isOk());

        final ArgumentCaptor<ActualizarCoordinadorDTO> captor = ArgumentCaptor.forClass(ActualizarCoordinadorDTO.class);
        verify(actualizar).execute(captor.capture());
        assertEquals(tipo, captor.getValue().idTipoIdentificacion());
        assertEquals(915900021, captor.getValue().numeroIdentificacion());
        assertEquals("Digitacion", captor.getValue().motivo());
    }
}
