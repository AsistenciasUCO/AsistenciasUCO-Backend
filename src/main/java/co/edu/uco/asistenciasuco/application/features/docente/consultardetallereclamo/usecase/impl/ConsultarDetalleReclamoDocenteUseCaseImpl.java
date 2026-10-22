package co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.ConsultarDetalleReclamoDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.domain.ConsultarDetalleReclamoDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.domain.DetalleReclamoDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.mapper.ConsultarDetalleReclamoDocenteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudRevisionDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.Objects;

/**
 * Detalle de una solicitud de revision del docente titular. El rol DOCENTE no basta: Application exige que el
 * usuario autenticado sea un docente institucional y SQL filtra ademas por titularidad del grupo; una solicitud
 * inexistente o de otro docente es indistinguible ({@link ResourceNotFoundException}) para no revelar su existencia.
 */
public final class ConsultarDetalleReclamoDocenteUseCaseImpl implements ConsultarDetalleReclamoDocenteUseCase {

    private final InstitutionalScopePort scopePort;
    private final SolicitudRevisionDocenteQueryPort queryPort;

    public ConsultarDetalleReclamoDocenteUseCaseImpl(
            final InstitutionalScopePort scopePort,
            final SolicitudRevisionDocenteQueryPort queryPort
    ) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "SolicitudRevisionDocenteQueryPort es obligatorio.");
    }

    @Override
    public DetalleReclamoDocenteDomain execute(final ConsultarDetalleReclamoDocenteDomain domain) {
        scopePort.findDocenteIdByUsuario(domain.actorUsuarioId())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el docente autenticado."));

        return queryPort.consultarDetalleSolicitudDocente(domain.idSolicitud(), domain.actorUsuarioId())
                .map(ConsultarDetalleReclamoDocenteRepositoryMapper::toDomain)
                .orElseThrow(() -> new ResourceNotFoundException("La solicitud de revision no existe."));
    }
}
