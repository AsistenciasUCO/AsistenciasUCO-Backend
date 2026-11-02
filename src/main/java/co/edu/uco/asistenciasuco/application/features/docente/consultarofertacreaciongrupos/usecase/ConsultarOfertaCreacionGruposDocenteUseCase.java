package co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.usecase;

import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.usecase.domain.OfertaCreacionGruposDocenteDomain;
import java.util.UUID;

public interface ConsultarOfertaCreacionGruposDocenteUseCase {
    OfertaCreacionGruposDocenteDomain execute(UUID actorUsuarioId);
}
