package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.usecase;

import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.SesionProcedenciaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.usecase.domain.ConsultarSesionesPorGrupoV2Domain;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

import java.util.List;

/**
 * Caso de uso para listar las sesiones v2 de un grupo.
 */
public interface ConsultarSesionesPorGrupoV2UseCase
        extends UseCaseWithReturn<ConsultarSesionesPorGrupoV2Domain, List<SesionProcedenciaEntity>> {
}
