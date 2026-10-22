package co.edu.uco.asistenciasuco.application.features.admin.actualizararea.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.usecase.ActualizarAreaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.usecase.domain.ActualizarAreaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaCommandPort;

import java.util.Objects;

public class ActualizarAreaUseCaseImpl implements ActualizarAreaUseCase {

    private final AreaCommandPort areaCommandPort;

    public ActualizarAreaUseCaseImpl(final AreaCommandPort areaCommandPort) {
        this.areaCommandPort = Objects.requireNonNull(areaCommandPort, "El puerto de comando de areas es obligatorio.");
    }

    @Override
    public void execute(final ActualizarAreaDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos para actualizar el area son obligatorios.");
        }

        if (domain.id() == null) {
            throw new ValidationException("El identificador del area a actualizar es obligatorio.");
        }

        final String codigo = domain.codigo() != null ? domain.codigo().trim().toUpperCase() : null;
        if (codigo == null || codigo.isEmpty() || codigo.length() > 3) {
            throw new ValidationException("El codigo del area es obligatorio y debe tener maximo 3 caracteres.");
        }

        final String nombre = domain.nombre() != null ? domain.nombre().trim() : null;
        if (nombre == null || nombre.length() < 3 || nombre.length() > 50) {
            throw new ValidationException("El nombre del area es obligatorio y debe tener entre 3 y 50 caracteres.");
        }

        areaCommandPort.actualizarArea(new AreaCommandPort.ActualizarAreaCommand(
                domain.id(),
                codigo,
                nombre,
                domain.actorUsuarioId()
        ));
    }
}
