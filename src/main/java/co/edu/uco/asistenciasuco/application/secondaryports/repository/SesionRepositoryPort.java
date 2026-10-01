package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.GenerarSesionesGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;

import java.util.List;
import java.util.UUID;

/**
 * Puerto secundario para persistencia relacionada con sesiones.
 */
public interface SesionRepositoryPort {

    void crearSesion(CrearSesionRepositoryDTO dto);

    void actualizarSesion(ActualizarSesionRepositoryDTO dto);

    SesionRepositoryProjection consultarSesion(ConsultarSesionRepositoryDTO dto);

    List<SesionRepositoryProjection> consultarSesionesPorGrupo(UUID grupoId);

    default List<SesionRepositoryProjection> consultarSesionesPorGrupo(UUID grupoId, UUID usuarioEjecutor) {
        return consultarSesionesPorGrupo(grupoId);
    }

    void cerrarSesion(CerrarSesionRepositoryDTO dto);

    void generarSesionesGrupo(GenerarSesionesGrupoRepositoryDTO dto);
}
