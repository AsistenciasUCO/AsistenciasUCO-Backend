package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.dto.ProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain.ProgramaDomain;

public final class ConsultarProgramasMapper {

    private ConsultarProgramasMapper() {
    }

    public static ProgramaDTO toDTO(final ProgramaDomain domain) {
        return new ProgramaDTO(
                domain.id(),
                domain.nombrePrograma(),
                domain.idFacultad(),
                domain.nombreFacultad(),
                domain.idInstitucion(),
                domain.nombreInstitucion(),
                domain.idCoordinador(),
                domain.nombreCoordinador(),
                domain.estaActivoPrograma(),
                domain.estaActivoTextoPrograma(),
                domain.idTipoPrograma(),
                domain.nombreTipoPrograma()
        );
    }
}
