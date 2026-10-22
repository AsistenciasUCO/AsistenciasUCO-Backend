package co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.primaryports.ActualizarFacultadInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.primaryports.dto.ActualizarFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.primaryports.mapper.ActualizarFacultadMapper;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.usecase.ActualizarFacultadUseCase;

import java.util.Objects;

public class ActualizarFacultadInteractor implements ActualizarFacultadInputPort {

    private final ActualizarFacultadUseCase useCase;

    public ActualizarFacultadInteractor(final ActualizarFacultadUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public void execute(final ActualizarFacultadDTO dto) {
        useCase.execute(ActualizarFacultadMapper.toDomain(dto));
    }
}
