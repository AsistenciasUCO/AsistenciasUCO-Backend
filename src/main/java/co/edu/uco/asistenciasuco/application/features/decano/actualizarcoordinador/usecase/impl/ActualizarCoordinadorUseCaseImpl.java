package co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.usecase.ActualizarCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.usecase.domain.ActualizarCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ResultadoGestionCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.ResultadoSincronizacion;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.SincronizarCuentaInstitucionalService;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoGestionCoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.ActualizarCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;
import java.util.UUID;

/**
 * HU121: el decano edita nombres, apellidos y correo de un coordinador de su facultad. HU159 (2026-11-05): tambien
 * corrige el tipo y el numero de documento (opcional, con motivo) y sincroniza el nombre de usuario de la cuenta.
 *
 * <p>Orden: primero la base de datos (fuente de verdad, con RBAC y ambito forzados dentro del SP) y despues la
 * cuenta de identidad. Si el proveedor falla, el cambio ya esta en DB: se responde con advertencia {@code IAM_002}.</p>
 */
public final class ActualizarCoordinadorUseCaseImpl implements ActualizarCoordinadorUseCase {

    static final String CODIGO_ADVERTENCIA_IDENTIDAD = "IAM_002";

    private final CoordinadorCommandPort commandPort;
    private final InstitutionalScopePort scopePort;
    private final SincronizarCuentaInstitucionalService sincronizacion;
    private final MessageCatalogPort messageCatalog;

    public ActualizarCoordinadorUseCaseImpl(
            final CoordinadorCommandPort commandPort,
            final InstitutionalScopePort scopePort,
            final SincronizarCuentaInstitucionalService sincronizacion,
            final MessageCatalogPort messageCatalog
    ) {
        this.commandPort = Objects.requireNonNull(commandPort, "CoordinadorCommandPort es obligatorio.");
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.sincronizacion = Objects.requireNonNull(sincronizacion, "SincronizarCuentaInstitucionalService es obligatorio.");
        this.messageCatalog = Objects.requireNonNull(messageCatalog, "MessageCatalogPort es obligatorio.");
    }

    @Override
    public ResultadoGestionCoordinadorDomain execute(final ActualizarCoordinadorDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para actualizar coordinador es obligatorio.");
        }
        final UUID facultad = scopePort.findFacultadIdByDecanoUsuario(domain.usuario())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver la facultad del decano autenticado."));

        final CorreccionDocumentoDTO documento = new CorreccionDocumentoDTO(
                domain.idTipoIdentificacion(), domain.numeroIdentificacion(), domain.motivo());
        final ResultadoGestionCoordinadorProjection resultado = commandPort.actualizarCoordinador(
                domain.idCoordinador(), domain.primerNombre(), domain.segundoNombre(), domain.primerApellido(),
                domain.segundoApellido(), domain.correo(), facultad, domain.usuario(), documento);

        // HU159: si se corrigio el documento, tambien cambia el nombre de usuario de la cuenta de identidad.
        final ResultadoSincronizacion sincronizacionResultado = sincronizacion.actualizarDatos(
                resultado.idUsuario(),
                new ActualizarCuentaIdentidadDTO(domain.correo(), domain.primerNombre(), domain.primerApellido(),
                        documento.nuevoUsername()));

        return sincronizacionResultado == ResultadoSincronizacion.PENDIENTE
                ? new ResultadoGestionCoordinadorDomain(domain.idCoordinador(), true,
                        messageCatalog.getUserMessage(CODIGO_ADVERTENCIA_IDENTIDAD))
                : new ResultadoGestionCoordinadorDomain(domain.idCoordinador(), false, null);
    }
}
