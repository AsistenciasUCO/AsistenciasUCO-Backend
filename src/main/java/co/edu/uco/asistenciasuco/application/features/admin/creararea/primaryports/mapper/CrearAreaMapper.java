package co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.dto.CrearAreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.usecase.domain.CrearAreaDomain;

public final class CrearAreaMapper {

    private CrearAreaMapper() {
    }

    public static CrearAreaDomain toDomain(final CrearAreaDTO dto) {
        if (dto == null) {
            return null;
        }
        return new CrearAreaDomain(
                dto.id(),
                dto.codigo(),
                dto.nombre(),
                dto.actorUsuarioId()
        );
    }
}
