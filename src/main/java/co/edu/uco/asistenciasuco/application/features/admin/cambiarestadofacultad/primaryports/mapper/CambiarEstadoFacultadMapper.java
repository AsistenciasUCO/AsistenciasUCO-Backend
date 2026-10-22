package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports.dto.CambiarEstadoFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.usecase.domain.CambiarEstadoFacultadDomain;

public final class CambiarEstadoFacultadMapper {

    private CambiarEstadoFacultadMapper() {
    }

    public static CambiarEstadoFacultadDomain toDomain(final CambiarEstadoFacultadDTO dto) {
        if (dto == null) {
            return null;
        }
        return new CambiarEstadoFacultadDomain(dto.id(), dto.activa(), dto.actorUsuarioId());
    }
}
