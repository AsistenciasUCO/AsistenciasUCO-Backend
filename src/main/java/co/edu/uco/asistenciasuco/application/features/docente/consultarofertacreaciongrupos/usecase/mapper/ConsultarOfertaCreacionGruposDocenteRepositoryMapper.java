package co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.usecase.domain.AsignaturaOfertaDocenteDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsignaturaOfertaDocenteProjection;

public final class ConsultarOfertaCreacionGruposDocenteRepositoryMapper {
    private ConsultarOfertaCreacionGruposDocenteRepositoryMapper() {
    }

    public static AsignaturaOfertaDocenteDomain toDomain(final AsignaturaOfertaDocenteProjection projection) {
        return new AsignaturaOfertaDocenteDomain(projection.idAsignatura(), projection.codigoAsignatura(),
                projection.nombreAsignatura(), projection.idPrograma(), projection.nombrePrograma());
    }
}
