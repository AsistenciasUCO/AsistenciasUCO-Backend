package co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.ConsultarOfertaCreacionGruposDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.dto.OfertaCreacionGruposDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.primaryports.mapper.ConsultarOfertaCreacionGruposDocenteMapper;
import co.edu.uco.asistenciasuco.application.features.docente.consultarofertacreaciongrupos.usecase.ConsultarOfertaCreacionGruposDocenteUseCase;
import java.util.Objects;
import java.util.UUID;

public final class ConsultarOfertaCreacionGruposDocenteInteractor implements ConsultarOfertaCreacionGruposDocenteInputPort {
    private final ConsultarOfertaCreacionGruposDocenteUseCase useCase;

    public ConsultarOfertaCreacionGruposDocenteInteractor(final ConsultarOfertaCreacionGruposDocenteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase);
    }

    @Override
    public OfertaCreacionGruposDocenteDTO execute(final UUID actorUsuarioId) {
        return ConsultarOfertaCreacionGruposDocenteMapper.toDTO(useCase.execute(actorUsuarioId));
    }
}
