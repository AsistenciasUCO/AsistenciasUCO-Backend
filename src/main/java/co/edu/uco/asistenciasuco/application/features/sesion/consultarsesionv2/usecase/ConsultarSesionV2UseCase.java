package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.usecase;

import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.SesionProcedenciaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.usecase.domain.ConsultarSesionV2Domain;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithReturn;

/**
 * Caso de uso para consultar una sesion por el contrato v2.
 */
public interface ConsultarSesionV2UseCase extends UseCaseWithReturn<ConsultarSesionV2Domain, SesionProcedenciaEntity> {
}
