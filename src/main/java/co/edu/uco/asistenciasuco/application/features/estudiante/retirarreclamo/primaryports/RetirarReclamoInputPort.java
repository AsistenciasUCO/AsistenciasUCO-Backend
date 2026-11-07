package co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.primaryports;

import co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.primaryports.dto.RetirarReclamoDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

/** CULM-001 A.2-7 (HU010/HU012): el estudiante retira su reclamo pendiente; devuelve el mensaje institucional. */
public interface RetirarReclamoInputPort extends InteractorWithReturn<RetirarReclamoDTO, String> {
}
