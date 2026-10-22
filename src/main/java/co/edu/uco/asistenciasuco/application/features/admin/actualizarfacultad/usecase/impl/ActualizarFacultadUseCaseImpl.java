package co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.usecase.ActualizarFacultadUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.usecase.domain.ActualizarFacultadDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadCommandPort;

import java.util.Objects;

public class ActualizarFacultadUseCaseImpl implements ActualizarFacultadUseCase {

    private final FacultadCommandPort facultadCommandPort;

    public ActualizarFacultadUseCaseImpl(final FacultadCommandPort facultadCommandPort) {
        this.facultadCommandPort = Objects.requireNonNull(facultadCommandPort, "El puerto de comando de facultades es obligatorio.");
    }

    @Override
    public void execute(final ActualizarFacultadDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos de la facultad son obligatorios.");
        }
        if (domain.id() == null) {
            throw new ValidationException("El identificador de la facultad es obligatorio.");
        }
        if (domain.nombre() == null && domain.idDecano() == null) {
            throw new ValidationException("Debe indicar al menos el nombre o el decano de la facultad.");
        }

        String nombre = null;
        if (domain.nombre() != null) {
            nombre = domain.nombre().trim();
            if (nombre.length() < 3 || nombre.length() > 50) {
                throw new ValidationException("El nombre de la facultad debe tener entre 3 y 50 caracteres.");
            }
        }

        facultadCommandPort.actualizarFacultad(new FacultadCommandPort.ActualizarFacultadCommand(
                domain.id(),
                nombre,
                domain.idDecano(),
                domain.actorUsuarioId()
        ));
    }
}
