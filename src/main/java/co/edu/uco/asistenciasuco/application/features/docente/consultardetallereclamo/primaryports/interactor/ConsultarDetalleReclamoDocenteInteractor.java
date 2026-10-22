package co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.ConsultarDetalleReclamoDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto.ConsultarDetalleReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.dto.DetalleReclamoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.primaryports.mapper.ConsultarDetalleReclamoDocenteMapper;
import co.edu.uco.asistenciasuco.application.features.docente.consultardetallereclamo.usecase.ConsultarDetalleReclamoDocenteUseCase;

import java.util.Objects;

public final class ConsultarDetalleReclamoDocenteInteractor implements ConsultarDetalleReclamoDocenteInputPort {

    private final ConsultarDetalleReclamoDocenteUseCase useCase;

    public ConsultarDetalleReclamoDocenteInteractor(final ConsultarDetalleReclamoDocenteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public DetalleReclamoDocenteDTO execute(final ConsultarDetalleReclamoDocenteDTO dto) {
        return ConsultarDetalleReclamoDocenteMapper.toDTO(useCase.execute(ConsultarDetalleReclamoDocenteMapper.toDomain(dto)));
    }
}
