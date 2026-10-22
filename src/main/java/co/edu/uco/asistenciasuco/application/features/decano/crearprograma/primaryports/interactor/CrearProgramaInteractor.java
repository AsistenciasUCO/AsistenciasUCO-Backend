package co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.CrearProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.dto.CrearProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.mapper.CrearProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.CrearProgramaUseCase;

import java.util.Objects;

public final class CrearProgramaInteractor implements CrearProgramaInputPort {

    private final CrearProgramaUseCase useCase;

    public CrearProgramaInteractor(final CrearProgramaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso de crear programa es obligatorio.");
    }

    @Override
    public void execute(final CrearProgramaDTO dto) {
        useCase.execute(CrearProgramaMapper.toDomain(dto));
    }
}
