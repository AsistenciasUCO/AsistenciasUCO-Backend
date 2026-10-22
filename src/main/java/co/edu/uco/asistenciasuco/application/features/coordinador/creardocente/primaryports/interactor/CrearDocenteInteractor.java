package co.edu.uco.asistenciasuco.application.features.coordinador.creardocente.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.creardocente.primaryports.CrearDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.creardocente.primaryports.dto.CrearDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.creardocente.primaryports.mapper.CrearDocenteMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.creardocente.usecase.CrearDocenteUseCase;

import java.util.Objects;

public final class CrearDocenteInteractor implements CrearDocenteInputPort {

    private final CrearDocenteUseCase useCase;

    public CrearDocenteInteractor(final CrearDocenteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "CrearDocenteUseCase es obligatorio.");
    }

    @Override
    public void execute(final CrearDocenteDTO dto) {
        useCase.execute(CrearDocenteMapper.toDomain(dto));
    }
}
