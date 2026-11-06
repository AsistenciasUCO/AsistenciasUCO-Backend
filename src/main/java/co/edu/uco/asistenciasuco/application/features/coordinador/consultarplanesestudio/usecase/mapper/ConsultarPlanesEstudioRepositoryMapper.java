package co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.domain.PlanEstudioDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PlanEstudioProjection;

public final class ConsultarPlanesEstudioRepositoryMapper {

    private ConsultarPlanesEstudioRepositoryMapper() {
    }

    public static PlanEstudioDomain toDomain(final PlanEstudioProjection projection) {
        return new PlanEstudioDomain(
                projection.id(),
                projection.idPrograma(),
                projection.nombrePrograma(),
                projection.inp(),
                projection.estaActivoPlanEstudio(),
                projection.estaActivoTextoPlanEstudio(),
                projection.justificacionEstado(),
                projection.totalSemestres(),
                projection.totalAsignaturas(),
                projection.totalCreditos()
        );
    }
}
