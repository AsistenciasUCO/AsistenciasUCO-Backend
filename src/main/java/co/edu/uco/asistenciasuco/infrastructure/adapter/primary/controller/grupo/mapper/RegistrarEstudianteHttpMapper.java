package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.mapper;

import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.dto.RegistrarEstudianteDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.grupo.request.RegistrarEstudianteRequest;

import java.util.Objects;
import java.util.UUID;

/**
 * Mapper del contrato HTTP al input de application.
 */
public final class RegistrarEstudianteHttpMapper {

    private RegistrarEstudianteHttpMapper() {
    }

    public static RegistrarEstudianteDTO toApplicationDTO(
            final UUID grupoId,
            final RegistrarEstudianteRequest request,
            final UUID usuarioEjecutor
    ) {
        Objects.requireNonNull(grupoId, "El identificador del grupo es obligatorio para realizar el mapping.");
        Objects.requireNonNull(request, "El request HTTP para registrar estudiante en grupo es obligatorio.");
        Objects.requireNonNull(usuarioEjecutor, "El usuario ejecutor autenticado es obligatorio para realizar el mapping.");
        return new RegistrarEstudianteDTO(
                request.getEstudianteId(),
                request.getTipoIdentificacionId(),
                request.getNumeroIdentificacion(),
                request.getPrimerApellido(),
                request.getSegundoApellido(),
                request.getPrimerNombre(),
                request.getSegundoNombre(),
                request.getCorreo(),
                request.getPassword(),
                grupoId,
                usuarioEjecutor
        );
    }
}
