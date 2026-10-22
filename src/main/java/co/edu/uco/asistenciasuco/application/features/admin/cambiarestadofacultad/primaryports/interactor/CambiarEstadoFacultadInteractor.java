package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports.CambiarEstadoFacultadInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports.dto.CambiarEstadoFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports.mapper.CambiarEstadoFacultadMapper;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.usecase.CambiarEstadoFacultadUseCase;

import java.util.Objects;

public class CambiarEstadoFacultadInteractor implements CambiarEstadoFacultadInputPort {

    private final CambiarEstadoFacultadUseCase useCase;

    public CambiarEstadoFacultadInteractor(final CambiarEstadoFacultadUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public void execute(final CambiarEstadoFacultadDTO dto) {
        useCase.execute(CambiarEstadoFacultadMapper.toDomain(dto));
    }
}
