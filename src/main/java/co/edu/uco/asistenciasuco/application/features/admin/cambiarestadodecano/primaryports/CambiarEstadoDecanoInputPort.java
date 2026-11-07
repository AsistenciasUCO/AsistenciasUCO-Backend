package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.dto.DecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.primaryports.dto.CambiarEstadoDecanoDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

/** CULM-001 A.2-8: activa o desactiva al decano y devuelve su fila con el estado nuevo. */
public interface CambiarEstadoDecanoInputPort extends InteractorWithReturn<CambiarEstadoDecanoDTO, DecanoDTO> {
}
