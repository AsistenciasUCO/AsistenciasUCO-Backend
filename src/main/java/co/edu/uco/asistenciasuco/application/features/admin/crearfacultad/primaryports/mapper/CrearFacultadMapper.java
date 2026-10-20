package co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.dto.CrearFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.usecase.domain.CrearFacultadDomain;

public final class CrearFacultadMapper {

    private CrearFacultadMapper() {
    }

    public static CrearFacultadDomain toDomain(final CrearFacultadDTO dto) {
        if (dto == null) {
            return null;
        }
        return new CrearFacultadDomain(
                dto.id(),
                dto.nombre(),
                dto.idInstitucion(),
                dto.idDecano(),
                dto.actorUsuarioId()
        );
    }
}
