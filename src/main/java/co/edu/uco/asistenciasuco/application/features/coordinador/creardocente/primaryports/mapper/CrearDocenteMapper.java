package co.edu.uco.asistenciasuco.application.features.coordinador.creardocente.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.creardocente.primaryports.dto.CrearDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.creardocente.usecase.domain.CrearDocenteDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

public final class CrearDocenteMapper {

    private CrearDocenteMapper() {
    }

    public static CrearDocenteDomain toDomain(final CrearDocenteDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para crear docente es obligatorio.");
        }

        return new CrearDocenteDomain(
                dto.numeroIdentificacion(),
                dto.primerNombre(),
                dto.segundoNombre(),
                dto.primerApellido(),
                dto.segundoApellido(),
                dto.correo(),
                dto.password(),
                dto.usuario()
        );
    }
}
