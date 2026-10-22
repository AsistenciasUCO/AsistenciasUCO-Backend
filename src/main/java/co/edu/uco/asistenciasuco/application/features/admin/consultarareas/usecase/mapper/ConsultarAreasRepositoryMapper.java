package co.edu.uco.asistenciasuco.application.features.admin.consultarareas.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.usecase.domain.AreaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AreaProjection;

public final class ConsultarAreasRepositoryMapper {

    private ConsultarAreasRepositoryMapper() {
    }

    public static AreaDomain toDomain(final AreaProjection projection) {
        return new AreaDomain(projection.id(), projection.codigo(), projection.nombre(), projection.totalAsignaturas());
    }
}
