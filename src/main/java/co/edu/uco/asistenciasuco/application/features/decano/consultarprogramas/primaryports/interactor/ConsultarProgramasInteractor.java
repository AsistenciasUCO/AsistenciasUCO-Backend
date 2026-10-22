package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.ConsultarProgramasInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto.ProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.mapper.ConsultarProgramasMapper;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.ConsultarProgramasUseCase;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ConsultarProgramasInteractor implements ConsultarProgramasInputPort {

    private final ConsultarProgramasUseCase useCase;

    public ConsultarProgramasInteractor(final ConsultarProgramasUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso de consultar programas es obligatorio.");
    }

    @Override
    public List<ProgramaDTO> execute(final UUID actorUsuarioId) {
        return useCase.execute(actorUsuarioId).stream()
                .map(ConsultarProgramasMapper::toDTO)
                .toList();
    }
}
