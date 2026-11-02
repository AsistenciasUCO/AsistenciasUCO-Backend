package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.profilesync;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort.IdentityProviderException;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.UsuarioSincronizacionIdentidadPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort.Estado;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort.Resultado;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileSyncingIdentityProviderTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final String CORREO = "persona@uco.edu.co";

    private final IdentityProviderPort delegate = mock(IdentityProviderPort.class);
    private final AccessProfilePort profiles = mock(AccessProfilePort.class);
    private final UsuarioSincronizacionIdentidadPort desfases = mock(UsuarioSincronizacionIdentidadPort.class);
    private final ProfileSyncingIdentityProvider provider = new ProfileSyncingIdentityProvider(delegate, profiles, desfases);

    private static CrearCuentaIdentidadDTO dto(final InstitutionalRole rol) {
        return new CrearCuentaIdentidadDTO("123456", USUARIO, CORREO, "Ana", "Perez", "temporal", rol);
    }

    @Test
    void crear_cuenta_asegura_el_perfil_despues_de_crear_la_cuenta_y_el_rol() {
        when(delegate.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("kc-1", true));
        when(profiles.sincronizar(CORREO, InstitutionalRole.DECANO)).thenReturn(new Resultado(Estado.ASIGNADO, "ok"));

        final CuentaIdentidadDTO cuenta = provider.crearCuenta(dto(InstitutionalRole.DECANO));

        assertEquals("kc-1", cuenta.idExterno());
        final var orden = inOrder(delegate, profiles);
        orden.verify(delegate).crearCuenta(any());
        orden.verify(profiles).sincronizar(CORREO, InstitutionalRole.DECANO);
        verify(desfases, never()).registrar(any(), anyString(), anyString());
    }

    @Test
    void persona_aun_no_registrada_en_el_pdp_no_interrumpe_el_alta_y_deja_el_desfase_registrado() {
        when(delegate.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("kc-1", true));
        when(profiles.sincronizar(CORREO, InstitutionalRole.DOCENTE))
                .thenReturn(new Resultado(Estado.PENDIENTE_PERSONA_NO_REGISTRADA, "no existe"));

        provider.crearCuenta(dto(InstitutionalRole.DOCENTE));

        verify(desfases).registrar(eq(USUARIO), eq(ProfileSyncingIdentityProvider.OPERACION_SINCRONIZAR), anyString());
    }

    @Test
    void falla_del_proveedor_al_crear_no_sincroniza_ningun_perfil() {
        when(delegate.crearCuenta(any())).thenThrow(new IdentityProviderException("kc caido"));

        assertThrows(IdentityProviderException.class, () -> provider.crearCuenta(dto(InstitutionalRole.DOCENTE)));
        verify(profiles, never()).sincronizar(anyString(), any());
    }

    @Test
    void asignar_rol_sincroniza_el_perfil_con_el_correo_de_la_cuenta() {
        when(delegate.correoDeCuenta("kc-1")).thenReturn(Optional.of(CORREO));
        when(profiles.sincronizar(CORREO, InstitutionalRole.COORDINADOR)).thenReturn(new Resultado(Estado.ASIGNADO, "ok"));

        provider.asignarRol("kc-1", InstitutionalRole.COORDINADOR);

        verify(delegate).asignarRol("kc-1", InstitutionalRole.COORDINADOR);
        verify(profiles).sincronizar(CORREO, InstitutionalRole.COORDINADOR);
    }

    @Test
    void retirar_rol_revoca_el_perfil_equivalente() {
        when(delegate.correoDeCuenta("kc-1")).thenReturn(Optional.of(CORREO));
        when(profiles.revocar(CORREO, InstitutionalRole.COORDINADOR)).thenReturn(new Resultado(Estado.NO_ASIGNADO, "ok"));

        provider.retirarRol("kc-1", InstitutionalRole.COORDINADOR);

        verify(delegate).retirarRol("kc-1", InstitutionalRole.COORDINADOR);
        verify(profiles).revocar(CORREO, InstitutionalRole.COORDINADOR);
        verify(desfases, never()).registrar(any(), anyString(), anyString());
    }

    @Test
    void revocacion_fallida_se_registra_como_desfase_sin_lanzar() {
        when(delegate.correoDeCuenta("kc-1")).thenReturn(Optional.of(CORREO));
        when(profiles.revocar(CORREO, InstitutionalRole.DOCENTE))
                .thenReturn(new Resultado(Estado.PENDIENTE_NO_DISPONIBLE, "pdp caido"));

        provider.retirarRol("kc-1", InstitutionalRole.DOCENTE);

        verify(desfases).registrar(any(), eq(ProfileSyncingIdentityProvider.OPERACION_REVOCAR), anyString());
    }

    @Test
    void correo_no_resoluble_deja_desfase_sin_tocar_el_pdp() {
        when(delegate.correoDeCuenta("kc-1")).thenThrow(new IdentityProviderException("kc caido"));

        provider.asignarRol("kc-1", InstitutionalRole.DOCENTE);

        verify(profiles, never()).sincronizar(anyString(), any());
        verify(desfases).registrar(any(), eq(ProfileSyncingIdentityProvider.OPERACION_SINCRONIZAR), anyString());
    }

    @Test
    void proveedor_legacy_sin_perfiles_no_registra_desfase() {
        when(delegate.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("kc-1", true));
        when(profiles.sincronizar(CORREO, InstitutionalRole.DOCENTE)).thenReturn(new Resultado(Estado.NO_APLICA, "legacy"));

        provider.crearCuenta(dto(InstitutionalRole.DOCENTE));

        verify(desfases, never()).registrar(any(), anyString(), anyString());
    }

    @Test
    void un_fallo_al_registrar_el_desfase_no_interrumpe_la_operacion() {
        when(delegate.crearCuenta(any())).thenReturn(new CuentaIdentidadDTO("kc-1", true));
        when(profiles.sincronizar(CORREO, InstitutionalRole.DOCENTE)).thenReturn(new Resultado(Estado.RECHAZADO, "x"));
        doThrow(new IllegalStateException("metricas")).when(desfases).registrar(any(), anyString(), anyString());

        provider.crearCuenta(dto(InstitutionalRole.DOCENTE));
    }

    @Test
    void las_demas_operaciones_se_delegan_sin_cambios() {
        provider.eliminarCuenta("kc-1");
        provider.deshabilitarCuenta("kc-1");
        provider.habilitarCuenta("kc-1");
        provider.buscarCuentaPorIdUsuario(USUARIO);
        provider.rolesDeCuenta("kc-1");

        verify(delegate).eliminarCuenta("kc-1");
        verify(delegate).deshabilitarCuenta("kc-1");
        verify(delegate).habilitarCuenta("kc-1");
        verify(delegate).buscarCuentaPorIdUsuario(USUARIO);
        verify(delegate).rolesDeCuenta("kc-1");
        verify(profiles, never()).sincronizar(anyString(), any());
    }

    @Test
    void confirmar_correo_se_delega_al_proveedor_y_no_cae_en_la_operacion_no_soportada_del_puerto() {
        provider.confirmarCorreoCuenta("kc-1");

        verify(delegate).confirmarCorreoCuenta("kc-1");
        verify(profiles, never()).sincronizar(anyString(), any());
    }
}
