package co.edu.uco.asistenciasuco.application.features.admin.creararea.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.usecase.CrearAreaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.usecase.domain.CrearAreaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaCommandPort;

import java.util.Objects;
import java.util.UUID;

public class CrearAreaUseCaseImpl implements CrearAreaUseCase {

    private final AreaCommandPort areaCommandPort;

    public CrearAreaUseCaseImpl(final AreaCommandPort areaCommandPort) {
        this.areaCommandPort = Objects.requireNonNull(areaCommandPort, "El puerto de comando de areas es obligatorio.");
    }

    @Override
    public void execute(final CrearAreaDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos para crear el area son obligatorios.");
        }

        final String codigo = domain.codigo() != null ? domain.codigo().trim().toUpperCase() : null;
        if (codigo == null || codigo.isEmpty() || codigo.length() > 3) {
            throw new ValidationException("El codigo del area es obligatorio y debe tener maximo 3 caracteres.");
        }

        final String nombre = domain.nombre() != null ? domain.nombre().trim() : null;
        if (nombre == null || nombre.length() < 3 || nombre.length() > 50) {
            throw new ValidationException("El nombre del area es obligatorio y debe tener entre 3 y 50 caracteres.");
        }

        final UUID idArea = domain.id() != null ? domain.id() : UUID.randomUUID();

        areaCommandPort.crearArea(new AreaCommandPort.CrearAreaCommand(
                idArea,
                codigo,
                nombre,
                domain.actorUsuarioId()
        ));
    }
}
