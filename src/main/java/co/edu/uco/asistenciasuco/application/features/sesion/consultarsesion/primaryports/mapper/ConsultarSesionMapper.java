package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.ConsultarSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.primaryports.dto.SesionConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.domain.ConsultarSesionDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity.SesionConsultadaEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

/**
 * Mapper para convertir entre DTOs y modelos internos del caso de uso.
 */
public final class ConsultarSesionMapper {

    private ConsultarSesionMapper() {
    }

    public static ConsultarSesionDomain toDomain(final ConsultarSesionDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El DTO para consultar sesion es obligatorio.");
        }

        return new ConsultarSesionDomain(dto.getSesion(), dto.getUsuarioEjecutor());
    }

    public static SesionConsultadaDTO toDTO(final SesionConsultadaEntity entity) {
        if (ObjectHelper.isNull(entity)) {
            throw new CrosscuttingException("La sesion consultada es obligatoria.");
        }

        return new SesionConsultadaDTO(
                entity.getSesion(),
                entity.getGrupo(),
                entity.getNombre(),
                entity.getNumero(),
                entity.getCodigo(),
                entity.getNumeroSemana(),
                entity.getCodigoGrupo(),
                entity.getNombreGrupo(),
                entity.getFechaHoraInicio(),
                entity.getFechaHoraFin(),
                entity.getCancelacion()
        );
    }
}
