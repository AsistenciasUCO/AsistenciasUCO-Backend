package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.ConsultarDocentesProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.mapper.ConsultarDocentesProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.ConsultarDocentesProgramaUseCase;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ConsultarDocentesProgramaInteractor implements ConsultarDocentesProgramaInputPort {

    private final ConsultarDocentesProgramaUseCase useCase;

    public ConsultarDocentesProgramaInteractor(final ConsultarDocentesProgramaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "ConsultarDocentesProgramaUseCase es obligatorio.");
    }

    @Override
    public List<DocenteProgramaDTO> execute(final UUID actorUsuarioId) {
        return useCase.execute(actorUsuarioId).stream()
                .map(ConsultarDocentesProgramaMapper::toDTO)
                .toList();
    }
}
