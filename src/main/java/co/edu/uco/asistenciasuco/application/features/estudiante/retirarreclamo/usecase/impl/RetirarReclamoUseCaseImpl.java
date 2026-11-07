package co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.usecase.RetirarReclamoUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.usecase.domain.RetirarReclamoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.Objects;

/**
 * CULM-001 A.2-7 (HU010/HU012): el estudiante autenticado retira su reclamo pendiente. La base de datos decide: el
 * reclamo debe ser suyo (REV_006, 404, sin revelar que existe) y estar pendiente (REV_007, 409); el estado pasa a
 * retirada ({@code X}) y el soporte se conserva (WORM y auditoria). Sin notificacion.
 */
public final class RetirarReclamoUseCaseImpl implements RetirarReclamoUseCase {

    private final InstitutionalScopePort scopePort;
    private final AsistenciaRepositoryPort asistenciaRepositoryPort;

    public RetirarReclamoUseCaseImpl(final InstitutionalScopePort scopePort, final AsistenciaRepositoryPort asistenciaRepositoryPort) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.asistenciaRepositoryPort = Objects.requireNonNull(asistenciaRepositoryPort, "AsistenciaRepositoryPort es obligatorio.");
    }

    @Override
    public String execute(final RetirarReclamoDomain domain) {
        if (domain == null || domain.idReclamo() == null) {
            throw new ValidationException("El reclamo a retirar es obligatorio.");
        }
        if (domain.actorUsuarioId() == null) {
            throw new ValidationException("El usuario autenticado es obligatorio.");
        }
        scopePort.findEstudianteIdByUsuario(domain.actorUsuarioId())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));
        return asistenciaRepositoryPort.retirarSolicitudRevision(domain.idReclamo(), domain.actorUsuarioId());
    }
}
