package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.ConsultarProgramasUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain.ProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.mapper.ConsultarProgramasRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ConsultarProgramasUseCaseImpl implements ConsultarProgramasUseCase {

    private final InstitutionalScopePort scopePort;
    private final ProgramaQueryPort programaQueryPort;

    public ConsultarProgramasUseCaseImpl(
            final InstitutionalScopePort scopePort,
            final ProgramaQueryPort programaQueryPort
    ) {
        this.scopePort = Objects.requireNonNull(scopePort, "El puerto de alcance institucional es obligatorio.");
        this.programaQueryPort = Objects.requireNonNull(programaQueryPort, "El puerto de consulta de programas es obligatorio.");
    }

    @Override
    public List<ProgramaDomain> execute(final UUID actorUsuarioId) {
        final UUID idFacultad = scopePort.findFacultadIdByDecanoUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver la facultad del decano autenticado."));

        return programaQueryPort.consultarProgramasPorFacultad(idFacultad).stream()
                .filter(Objects::nonNull)
                .map(ConsultarProgramasRepositoryMapper::toDomain)
                .toList();
    }
}
