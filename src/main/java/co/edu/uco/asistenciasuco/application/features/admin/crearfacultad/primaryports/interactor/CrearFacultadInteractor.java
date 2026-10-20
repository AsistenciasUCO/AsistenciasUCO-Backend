package co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.CrearFacultadInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.dto.CrearFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.mapper.CrearFacultadMapper;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.usecase.CrearFacultadUseCase;

import java.util.Objects;

public class CrearFacultadInteractor implements CrearFacultadInputPort {

    private final CrearFacultadUseCase useCase;

    public CrearFacultadInteractor(final CrearFacultadUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso de crear facultad es obligatorio.");
    }

    @Override
    public void execute(final CrearFacultadDTO dto) {
        useCase.execute(CrearFacultadMapper.toDomain(dto));
    }
}
