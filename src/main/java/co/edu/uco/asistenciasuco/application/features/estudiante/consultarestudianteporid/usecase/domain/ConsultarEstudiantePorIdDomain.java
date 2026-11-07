package co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudianteporid.usecase.domain;


import co.edu.uco.asistenciasuco.application.features.estudiante.exception.EstudianteErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;

import java.util.UUID;

public record ConsultarEstudiantePorIdDomain(UUID estudianteId, UUID actorUsuarioId) {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    public static ConsultarEstudiantePorIdDomain crear(final UUID estudianteId, final UUID actorUsuarioId) {
        if (estudianteId == null) {
            throw new ValidationException(EstudianteErrorCode.ERR_ESTUDIANTE_ID_REQUERIDO);
        }
        if (EMPTY_UUID.equals(estudianteId)) {
            throw new ValidationException(EstudianteErrorCode.ERR_ESTUDIANTE_ID_INVALIDO);
        }
        return new ConsultarEstudiantePorIdDomain(estudianteId, actorUsuarioId);
    }
}
