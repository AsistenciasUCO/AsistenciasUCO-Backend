package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.util.UUID;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;

import java.util.List;

/**
 * Puerto secundario para persistencia relacionada con asistencias.
 */
public interface AsistenciaRepositoryPort {

    void registrarAsistencia(RegistrarAsistenciaRepositoryDTO dto);

    void registrarAsistenciasSesion(RegistrarAsistenciasSesionRepositoryDTO dto);


    List<AsistenciaRepositoryProjection> consultarAsistenciasPorGrupo(ConsultarAsistenciasPorGrupoRepositoryDTO dto);

    void solicitarRevisionAsistencia(SolicitarRevisionAsistenciaRepositoryDTO dto);

    void resolverSolicitudRevisionAsistencia(ResolverSolicitudRevisionAsistenciaRepositoryDTO dto);

    /**
     * CULM-001 A.2-7 (HU010/HU012): el estudiante retira su reclamo pendiente. El SP valida propiedad (REV_006, 404) y
     * estado pendiente (REV_007, 409). Devuelve el mensaje institucional de exito (REV_008).
     */
    String retirarSolicitudRevision(UUID idSolicitudRevision, UUID usuarioEjecutor);
}
