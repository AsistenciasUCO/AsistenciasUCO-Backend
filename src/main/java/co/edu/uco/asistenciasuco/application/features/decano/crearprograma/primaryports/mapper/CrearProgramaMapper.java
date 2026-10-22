package co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.dto.CrearProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.domain.CrearProgramaDomain;

public final class CrearProgramaMapper {

    private CrearProgramaMapper() {
    }

    public static CrearProgramaDomain toDomain(final CrearProgramaDTO dto) {
        if (dto == null) {
            return null;
        }
        return new CrearProgramaDomain(
                dto.idPrograma(),
                dto.codigo(),
                dto.nombre(),
                dto.idCoordinador(),
                dto.idTipoPrograma(),
                dto.actorUsuarioId()
        );
    }
}
