package co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.primaryports.RetirarReclamoInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.primaryports.dto.RetirarReclamoDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.usecase.RetirarReclamoUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarreclamo.usecase.domain.RetirarReclamoDomain;

import java.util.Objects;

public final class RetirarReclamoInteractor implements RetirarReclamoInputPort {

    private final RetirarReclamoUseCase useCase;

    public RetirarReclamoInteractor(final RetirarReclamoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "RetirarReclamoUseCase es obligatorio.");
    }

    @Override
    public String execute(final RetirarReclamoDTO dto) {
        if (dto == null) {
            throw new ValidationException("Los datos del retiro del reclamo son obligatorios.");
        }
        return useCase.execute(new RetirarReclamoDomain(dto.idReclamo(), dto.actorUsuarioId()));
    }
}
