package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.primaryports.GuardarTipoIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.primaryports.dto.GuardarTipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.usecase.GuardarTipoIdentificacionUseCase;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.usecase.domain.GuardarTipoIdentificacionDomain;

import java.util.Objects;

public class GuardarTipoIdentificacionInteractor implements GuardarTipoIdentificacionInputPort {

    private final GuardarTipoIdentificacionUseCase useCase;

    public GuardarTipoIdentificacionInteractor(final GuardarTipoIdentificacionUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public void execute(final GuardarTipoIdentificacionDTO dto) {
        useCase.execute(dto == null ? null
                : new GuardarTipoIdentificacionDomain(dto.id(), dto.codigo(), dto.nombre(), dto.actorUsuarioId()));
    }
}
