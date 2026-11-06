package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.ConsultarCoordinadoresUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.domain.CoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.mapper.ConsultarCoordinadoresRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenCoordinacionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ConsultarCoordinadoresUseCaseImpl implements ConsultarCoordinadoresUseCase {
    private final InstitutionalScopePort scopePort;
    private final CoordinadorQueryPort coordinadorQueryPort;
    public ConsultarCoordinadoresUseCaseImpl(final InstitutionalScopePort scopePort, final CoordinadorQueryPort coordinadorQueryPort) {
        this.scopePort = Objects.requireNonNull(scopePort);
        this.coordinadorQueryPort = Objects.requireNonNull(coordinadorQueryPort);
    }
    @Override public List<CoordinadorDomain> execute(final UUID actorUsuarioId) {
        final UUID idFacultad = scopePort.findFacultadIdByDecanoUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver la facultad del decano autenticado."));
        final Map<UUID, CoordinadorProjection> consolidado = new LinkedHashMap<>();
        for (final CoordinadorProjection item : coordinadorQueryPort.consultarCoordinadoresPorFacultad(idFacultad)) {
            if (item != null && item.id() != null) {
                consolidado.putIfAbsent(item.id(), item);
            }
        }
        for (final CoordinadorProjection item : coordinadorQueryPort.consultarCoordinadoresSinPrograma(idFacultad)) {
            if (item != null && item.id() != null) {
                consolidado.putIfAbsent(item.id(), item);
            }
        }
        // Certificacion F3: facultad y conteos reales por programa (antes la pantalla inventaba "Sin Facultad" y 0/0).
        final Map<UUID, ResumenCoordinacionProjection> resumenes = new LinkedHashMap<>();
        String nombreFacultad = null;
        for (final ResumenCoordinacionProjection resumen : coordinadorQueryPort.consultarResumenProgramasFacultad(idFacultad)) {
            if (resumen != null && resumen.idPrograma() != null) {
                resumenes.putIfAbsent(resumen.idPrograma(), resumen);
                nombreFacultad = nombreFacultad == null ? resumen.nombreFacultad() : nombreFacultad;
            }
        }
        final String facultad = nombreFacultad;
        return consolidado.values().stream()
                .map(c -> ConsultarCoordinadoresRepositoryMapper.toDomain(c, facultad,
                        c.idPrograma() == null ? null : resumenes.get(c.idPrograma())))
                .toList();
    }
}
