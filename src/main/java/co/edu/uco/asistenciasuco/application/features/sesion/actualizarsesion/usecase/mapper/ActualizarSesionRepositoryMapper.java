package co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.domain.ActualizarSesionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

public final class ActualizarSesionRepositoryMapper {

    private ActualizarSesionRepositoryMapper() {
    }

    public static ActualizarSesionRepositoryDTO toRepositoryDTO(final ActualizarSesionDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para actualizar sesion es obligatorio.");
        }

        return new ActualizarSesionRepositoryDTO(
                domain.getSesion(),
                domain.getNombre(),
                domain.getFechaHoraInicio(),
                domain.getFechaHoraFin(),
                domain.getUsuarioEjecutor(),
                domain.getContratoTemporal()
        );
    }
}
