package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto.SolicitarRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.domain.SolicitarRevisionAsistenciaDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

/**
 * Mapper para convertir el DTO de entrada al dominio del caso de uso.
 */
public final class SolicitarRevisionAsistenciaMapper {

    private SolicitarRevisionAsistenciaMapper() {
    }

    public static SolicitarRevisionAsistenciaDomain toDomain(final SolicitarRevisionAsistenciaDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para solicitar revision de asistencia es obligatorio.");
        }

        return new SolicitarRevisionAsistenciaDomain(
                dto.getSesion(),
                dto.getCategoria(),
                dto.getJustificacion(),
                dto.getSoporteArchivoId(),
                dto.getUsuario()
        );
    }
}
