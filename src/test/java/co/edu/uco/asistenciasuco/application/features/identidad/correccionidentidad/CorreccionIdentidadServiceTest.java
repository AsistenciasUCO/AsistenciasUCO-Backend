package co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto.CorregirIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.identidad.correccionidentidad.primaryports.dto.ResultadoCorreccionIdentidadDTO;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.ResultadoSincronizacion;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.SincronizarCuentaInstitucionalService;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.CorreccionIdentidadCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.CorreccionIdentidadCommandPort.NombresCorregidos;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.CorreccionIdentidadCommandPort.ResultadoCorreccionIdentidad;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.CorreccionIdentidadCommandPort.TipoUsuarioCorregible;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.ActualizarCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU159: correccion de identidad de estudiantes (coordinador) y decanos (administrador). */
class CorreccionIdentidadServiceTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID REGISTRO = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID TIPO = UUID.randomUUID();

    private final CorreccionIdentidadCommandPort commandPort = mock(CorreccionIdentidadCommandPort.class);
    private final SincronizarCuentaInstitucionalService sincronizacion = mock(SincronizarCuentaInstitucionalService.class);
    private final MessageCatalogPort messageCatalog = mock(MessageCatalogPort.class);
    private final ParameterCatalogPort parameterCatalog = mock(ParameterCatalogPort.class);
    private final CorreccionIdentidadService service =
            new CorreccionIdentidadService(commandPort, sincronizacion, messageCatalog, parameterCatalog);

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new CorreccionIdentidadService(null, sincronizacion, messageCatalog, parameterCatalog));
        assertThrows(NullPointerException.class, () -> new CorreccionIdentidadService(commandPort, null, messageCatalog, parameterCatalog));
        assertThrows(NullPointerException.class, () -> new CorreccionIdentidadService(commandPort, sincronizacion, null, parameterCatalog));
        assertThrows(NullPointerException.class, () -> new CorreccionIdentidadService(commandPort, sincronizacion, messageCatalog, null));
    }

    @Test
    void T_159_SVC_01_corrige_el_documento_del_estudiante_y_sincroniza_el_username() {
        when(commandPort.corregir(eq(TipoUsuarioCorregible.ESTUDIANTE), eq(REGISTRO), any(), any(), eq(ACTOR)))
                .thenReturn(new ResultadoCorreccionIdentidad(USUARIO, "ana@uco.edu.co", 915900011));
        when(sincronizacion.actualizarDatos(eq(USUARIO), any())).thenReturn(ResultadoSincronizacion.OK);

        final ResultadoCorreccionIdentidadDTO resultado = service.corregir(dto("ESTUDIANTE", TIPO, 915900011));

        assertEquals(REGISTRO, resultado.idRegistro());
        assertEquals(ResultadoCorreccionIdentidadDTO.SINCRONIZACION_OK, resultado.sincronizacionIdentidad());
        assertNull(resultado.mensajeUsuario());
        verify(commandPort).corregir(TipoUsuarioCorregible.ESTUDIANTE, REGISTRO,
                new NombresCorregidos("Ana", "Maria", "Gomez", null), new CorreccionDocumentoDTO(TIPO, 915900011, "Digitacion"), ACTOR);
        verify(sincronizacion).actualizarDatos(USUARIO, new ActualizarCuentaIdentidadDTO("ana@uco.edu.co", "Ana", "Gomez", "915900011"));
    }

    @Test
    void T_159_SVC_02_sin_documento_no_cambia_el_username_y_un_fallo_del_proveedor_queda_pendiente() {
        when(commandPort.corregir(eq(TipoUsuarioCorregible.DECANO), eq(REGISTRO), any(), any(), eq(ACTOR)))
                .thenReturn(new ResultadoCorreccionIdentidad(USUARIO, "decano@uco.edu.co", 123));
        when(sincronizacion.actualizarDatos(eq(USUARIO), any())).thenReturn(ResultadoSincronizacion.PENDIENTE);
        when(messageCatalog.getUserMessage(CorreccionIdentidadService.CODIGO_ADVERTENCIA_IDENTIDAD)).thenReturn("Pendiente");

        final ResultadoCorreccionIdentidadDTO resultado = service.corregir(dto("DECANO", null, null));

        assertEquals(ResultadoCorreccionIdentidadDTO.SINCRONIZACION_PENDIENTE, resultado.sincronizacionIdentidad());
        assertEquals("Pendiente", resultado.mensajeUsuario());
        verify(sincronizacion).actualizarDatos(USUARIO, new ActualizarCuentaIdentidadDTO("decano@uco.edu.co", "Ana", "Gomez", null));
    }

    @Test
    void T_159_SVC_03_tipo_no_soportado_o_dto_nulo_se_rechazan_sin_tocar_la_db() {
        assertThrows(ValidationException.class, () -> service.corregir(dto("ADMINISTRADOR", null, null)));
        assertThrows(CrosscuttingException.class, () -> service.corregir(null));
        verifyNoInteractions(commandPort, sincronizacion);
    }

    @Test
    void T_159_SVC_04_configuracion_lee_el_parametro_y_por_defecto_permite_corregir_el_documento() {
        when(parameterCatalog.getParameter("IDENTIDAD", "PERMITIR_CORREGIR_DOCUMENTO")).thenReturn(Optional.of(" 1 "));
        assertTrue(service.configuracion().permitirCorregirDocumento());

        when(parameterCatalog.getParameter("IDENTIDAD", "PERMITIR_CORREGIR_DOCUMENTO")).thenReturn(Optional.of("0"));
        assertFalse(service.configuracion().permitirCorregirDocumento());

        when(parameterCatalog.getParameter("IDENTIDAD", "PERMITIR_CORREGIR_DOCUMENTO")).thenReturn(Optional.empty());
        assertTrue(service.configuracion().permitirCorregirDocumento());
    }

    @Test
    void T_159_SVC_05_correccion_de_documento_expone_si_cambia_y_el_nuevo_username() {
        assertFalse(CorreccionDocumentoDTO.ninguna().solicitaCambio());
        assertNull(CorreccionDocumentoDTO.ninguna().nuevoUsername());
        assertTrue(new CorreccionDocumentoDTO(TIPO, null, "m").solicitaCambio());
        assertEquals("42", new CorreccionDocumentoDTO(null, 42, "m").nuevoUsername());
    }

    private static CorregirIdentidadDTO dto(final String tipo, final UUID tipoDocumento, final Integer numero) {
        return new CorregirIdentidadDTO(tipo, REGISTRO, tipoDocumento, numero, "Ana", "Maria", "Gomez", null, "Digitacion", ACTOR);
    }
}
