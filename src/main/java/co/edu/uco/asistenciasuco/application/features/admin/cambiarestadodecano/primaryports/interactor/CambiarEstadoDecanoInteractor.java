package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.dto.DecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.primaryports.CambiarEstadoDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.primaryports.dto.CambiarEstadoDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.usecase.CambiarEstadoDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadodecano.usecase.domain.CambiarEstadoDecanoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DecanoProjection;

import java.util.Objects;

public final class CambiarEstadoDecanoInteractor implements CambiarEstadoDecanoInputPort {

    private final CambiarEstadoDecanoUseCase useCase;

    public CambiarEstadoDecanoInteractor(final CambiarEstadoDecanoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "CambiarEstadoDecanoUseCase es obligatorio.");
    }

    @Override
    public DecanoDTO execute(final CambiarEstadoDecanoDTO dto) {
        if (dto == null) {
            throw new ValidationException("Los datos del cambio de estado del decano son obligatorios.");
        }
        final DecanoProjection d = useCase.execute(new CambiarEstadoDecanoDomain(dto.idDecano(), dto.actorUsuarioId()));
        return new DecanoDTO(d.id(), d.idUsuario(), d.numeroIdentificacion(), d.nombreCompleto(), d.idFacultad(),
                d.nombreFacultad(), d.estaActivoDecano());
    }
}
