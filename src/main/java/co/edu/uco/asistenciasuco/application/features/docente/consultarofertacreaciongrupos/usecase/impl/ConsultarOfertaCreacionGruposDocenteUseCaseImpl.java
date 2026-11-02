package co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.usecase.ConsultarOfertaCreacionGruposDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.usecase.domain.OfertaCreacionGruposDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.usecase.mapper.ConsultarOfertaCreacionGruposDocenteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.OfertaCreacionGruposDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.Objects;
import java.util.UUID;

public final class ConsultarOfertaCreacionGruposDocenteUseCaseImpl implements ConsultarOfertaCreacionGruposDocenteUseCase {
    private final InstitutionalScopePort scopePort;
    private final OfertaCreacionGruposDocenteQueryPort queryPort;

    public ConsultarOfertaCreacionGruposDocenteUseCaseImpl(final InstitutionalScopePort scopePort,
                                                           final OfertaCreacionGruposDocenteQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort);
        this.queryPort = Objects.requireNonNull(queryPort);
    }

    @Override
    public OfertaCreacionGruposDocenteDomain execute(final UUID actorUsuarioId) {
        final UUID idDocente = scopePort.findDocenteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el docente autenticado."));
        return new OfertaCreacionGruposDocenteDomain(idDocente, queryPort.consultarOferta(actorUsuarioId).stream()
                .map(ConsultarOfertaCreacionGruposDocenteRepositoryMapper::toDomain)
                .toList());
    }
}
