package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.usecase;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DecanoProjection;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.usecase.domain.CambiarEstadoDecanoDomain;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

public interface CambiarEstadoDecanoUseCase extends UseCaseWithReturn<CambiarEstadoDecanoDomain, DecanoProjection> {
}
