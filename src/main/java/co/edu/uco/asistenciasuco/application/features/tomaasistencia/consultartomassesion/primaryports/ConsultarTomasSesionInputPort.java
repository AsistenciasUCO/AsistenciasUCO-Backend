package co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultartomassesion.primaryports;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomasSesionResumen;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/**
 * CULM-001 B.2 (contrato 3.2.5): tomas de una sesion del docente titular con la ventana de registro que calcula la base de
 * datos, el estado de la sesion y su cancelacion (explica por que no se pueden abrir tomas).
 */
public interface ConsultarTomasSesionInputPort
        extends InteractorWithReturn<ConsultarTomasSesionInputPort.Consulta, ConsultarTomasSesionInputPort.Resultado> {

    record Consulta(UUID sesionId, UUID usuarioEjecutor) {
    }

    record Resultado(EstadoSesion estado, CancelacionSesionDTO cancelacion, TomasSesionResumen tomas) {
    }
}
