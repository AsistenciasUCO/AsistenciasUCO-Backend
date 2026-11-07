package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.impl;


import co.edu.uco.asistenciasuco.application.features.estudiante.exception.EstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.ConsultarEstudiantePorIdUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.domain.ConsultarEstudiantePorIdDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.entity.EstudianteDetalleEntity;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.mapper.ConsultarEstudiantePorIdRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import java.util.Objects;

public final class ConsultarEstudiantePorIdUseCaseImpl implements ConsultarEstudiantePorIdUseCase {

    private final EstudianteRepositoryPort estudianteRepositoryPort;

    public ConsultarEstudiantePorIdUseCaseImpl(final EstudianteRepositoryPort estudianteRepositoryPort) {
        this.estudianteRepositoryPort = Objects.requireNonNull(estudianteRepositoryPort, "El puerto de salida EstudianteRepositoryPort es obligatorio.");
    }

    @Override
    public EstudianteDetalleEntity execute(final ConsultarEstudiantePorIdDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para consultar estudiante por ID es obligatorio.");
        }
        if (ObjectHelper.isNull(domain.actorUsuarioId())) {
            throw new CrosscuttingException("El usuario ejecutor de la consulta del estudiante es obligatorio.");
        }
        // CULM-001 A.2-1: fuera del ambito del ejecutor el SP no devuelve filas y se responde 404 (no se revela la existencia).
        return estudianteRepositoryPort.consultarEstudiantePorId(domain.estudianteId(), domain.actorUsuarioId())
                .map(ConsultarEstudiantePorIdRepositoryMapper::toUseCaseEntity)
                .orElseThrow(() -> new ResourceNotFoundException(EstudianteErrorCode.ERR_ESTUDIANTE_NO_EXISTE));
    }
}