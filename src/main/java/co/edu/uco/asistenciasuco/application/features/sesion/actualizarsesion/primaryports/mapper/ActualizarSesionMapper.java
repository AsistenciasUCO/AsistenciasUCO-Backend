package co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.primaryports.dto.ActualizarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.domain.ActualizarSesionDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

public final class ActualizarSesionMapper {

    private ActualizarSesionMapper() {
    }

    public static ActualizarSesionDomain toDomain(final ActualizarSesionDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para actualizar sesion es obligatorio.");
        }

        return new ActualizarSesionDomain(
                dto.getSesion(),
                dto.getNombre(),
                dto.getFechaHoraInicio(),
                dto.getFechaHoraFin(),
                dto.getUsuarioEjecutor(),
                dto.getContratoTemporal()
        );
    }
}
