package co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.dto.AsignaturaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.domain.AsignaturaDocenteDomain;

public final class ConsultarAsignaturasDocenteMapper {

    private ConsultarAsignaturasDocenteMapper() {
    }

    public static AsignaturaDocenteDTO toDTO(final AsignaturaDocenteDomain domain) {
        return new AsignaturaDocenteDTO(domain.idAsignatura(), domain.nombreAsignatura(), domain.idGrupo(),
                domain.nombreGrupo(), domain.idPrograma(), domain.nombrePrograma(), domain.estudiantesActivos());
    }
}
