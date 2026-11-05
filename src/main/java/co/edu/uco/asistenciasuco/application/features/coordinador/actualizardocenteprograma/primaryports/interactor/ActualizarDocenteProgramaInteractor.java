package co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.primaryports.ActualizarDocenteProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.primaryports.dto.ActualizarDocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.usecase.ActualizarDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.usecase.domain.ActualizarDocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.mapper.DocenteProgramaMapper;

import java.util.Objects;

public final class ActualizarDocenteProgramaInteractor implements ActualizarDocenteProgramaInputPort {

    private final ActualizarDocenteProgramaUseCase useCase;

    public ActualizarDocenteProgramaInteractor(final ActualizarDocenteProgramaUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public DocenteProgramaDTO execute(final ActualizarDocenteProgramaDTO dto) {
        return DocenteProgramaMapper.toDTO(useCase.execute(new ActualizarDocenteProgramaDomain(
                dto.actorUsuarioId(), dto.idDocente(), dto.idTipoVinculacion(), dto.primerNombre(),
                dto.segundoNombre(), dto.primerApellido(), dto.segundoApellido(),
                dto.idTipoIdentificacion(), dto.numeroIdentificacion(), dto.motivo())));
    }
}
