package co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.dto.ActualizarAreaDTO;

public interface ActualizarAreaInputPort {
    void execute(ActualizarAreaDTO dto);
}
