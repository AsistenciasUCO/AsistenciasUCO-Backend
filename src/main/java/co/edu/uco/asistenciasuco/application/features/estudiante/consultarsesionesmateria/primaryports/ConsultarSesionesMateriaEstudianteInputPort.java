package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto.SesionMateriaEstudianteDTO;
import java.util.List; import java.util.UUID;

public interface ConsultarSesionesMateriaEstudianteInputPort {
    default List<SesionMateriaEstudianteDTO> execute(UUID actorUsuarioId, UUID idAsignatura) {
        return execute(actorUsuarioId, idAsignatura, null, null);
    }

    /** {@code pagina} (1 por defecto) y {@code tamano} (PAGINACION/TAMANO_MAXIMO por defecto) son opcionales. */
    List<SesionMateriaEstudianteDTO> execute(UUID actorUsuarioId, UUID idAsignatura, Integer pagina, Integer tamano);
}
