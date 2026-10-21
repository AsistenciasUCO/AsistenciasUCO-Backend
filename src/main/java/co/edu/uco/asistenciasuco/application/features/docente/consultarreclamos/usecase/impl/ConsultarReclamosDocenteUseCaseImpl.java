package co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudRevisionDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudRevisionDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.ConsultarReclamosDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.domain.ConsultarReclamosDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.mapper.ConsultarReclamosDocenteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.domain.ReclamosDocentePaginaDomain;

import java.util.List;
import java.util.Objects;

/**
 * Lista las solicitudes de revision del docente titular. El rol DOCENTE no basta: Application exige que el
 * usuario autenticado sea un docente institucional y SQL filtra ademas por titularidad del grupo
 * ({@code Grupo.docente}), de modo que otro docente nunca recibe reclamos ajenos.
 */
public final class ConsultarReclamosDocenteUseCaseImpl implements ConsultarReclamosDocenteUseCase {

    private final InstitutionalScopePort scopePort;
    private final SolicitudRevisionDocenteQueryPort queryPort;

    public ConsultarReclamosDocenteUseCaseImpl(
            final InstitutionalScopePort scopePort,
            final SolicitudRevisionDocenteQueryPort queryPort
    ) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "SolicitudRevisionDocenteQueryPort es obligatorio.");
    }

    @Override
    public ReclamosDocentePaginaDomain execute(final ConsultarReclamosDocenteDomain domain) {
        scopePort.findDocenteIdByUsuario(domain.actorUsuarioId())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el docente autenticado."));

        final List<SolicitudRevisionDocenteProjection> filas = queryPort.consultarSolicitudesDocente(
                domain.actorUsuarioId(), domain.codigoEstado(), domain.pagina(), domain.tamano());
        final int total = filas.isEmpty() ? 0 : filas.getFirst().totalRegistros();
        return new ReclamosDocentePaginaDomain(
                filas.stream().map(ConsultarReclamosDocenteRepositoryMapper::toDomain).toList(), total);
    }
}
