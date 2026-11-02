package co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports;

import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.dto.OfertaCreacionGruposDocenteDTO;
import java.util.UUID;

public interface ConsultarOfertaCreacionGruposDocenteInputPort {
    OfertaCreacionGruposDocenteDTO execute(UUID actorUsuarioId);
}
