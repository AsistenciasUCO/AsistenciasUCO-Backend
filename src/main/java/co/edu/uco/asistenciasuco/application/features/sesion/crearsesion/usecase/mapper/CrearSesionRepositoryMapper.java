package co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.domain.CrearSesionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

/**
 * Mapper entre el dominio del caso de uso y el contrato del puerto secundario.
 */
public final class CrearSesionRepositoryMapper {

    private CrearSesionRepositoryMapper() {
    }

    public static CrearSesionRepositoryDTO toRepositoryDTO(final CrearSesionDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para crear sesion es obligatorio.");
        }

        return new CrearSesionRepositoryDTO(
                domain.getGrupo(),
                domain.getNombre(),
                domain.getFechaHoraInicio(),
                domain.getFechaHoraFin(),
                domain.getUsuarioEjecutor(),
                domain.getContratoTemporal()
        );
    }
}
