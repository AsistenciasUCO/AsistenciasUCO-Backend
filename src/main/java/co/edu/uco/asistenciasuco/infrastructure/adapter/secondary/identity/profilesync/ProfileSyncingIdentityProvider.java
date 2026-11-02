package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.profilesync;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.UsuarioSincronizacionIdentidadPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.ActualizarCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort.Estado;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort.Resultado;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Decorador del proveedor de identidad del modo baseline: cada rol que se asigna o retira en el proveedor deja
 * también asegurado (o revocado) el perfil equivalente en el motor de autorización. Asignar un client role no
 * concede acceso por sí solo, así que ningún flujo de alta o cambio de rol declara listo a un usuario solo porque
 * existe en SQL Server y en el proveedor.
 *
 * <p>La sincronización del perfil nunca interrumpe la operación principal ya confirmada: si el motor no responde o
 * aún no conoce a la persona, el desfase se registra y el estado queda PENDIENTE (sin acceso) hasta reconciliar.</p>
 */
public final class ProfileSyncingIdentityProvider implements IdentityProviderPort {

    static final String OPERACION_SINCRONIZAR = "SINCRONIZAR_PERFIL";
    static final String OPERACION_REVOCAR = "REVOCAR_PERFIL";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileSyncingIdentityProvider.class);

    private final IdentityProviderPort delegate;
    private final AccessProfilePort accessProfiles;
    private final UsuarioSincronizacionIdentidadPort desfases;

    public ProfileSyncingIdentityProvider(
            final IdentityProviderPort delegate,
            final AccessProfilePort accessProfiles,
            final UsuarioSincronizacionIdentidadPort desfases
    ) {
        this.delegate = Objects.requireNonNull(delegate, "IdentityProviderPort es obligatorio.");
        this.accessProfiles = Objects.requireNonNull(accessProfiles, "AccessProfilePort es obligatorio.");
        this.desfases = Objects.requireNonNull(desfases, "UsuarioSincronizacionIdentidadPort es obligatorio.");
    }

    @Override
    public CuentaIdentidadDTO crearCuenta(final CrearCuentaIdentidadDTO dto) {
        final CuentaIdentidadDTO cuenta = delegate.crearCuenta(dto);
        sincronizar(dto.idUsuario(), dto.correo(), dto.rolInstitucional());
        return cuenta;
    }

    @Override
    public void eliminarCuenta(final String idExterno) {
        delegate.eliminarCuenta(idExterno);
    }

    @Override
    public void asignarRol(final String idExterno, final InstitutionalRole rol) {
        delegate.asignarRol(idExterno, rol);
        correo(idExterno).ifPresentOrElse(
                correo -> sincronizar(null, correo, rol),
                () -> registrar(null, OPERACION_SINCRONIZAR, rol + ": no se pudo resolver el correo de la cuenta"));
    }

    @Override
    public void retirarRol(final String idExterno, final InstitutionalRole rol) {
        delegate.retirarRol(idExterno, rol);
        correo(idExterno).ifPresentOrElse(
                correo -> revocar(correo, rol),
                () -> registrar(null, OPERACION_REVOCAR, rol + ": no se pudo resolver el correo de la cuenta"));
    }

    @Override
    public Optional<CuentaIdentidadDTO> buscarCuentaPorIdUsuario(final UUID idUsuario) {
        return delegate.buscarCuentaPorIdUsuario(idUsuario);
    }

    @Override
    public void deshabilitarCuenta(final String idExterno) {
        delegate.deshabilitarCuenta(idExterno);
    }

    @Override
    public void habilitarCuenta(final String idExterno) {
        delegate.habilitarCuenta(idExterno);
    }

    @Override
    public void confirmarCorreoCuenta(final String idExterno) {
        delegate.confirmarCorreoCuenta(idExterno);
    }

    @Override
    public void actualizarDatosCuenta(final String idExterno, final ActualizarCuentaIdentidadDTO datos) {
        delegate.actualizarDatosCuenta(idExterno, datos);
    }

    @Override
    public Optional<String> correoDeCuenta(final String idExterno) {
        return delegate.correoDeCuenta(idExterno);
    }

    @Override
    public Set<InstitutionalRole> rolesDeCuenta(final String idExterno) {
        return delegate.rolesDeCuenta(idExterno);
    }

    private Optional<String> correo(final String idExterno) {
        try {
            return delegate.correoDeCuenta(idExterno);
        } catch (final IdentityProviderException exception) {
            return Optional.empty();
        }
    }

    private void sincronizar(final UUID idUsuario, final String correo, final InstitutionalRole rol) {
        final Resultado resultado = accessProfiles.sincronizar(correo, rol);
        if (resultado.estado() != Estado.ASIGNADO && resultado.estado() != Estado.NO_APLICA) {
            registrar(idUsuario, OPERACION_SINCRONIZAR, rol + ":" + resultado.estado() + ":" + resultado.detalle());
        }
    }

    private void revocar(final String correo, final InstitutionalRole rol) {
        final Resultado resultado = accessProfiles.revocar(correo, rol);
        if (resultado.estado() != Estado.NO_ASIGNADO && resultado.estado() != Estado.NO_APLICA) {
            registrar(null, OPERACION_REVOCAR, rol + ":" + resultado.estado() + ":" + resultado.detalle());
        }
    }

    private void registrar(final UUID idUsuario, final String operacion, final String detalle) {
        try {
            desfases.registrar(idUsuario, operacion, detalle);
        } catch (final RuntimeException exception) {
            LOGGER.atError()
                    .addKeyValue("operation", operacion)
                    .addKeyValue("eventType", "PROFILE_DRIFT_REGISTRATION_FAILED")
                    .setCause(exception)
                    .log("No fue posible registrar el desfase del perfil de acceso.");
        }
    }
}
