package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain.ProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaProjection;

public final class ConsultarProgramasRepositoryMapper {

    private ConsultarProgramasRepositoryMapper() {
    }

    public static ProgramaDomain toDomain(final ProgramaProjection row) {
        return new ProgramaDomain(
                row.id(),
                row.nombrePrograma(),
                row.idFacultad(),
                row.nombreFacultad(),
                row.idInstitucion(),
                row.nombreInstitucion(),
                row.idCoordinador(),
                row.nombreCoordinador(),
                row.estaActivoPrograma(),
                row.estaActivoTextoPrograma(),
                row.idTipoPrograma(),
                row.nombreTipoPrograma()
        );
    }
}
