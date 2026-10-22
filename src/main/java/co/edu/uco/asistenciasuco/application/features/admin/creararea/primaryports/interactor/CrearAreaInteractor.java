package co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.CrearAreaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.dto.CrearAreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.mapper.CrearAreaMapper;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.usecase.CrearAreaUseCase;

import java.util.Objects;

public class CrearAreaInteractor implements CrearAreaInputPort {

    private final CrearAreaUseCase useCase;

    public CrearAreaInteractor(final CrearAreaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso de crear area es obligatorio.");
    }

    @Override
    public void execute(final CrearAreaDTO dto) {
        useCase.execute(CrearAreaMapper.toDomain(dto));
    }
}
