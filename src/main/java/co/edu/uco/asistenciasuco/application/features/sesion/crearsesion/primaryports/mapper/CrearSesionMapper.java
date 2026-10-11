package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain.CrearSesionDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

/**
 * Mapper para convertir el DTO de entrada al dominio del caso de uso.
 */
public final class CrearSesionMapper {

    private CrearSesionMapper() {
    }

    public static CrearSesionDomain toDomain(final CrearSesionDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para crear sesion es obligatorio.");
        }

        return new CrearSesionDomain(
                dto.getGrupo(),
                dto.getNombre(),
                dto.getFechaHoraInicio(),
                dto.getFechaHoraFin(),
                dto.getUsuarioEjecutor(),
                dto.getContratoTemporal()
        );
    }
}
