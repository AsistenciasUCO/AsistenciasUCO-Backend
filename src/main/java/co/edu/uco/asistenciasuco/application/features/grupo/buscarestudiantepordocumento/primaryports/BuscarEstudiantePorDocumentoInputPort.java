package co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports;

import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports.dto.BuscarEstudiantePorDocumentoDTO;
import co.edu.uco.asistenciasuco.application.features.grupo.buscarestudiantepordocumento.primaryports.dto.EstudianteBusquedaDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.Optional;

/** CULM-001 A.2-15 (DEC-027): el docente titular busca por documento exacto al estudiante que va a matricular. */
public interface BuscarEstudiantePorDocumentoInputPort
        extends InteractorWithReturn<BuscarEstudiantePorDocumentoDTO, Optional<EstudianteBusquedaDTO>> {
}
