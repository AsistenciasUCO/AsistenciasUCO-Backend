package co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.ConsultarReclamosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ConsultarReclamosDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.dto.ReclamosDocentePaginaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.primaryports.mapper.ConsultarReclamosDocenteMapper;
import co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.ConsultarReclamosDocenteUseCase;

import java.util.Objects;

public final class ConsultarReclamosDocenteInteractor implements ConsultarReclamosDocenteInputPort {

    private final ConsultarReclamosDocenteUseCase useCase;

    public ConsultarReclamosDocenteInteractor(final ConsultarReclamosDocenteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public ReclamosDocentePaginaDTO execute(final ConsultarReclamosDocenteDTO dto) {
        return ConsultarReclamosDocenteMapper.toDTO(useCase.execute(ConsultarReclamosDocenteMapper.toDomain(dto)));
    }
}
