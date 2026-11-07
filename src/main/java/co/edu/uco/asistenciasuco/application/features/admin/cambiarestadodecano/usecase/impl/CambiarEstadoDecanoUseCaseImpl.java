package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.usecase.CambiarEstadoDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.usecase.domain.CambiarEstadoDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.SincronizarCuentaInstitucionalService;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort.ResultadoCambioEstadoDecano;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DecanoProjection;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;

import java.util.Objects;

/**
 * CULM-001 A.2-8 (F-ADM-002): el administrador activa o desactiva a un decano de su institucion
 * ({@code PATCH /api/v1/admin/decanos/{id}/toggle}).
 *
 * <p>La base de datos decide (RBAC, ambito de la institucion, DEC_002 si la facultad tiene programas activos) y responde
 * si la persona quedo sin roles activos. Solo despues se sincroniza la cuenta de identidad, como en HU122 (coordinador):
 * un fallo del proveedor no revierte el cambio, porque el RBAC en BD ya bloquea el rol. Se devuelve la fila del decano
 * releida con su estado nuevo.</p>
 */
public final class CambiarEstadoDecanoUseCaseImpl implements CambiarEstadoDecanoUseCase {

    private final DecanoCommandPort commandPort;
    private final DecanoQueryPort queryPort;
    private final SincronizarCuentaInstitucionalService sincronizacion;

    public CambiarEstadoDecanoUseCaseImpl(
            final DecanoCommandPort commandPort,
            final DecanoQueryPort queryPort,
            final SincronizarCuentaInstitucionalService sincronizacion
    ) {
        this.commandPort = Objects.requireNonNull(commandPort, "DecanoCommandPort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "DecanoQueryPort es obligatorio.");
        this.sincronizacion = Objects.requireNonNull(sincronizacion, "SincronizarCuentaInstitucionalService es obligatorio.");
    }

    @Override
    public DecanoProjection execute(final CambiarEstadoDecanoDomain domain) {
        if (domain == null || domain.idDecano() == null) {
            throw new ValidationException("El decano es obligatorio.");
        }
        if (domain.actorUsuarioId() == null) {
            throw new ValidationException("El usuario autenticado es obligatorio.");
        }
        final ResultadoCambioEstadoDecano resultado = commandPort.cambiarEstadoDecano(domain.idDecano(), null, domain.actorUsuarioId());
        if (resultado.estaActivoDecano()) {
            sincronizacion.reactivar(resultado.idUsuario(), InstitutionalRole.DECANO);
        } else {
            sincronizacion.desactivar(resultado.idUsuario(), InstitutionalRole.DECANO, resultado.personaSinRolesActivos());
        }
        return queryPort.consultarDecanos().stream()
                .filter(decano -> domain.idDecano().equals(decano.id()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("El decano especificado no existe en el sistema."));
    }
}
