package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.primaryports;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.SesionV2ConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.primaryports.dto.ConsultarSesionesPorGrupoV2DTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.List;

/**
 * Puerto de entrada para listar por el contrato v2 todas las sesiones de un grupo (confirmadas e
 * indeterminadas, sin ocultar historicos).
 */
public interface ConsultarSesionesPorGrupoV2InputPort
        extends InteractorWithReturn<ConsultarSesionesPorGrupoV2DTO, List<SesionV2ConsultadaDTO>> {
}
