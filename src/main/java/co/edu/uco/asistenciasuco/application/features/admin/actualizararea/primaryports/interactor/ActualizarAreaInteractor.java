package co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.ActualizarAreaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.dto.ActualizarAreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.mapper.ActualizarAreaMapper;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.usecase.ActualizarAreaUseCase;

import java.util.Objects;

public class ActualizarAreaInteractor implements ActualizarAreaInputPort {

    private final ActualizarAreaUseCase useCase;

    public ActualizarAreaInteractor(final ActualizarAreaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso de actualizar area es obligatorio.");
    }

    @Override
    public void execute(final ActualizarAreaDTO dto) {
        useCase.execute(ActualizarAreaMapper.toDomain(dto));
    }
}
