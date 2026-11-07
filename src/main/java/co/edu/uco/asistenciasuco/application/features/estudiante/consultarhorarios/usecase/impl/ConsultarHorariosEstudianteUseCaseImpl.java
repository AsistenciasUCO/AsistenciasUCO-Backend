package co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.ConsultarHorariosEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.domain.HorarioEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.mapper.ConsultarHorariosEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HorarioEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.List; import java.util.Objects; import java.util.UUID;

public final class ConsultarHorariosEstudianteUseCaseImpl implements ConsultarHorariosEstudianteUseCase {
    private final InstitutionalScopePort scopePort; private final HorarioEstudianteQueryPort queryPort;
    public ConsultarHorariosEstudianteUseCaseImpl(final InstitutionalScopePort scopePort, final HorarioEstudianteQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort); this.queryPort = Objects.requireNonNull(queryPort);
    }
    @Override public List<HorarioEstudianteDomain> execute(final UUID actorUsuarioId) {
        // CULM-001 A.2-11: sin estudiante, 403; el SP recibe el usuario y solo devuelve matriculas inscritas (A).
        scopePort.findEstudianteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));
        return queryPort.consultarHorarioEstudiante(actorUsuarioId).stream()
                .map(ConsultarHorariosEstudianteRepositoryMapper::toDomain)
                .toList();
    }
}
