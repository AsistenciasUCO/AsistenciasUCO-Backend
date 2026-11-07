package co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.ConsultarMateriasEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.domain.MateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.mapper.ConsultarMateriasEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.MateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarMateriasEstudianteUseCaseImpl implements ConsultarMateriasEstudianteUseCase {
    private final InstitutionalScopePort scopePort; private final MateriaEstudianteQueryPort queryPort;
    public ConsultarMateriasEstudianteUseCaseImpl(final InstitutionalScopePort scopePort, final MateriaEstudianteQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort); this.queryPort = Objects.requireNonNull(queryPort);
    }
    @Override public List<MateriaEstudianteDomain> execute(final UUID actorUsuarioId) {
        // CULM-001 A.2-11: sin estudiante, 403; el SP recibe el usuario y solo devuelve matriculas inscritas (A).
        scopePort.findEstudianteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));
        return queryPort.consultarMateriasEstudiante(actorUsuarioId).stream()
                .map(ConsultarMateriasEstudianteRepositoryMapper::toDomain)
                .toList();
    }
}
