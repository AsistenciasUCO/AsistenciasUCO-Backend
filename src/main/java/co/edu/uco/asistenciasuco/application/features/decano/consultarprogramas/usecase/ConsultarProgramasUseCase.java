package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase;

import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain.ProgramaDomain;

import java.util.List;
import java.util.UUID;

public interface ConsultarProgramasUseCase {

    List<ProgramaDomain> execute(UUID actorUsuarioId);
}
