package co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.domain.AsignaturaDocenteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaDocenteProjection;

public final class ConsultarAsignaturasDocenteRepositoryMapper {

    private ConsultarAsignaturasDocenteRepositoryMapper() {
    }

    public static AsignaturaDocenteDomain toDomain(final AsignaturaDocenteProjection projection) {
        return new AsignaturaDocenteDomain(projection.idAsignatura(), projection.nombreAsignatura(),
                projection.idGrupo(), projection.nombreGrupo(), projection.idPrograma(), projection.nombrePrograma(),
                projection.idTipoPrograma(), projection.nombreTipoPrograma(), projection.estudiantesActivos());
    }
}
