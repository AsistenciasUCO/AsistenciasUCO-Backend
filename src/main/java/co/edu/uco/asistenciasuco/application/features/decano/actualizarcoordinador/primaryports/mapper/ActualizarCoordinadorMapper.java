package co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.dto.ActualizarCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.usecase.domain.ActualizarCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ResultadoGestionCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.decano.common.dto.ResultadoGestionCoordinadorDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

/**
 * Mapper entre los DTO del puerto primario y el dominio de editar coordinador.
 */
public final class ActualizarCoordinadorMapper {

    private ActualizarCoordinadorMapper() {
    }

    public static ActualizarCoordinadorDomain toDomain(final ActualizarCoordinadorDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para actualizar coordinador es obligatorio.");
        }
        return new ActualizarCoordinadorDomain(dto.idCoordinador(), dto.primerNombre(), dto.segundoNombre(),
                dto.primerApellido(), dto.segundoApellido(), dto.correo(), dto.usuario(),
                dto.idTipoIdentificacion(), dto.numeroIdentificacion(), dto.motivo());
    }

    public static ResultadoGestionCoordinadorDTO toDTO(final ResultadoGestionCoordinadorDomain domain) {
        return new ResultadoGestionCoordinadorDTO(
                domain.idCoordinador(),
                domain.sincronizacionPendiente()
                        ? ResultadoGestionCoordinadorDTO.SINCRONIZACION_PENDIENTE
                        : ResultadoGestionCoordinadorDTO.SINCRONIZACION_OK,
                domain.mensajeUsuario());
    }
}
