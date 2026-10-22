package co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.dto.AreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.usecase.domain.AreaDomain;

public final class ConsultarAreasMapper {

    private ConsultarAreasMapper() {
    }

    public static AreaDTO toDTO(final AreaDomain domain) {
        return new AreaDTO(domain.id(), domain.codigo(), domain.nombre(), domain.totalAsignaturas());
    }
}
