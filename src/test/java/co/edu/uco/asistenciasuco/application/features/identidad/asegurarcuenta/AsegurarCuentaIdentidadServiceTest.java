package co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.admin.exception.AdmisionEstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.UsuarioSincronizacionIdentidadPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CULM-001 D.4 (CONTRACT §5.3): {@code AsegurarCuentaIdentidadService}, reutilizado por D.5 y D.9 (F-ADM-024). Cuatro ramas:
 * cuenta nueva con y sin verificacion de correo, fallo del proveedor (desfase y PENDIENTE_SINCRONIZACION, nunca 500) y la
 * reparacion de una cuenta faltante cuando la persona ya estaba registrada (ADS_002).
 */
class AsegurarCuentaIdentidadServiceTest {

    private static final UUID USUARIO = UUID.randomUUID();

    private final IdentityProviderPort proveedor = mock(IdentityProviderPort.class);
    private final UsuarioSincronizacionIdentidadPort desfases = mock(UsuarioSincronizacionIdentidadPort.class);
    private final AsegurarCuentaIdentidadService servicio = new AsegurarCuentaIdentidadService(proveedor, desfases);

    private static SolicitudCuentaIdentidad solicitud(final boolean requiereVerificacion) {
        return new SolicitudCuentaIdentidad(USUARIO, "1017123456", "ana@uco.edu.co", "Ana", "Perez", "Clave123!", InstitutionalRole.ESTUDIANTE,
                requiereVerificacion);
    }

    @Test
    void cuenta_nueva_con_verificacion_de_correo_queda_pendiente_de_verificacion() {
        when(proveedor.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("ext-1", true));

        final ResultadoCuentaIdentidad resultado = servicio.asegurar(solicitud(true));

        assertEquals(EstadoCuentaAprovisionada.PENDIENTE_VERIFICACION, resultado.estado());
        assertFalse(resultado.cuentaReparada());
        final ArgumentCaptor<CrearCuentaIdentidadDTO> captor = ArgumentCaptor.forClass(CrearCuentaIdentidadDTO.class);
        verify(proveedor).crearCuenta(captor.capture());
        assertTrue(captor.getValue().requiereVerificacionCorreo());
        assertEquals(USUARIO, captor.getValue().idUsuario());
        assertEquals("1017123456", captor.getValue().username());
        assertEquals(InstitutionalRole.ESTUDIANTE, captor.getValue().rolInstitucional());
        verify(desfases, never()).registrar(any(), anyString(), anyString());
    }

    @Test
    void cuenta_sin_verificacion_o_ya_existente_en_el_proveedor_queda_activa() {
        when(proveedor.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("ext-1", true), new CuentaIdentidadDTO("ext-1", false));

        assertEquals(EstadoCuentaAprovisionada.ACTIVA, servicio.asegurar(solicitud(false)).estado());
        // Con verificacion pedida pero cuenta ya existente en el IdP: nada que verificar.
        assertEquals(EstadoCuentaAprovisionada.ACTIVA, servicio.asegurar(solicitud(true)).estado());
    }

    @Test
    void un_fallo_del_proveedor_registra_el_desfase_y_deja_pendiente_de_sincronizacion_sin_lanzar() {
        when(proveedor.crearCuenta(any())).thenThrow(new IdentityProviderPort.IdentityProviderException("IdP caido"));

        final ResultadoCuentaIdentidad resultado = servicio.asegurar(solicitud(true));

        assertEquals(EstadoCuentaAprovisionada.PENDIENTE_SINCRONIZACION, resultado.estado());
        assertFalse(resultado.cuentaReparada());
        verify(desfases).registrar(eq(USUARIO), eq("APROVISIONAR"), anyString());
    }

    @Test
    void si_registrar_el_desfase_tambien_falla_no_se_oculta_el_resultado() {
        when(proveedor.crearCuenta(any())).thenThrow(new IdentityProviderPort.IdentityProviderException("IdP caido"));
        doThrow(new IllegalStateException("sin metricas")).when(desfases).registrar(any(), anyString(), anyString());

        assertEquals(EstadoCuentaAprovisionada.PENDIENTE_SINCRONIZACION, servicio.asegurar(solicitud(false)).estado());
    }

    @Test
    void reparar_con_la_cuenta_existente_es_conflicto_ADS_002() {
        when(proveedor.buscarCuentaPorIdUsuario(USUARIO)).thenReturn(Optional.of(new CuentaIdentidadDTO("ext-1", false)));

        final ConflictException conflicto = assertThrows(ConflictException.class, () -> servicio.repararOFallar(solicitud(false)));

        assertEquals(AdmisionEstudianteErrorCode.ADS_002.code(), conflicto.getCode());
        verify(proveedor, never()).crearCuenta(any());
    }

    @Test
    void reparar_con_la_cuenta_faltante_la_crea_y_marca_cuenta_reparada() {
        when(proveedor.buscarCuentaPorIdUsuario(USUARIO)).thenReturn(Optional.empty());
        when(proveedor.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("ext-9", true));

        final ResultadoCuentaIdentidad resultado = servicio.repararOFallar(solicitud(false));

        assertEquals(EstadoCuentaAprovisionada.ACTIVA, resultado.estado());
        assertTrue(resultado.cuentaReparada());
        verify(proveedor).crearCuenta(any());
    }

    @Test
    void reparar_sin_la_capacidad_de_buscar_cuenta_es_conflicto_ADS_002() {
        when(proveedor.buscarCuentaPorIdUsuario(USUARIO))
                .thenThrow(new IdentityProviderPort.IdentityProviderException("operacion no soportada"));

        final ConflictException conflicto = assertThrows(ConflictException.class, () -> servicio.repararOFallar(solicitud(false)));

        assertEquals(AdmisionEstudianteErrorCode.ADS_002.code(), conflicto.getCode());
        verify(proveedor, never()).crearCuenta(any());
    }

    @Test
    void reparar_cuando_crear_la_cuenta_falla_deja_pendiente_de_sincronizacion_y_registra_el_desfase() {
        when(proveedor.buscarCuentaPorIdUsuario(USUARIO)).thenReturn(Optional.empty());
        when(proveedor.crearCuenta(any())).thenThrow(new IdentityProviderPort.IdentityProviderException("IdP caido"));

        final ResultadoCuentaIdentidad resultado = servicio.repararOFallar(solicitud(false));

        assertEquals(EstadoCuentaAprovisionada.PENDIENTE_SINCRONIZACION, resultado.estado());
        assertFalse(resultado.cuentaReparada());
        verify(desfases).registrar(eq(USUARIO), eq("APROVISIONAR"), anyString());
    }

    @Test
    void las_dependencias_y_la_solicitud_son_obligatorias() {
        assertThrows(NullPointerException.class, () -> new AsegurarCuentaIdentidadService(null, desfases));
        assertThrows(NullPointerException.class, () -> new AsegurarCuentaIdentidadService(proveedor, null));
        assertThrows(NullPointerException.class, () -> servicio.asegurar(null));
        assertThrows(NullPointerException.class, () -> servicio.repararOFallar(null));
    }
}
