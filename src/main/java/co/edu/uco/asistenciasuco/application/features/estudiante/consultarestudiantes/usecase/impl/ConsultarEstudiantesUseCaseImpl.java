package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.ConsultarEstudiantesUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.domain.ConsultarEstudiantesDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.entity.EstudiantePaginaEntity;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.mapper.ConsultarEstudiantesRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import java.util.Objects;

public final class ConsultarEstudiantesUseCaseImpl implements ConsultarEstudiantesUseCase {

    private final EstudianteRepositoryPort estudianteRepositoryPort;

    public ConsultarEstudiantesUseCaseImpl(final EstudianteRepositoryPort estudianteRepositoryPort) {
        this.estudianteRepositoryPort = Objects.requireNonNull(estudianteRepositoryPort, "El puerto de salida EstudianteRepositoryPort es obligatorio.");
    }

    @Override
    public EstudiantePaginaEntity execute(final ConsultarEstudiantesDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio de consulta de estudiantes es obligatorio.");
        }
        // CULM-001 A.2-1: el ambito (ADMINISTRADOR institucion, COORDINADOR programas, DOCENTE grupos propios) lo
        // aplica usp_consultar_directorio_estudiantes_paginado con el ejecutor autenticado.
        if (ObjectHelper.isNull(domain.actorUsuarioId())) {
            throw new CrosscuttingException("El usuario ejecutor de la consulta de estudiantes es obligatorio.");
        }
        return ConsultarEstudiantesRepositoryMapper.toUseCaseEntity(
                estudianteRepositoryPort.consultarEstudiantes(
                        ConsultarEstudiantesRepositoryMapper.toRepositoryDTO(domain)
                )
        );
    }
}