package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.mapper;

import java.util.UUID;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.ConsultarEstudiantesDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.estudiante.request.ConsultarEstudiantesRequest;

import java.util.Objects;

public final class EstudianteHttpMapper {

    private EstudianteHttpMapper() {
    }

    public static ConsultarEstudiantesDTO toApplicationDTO(final ConsultarEstudiantesRequest request, final UUID actorUsuarioId) {
        Objects.requireNonNull(request, "El request HTTP para consultar estudiantes es obligatorio.");
        return new ConsultarEstudiantesDTO(
                request.getTipoIdentificacionId(),
                request.getNumeroIdentificacion(),
                request.getNombre(),
                request.getCorreo(),
                request.getInstitucionId(),
                request.getFacultadId(),
                request.getProgramaId(),
                request.getGrupoId(),
                request.getActivo(),
                request.getPage(),
                request.getSize(),
                actorUsuarioId
        );
    }
}
