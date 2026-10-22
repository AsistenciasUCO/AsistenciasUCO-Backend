package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports.dto.CambiarEstadoFacultadDTO;

public interface CambiarEstadoFacultadInputPort {

    void execute(CambiarEstadoFacultadDTO dto);
}
