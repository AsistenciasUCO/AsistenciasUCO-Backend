package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.dto.ConsultarEstudiantePorIdDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.ConsultarEstudiantePorIdInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.dto.EstudianteDetalleDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.primaryports.mapper.ConsultarEstudiantePorIdMapper;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.ConsultarEstudiantePorIdUseCase;

import java.util.UUID;
import java.util.Objects;

public final class ConsultarEstudiantePorIdInteractor implements ConsultarEstudiantePorIdInputPort {

    private final ConsultarEstudiantePorIdUseCase useCase;

    public ConsultarEstudiantePorIdInteractor(final ConsultarEstudiantePorIdUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso ConsultarEstudiantePorIdUseCase es obligatorio.");
    }

    @Override
    public EstudianteDetalleDTO execute(final ConsultarEstudiantePorIdDTO dto) {
        return ConsultarEstudiantePorIdMapper.toDTO(useCase.execute(ConsultarEstudiantePorIdMapper.toDomain(dto)));
    }
}