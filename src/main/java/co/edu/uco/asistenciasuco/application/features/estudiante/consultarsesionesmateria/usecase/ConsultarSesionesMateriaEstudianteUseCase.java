package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import java.util.List; import java.util.UUID;

public interface ConsultarSesionesMateriaEstudianteUseCase {
    /** Primera pagina con el tamano por defecto (PAGINACION/TAMANO_MAXIMO). */
    default List<SesionMateriaEstudianteDomain> execute(UUID actorUsuarioId, UUID idAsignatura) {
        return execute(actorUsuarioId, idAsignatura, null, null);
    }

    /** {@code pagina} (1 por defecto) y {@code tamano} (PAGINACION/TAMANO_MAXIMO por defecto) son opcionales. */
    List<SesionMateriaEstudianteDomain> execute(UUID actorUsuarioId, UUID idAsignatura, Integer pagina, Integer tamano);
}
