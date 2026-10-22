package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.usecase.CambiarEstadoFacultadUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.usecase.domain.CambiarEstadoFacultadDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadCommandPort;

import java.util.Objects;

public class CambiarEstadoFacultadUseCaseImpl implements CambiarEstadoFacultadUseCase {

    private final FacultadCommandPort facultadCommandPort;

    public CambiarEstadoFacultadUseCaseImpl(final FacultadCommandPort facultadCommandPort) {
        this.facultadCommandPort = Objects.requireNonNull(facultadCommandPort, "El puerto de comando de facultades es obligatorio.");
    }

    @Override
    public void execute(final CambiarEstadoFacultadDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos de la facultad son obligatorios.");
        }
        if (domain.id() == null) {
            throw new ValidationException("El identificador de la facultad es obligatorio.");
        }
        if (domain.activa() == null) {
            throw new ValidationException("El estado deseado de la facultad es obligatorio.");
        }

        facultadCommandPort.cambiarEstadoFacultad(new FacultadCommandPort.CambiarEstadoFacultadCommand(
                domain.id(),
                domain.activa(),
                domain.actorUsuarioId()
        ));
    }
}
