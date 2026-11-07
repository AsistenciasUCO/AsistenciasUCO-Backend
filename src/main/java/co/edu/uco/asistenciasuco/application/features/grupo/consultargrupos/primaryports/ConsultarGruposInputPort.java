package co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports;

import java.util.UUID;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.dto.GrupoDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.List;

/**
 * Puerto de entrada para consultar grupos.
 */
public interface ConsultarGruposInputPort extends InteractorWithReturn<UUID, List<GrupoDTO>> {
}
