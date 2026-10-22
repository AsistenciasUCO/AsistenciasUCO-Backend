package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.ConsultarDocentesProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.mapper.ConsultarDocentesProgramaRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ConsultarDocentesProgramaUseCaseImpl implements ConsultarDocentesProgramaUseCase {

    private final InstitutionalScopePort scopePort;
    private final DocenteProgramaQueryPort queryPort;

    public ConsultarDocentesProgramaUseCaseImpl(
            final InstitutionalScopePort scopePort,
            final DocenteProgramaQueryPort queryPort
    ) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "DocenteProgramaQueryPort es obligatorio.");
    }

    @Override
    public List<DocenteProgramaDomain> execute(final UUID actorUsuarioId) {
        final UUID idPrograma = scopePort.findProgramaIdByCoordinadorUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el programa del coordinador autenticado."));

        final Map<UUID, DocenteProgramaProjection> consolidado = new LinkedHashMap<>();
        for (final DocenteProgramaProjection item : queryPort.consultarDocentesPorPrograma(idPrograma)) {
            if (item != null && item.id() != null) {
                consolidado.putIfAbsent(item.id(), item);
            }
        }
        for (final DocenteProgramaProjection item : queryPort.consultarDocentesDisponibles()) {
            if (item != null && item.id() != null) {
                consolidado.putIfAbsent(item.id(), item);
            }
        }
        return consolidado.values().stream()
                .map(ConsultarDocentesProgramaRepositoryMapper::toDomain)
                .toList();
    }
}
