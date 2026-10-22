package co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.primaryports.ConsultarTiposProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.primaryports.dto.TipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.primaryports.mapper.ConsultarTiposProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.usecase.ConsultarTiposProgramaUseCase;

import java.util.List;
import java.util.Objects;

public final class ConsultarTiposProgramaInteractor implements ConsultarTiposProgramaInputPort {

    private final ConsultarTiposProgramaUseCase useCase;

    public ConsultarTiposProgramaInteractor(final ConsultarTiposProgramaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso de consultar tipos de programa es obligatorio.");
    }

    @Override
    public List<TipoProgramaDTO> execute() {
        return useCase.execute().stream()
                .map(ConsultarTiposProgramaMapper::toDTO)
                .toList();
    }
}
