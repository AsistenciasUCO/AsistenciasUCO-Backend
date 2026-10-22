package co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.dto.ActualizarAreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.usecase.domain.ActualizarAreaDomain;

public final class ActualizarAreaMapper {

    private ActualizarAreaMapper() {
    }

    public static ActualizarAreaDomain toDomain(final ActualizarAreaDTO dto) {
        if (dto == null) {
            return null;
        }
        return new ActualizarAreaDomain(
                dto.id(),
                dto.codigo(),
                dto.nombre(),
                dto.actorUsuarioId()
        );
    }
}
