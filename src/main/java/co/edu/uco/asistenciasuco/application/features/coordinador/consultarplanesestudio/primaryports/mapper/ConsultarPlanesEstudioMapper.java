package co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.primaryports.dto.PlanEstudioDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarplanesestudio.usecase.domain.PlanEstudioDomain;

public final class ConsultarPlanesEstudioMapper {

    private ConsultarPlanesEstudioMapper() {
    }

    public static PlanEstudioDTO toDTO(final PlanEstudioDomain domain) {
        return new PlanEstudioDTO(
                domain.id(),
                domain.idPrograma(),
                domain.nombrePrograma(),
                domain.inp(),
                domain.estaActivoPlanEstudio(),
                domain.estaActivoTextoPlanEstudio(),
                domain.justificacionEstado(),
                domain.totalSemestres(),
                domain.totalAsignaturas(),
                domain.totalCreditos()
        );
    }
}
