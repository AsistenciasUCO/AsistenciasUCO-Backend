package co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.primaryports.dto.ActualizarFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.usecase.domain.ActualizarFacultadDomain;

public final class ActualizarFacultadMapper {

    private ActualizarFacultadMapper() {
    }

    public static ActualizarFacultadDomain toDomain(final ActualizarFacultadDTO dto) {
        if (dto == null) {
            return null;
        }
        return new ActualizarFacultadDomain(dto.id(), dto.nombre(), dto.idDecano(), dto.actorUsuarioId());
    }
}
