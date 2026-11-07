package co.edu.uco.asistenciasuco.application.features.sesioncancelacion.cancelarsesion.primaryports;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/**
 * CULM-001 B.1 (HU047): el docente titular cancela una sesion futura o sin asistencia registrada, con un motivo. Se avisa
 * a los estudiantes activos del grupo (notificacion {@code SESION_CANCELADA}) y al canal SSE del grupo.
 */
public interface CancelarSesionInputPort
        extends InteractorWithReturn<CancelarSesionInputPort.Comando, CancelarSesionInputPort.Resultado> {

    record Comando(UUID sesionId, String motivo, UUID usuarioEjecutor) {
    }

    /** {@code estudiantesNotificados} = avisos encolados (mejor esfuerzo: un fallo no cambia la respuesta). */
    record Resultado(UUID idSesion, UUID idGrupo, EstadoSesion estado, CancelacionSesionDTO cancelacion,
                     int estudiantesNotificados) {
    }
}
