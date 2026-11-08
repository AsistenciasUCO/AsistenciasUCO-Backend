package co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.features.admin.exception.AdmisionEstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.UsuarioSincronizacionIdentidadPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * Asegura la cuenta de identidad de un usuario despues de que la base de datos (fuente de verdad) ya confirmo el alta
 * (CULM-001 D.4, CONTRACT §5.3; lo reutilizan D.5 y D.9 F-ADM-024).
 *
 * <p>Un fallo del proveedor nunca revierte la base de datos ni responde 500: se registra el desfase
 * ({@code APROVISIONAR}) y el estado queda {@link EstadoCuentaAprovisionada#PENDIENTE_SINCRONIZACION}; repetir la operacion
 * repara la cuenta con {@link #repararOFallar}.</p>
 *
 * <p>POJO sin anotaciones de framework: se cablea con {@code @Bean} en la configuracion de infraestructura.</p>
 */
public final class AsegurarCuentaIdentidadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsegurarCuentaIdentidadService.class);

    static final String OPERACION_APROVISIONAR = "APROVISIONAR";

    private final IdentityProviderPort identityProvider;
    private final UsuarioSincronizacionIdentidadPort desfases;

    public AsegurarCuentaIdentidadService(
            final IdentityProviderPort identityProvider,
            final UsuarioSincronizacionIdentidadPort desfases
    ) {
        this.identityProvider = Objects.requireNonNull(identityProvider, "IdentityProviderPort es obligatorio.");
        this.desfases = Objects.requireNonNull(desfases, "UsuarioSincronizacionIdentidadPort es obligatorio.");
    }

    /**
     * Crea la cuenta (el proveedor es idempotente: una cuenta existente se devuelve sin modificarla). Con
     * {@code requiereVerificacionCorreo} y cuenta nueva queda pendiente de verificacion.
     */
    public ResultadoCuentaIdentidad asegurar(final SolicitudCuentaIdentidad solicitud) {
        Objects.requireNonNull(solicitud, "La solicitud de cuenta de identidad es obligatoria.");
        return crear(solicitud, false);
    }

    /**
     * Para una persona que ya estaba admitida (ADS_002): si su cuenta existe, el conflicto se mantiene; si falta, se crea y el
     * resultado informa {@code cuentaReparada}. Si el proveedor no puede buscar la cuenta, el conflicto se mantiene.
     *
     * @throws ConflictException ADS_002 cuando la cuenta existe o no es posible comprobarlo
     */
    public ResultadoCuentaIdentidad repararOFallar(final SolicitudCuentaIdentidad solicitud) {
        Objects.requireNonNull(solicitud, "La solicitud de cuenta de identidad es obligatoria.");
        final Optional<CuentaIdentidadDTO> existente;
        try {
            existente = identityProvider.buscarCuentaPorIdUsuario(solicitud.idUsuario());
        } catch (final IdentityProviderPort.IdentityProviderException exception) {
            throw new ConflictException(AdmisionEstudianteErrorCode.ADS_002);
        }
        if (existente.isPresent()) {
            throw new ConflictException(AdmisionEstudianteErrorCode.ADS_002);
        }
        return crear(solicitud, true);
    }

    private ResultadoCuentaIdentidad crear(final SolicitudCuentaIdentidad solicitud, final boolean reparacion) {
        try {
            final CrearCuentaIdentidadDTO cuenta = new CrearCuentaIdentidadDTO(solicitud.username(), solicitud.idUsuario(),
                    solicitud.correo(), solicitud.primerNombre(), solicitud.primerApellido(), solicitud.passwordInicial(),
                    solicitud.rol(), solicitud.requiereVerificacionCorreo());
            final CuentaIdentidadDTO creada = identityProvider.crearCuenta(cuenta);
            final boolean pendienteVerificacion = solicitud.requiereVerificacionCorreo() && creada.newlyCreated();
            return new ResultadoCuentaIdentidad(
                    pendienteVerificacion ? EstadoCuentaAprovisionada.PENDIENTE_VERIFICACION : EstadoCuentaAprovisionada.ACTIVA,
                    reparacion);
        } catch (final IdentityProviderPort.IdentityProviderException exception) {
            registrarDesfase(solicitud, String.valueOf(exception.getMessage()));
            return new ResultadoCuentaIdentidad(EstadoCuentaAprovisionada.PENDIENTE_SINCRONIZACION, false);
        }
    }

    private void registrarDesfase(final SolicitudCuentaIdentidad solicitud, final String error) {
        try {
            desfases.registrar(solicitud.idUsuario(), OPERACION_APROVISIONAR, error);
        } catch (final RuntimeException exception) {
            // Registrar el desfase no puede ocultar ni revertir la operacion principal ya confirmada en DB.
            LOGGER.atError()
                    .addKeyValue("operation", OPERACION_APROVISIONAR)
                    .addKeyValue("eventType", "IDENTITY_DRIFT_REGISTRATION_FAILED")
                    .setCause(exception)
                    .log("No fue posible registrar el desfase de identidad.");
        }
    }
}
