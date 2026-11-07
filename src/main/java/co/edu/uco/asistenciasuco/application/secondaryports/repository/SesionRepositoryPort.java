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

    void cerrarSesion(CerrarSesionRepositoryDTO dto);

    /**
     * CULM-001 B.1: {@code usp_generar_sesiones_grupo_v2} (genera con el SP congelado y cancela por festivo las sesiones
     * nuevas que caen en un dia festivo activo).
     */
    GeneracionSesiones generarSesionesGrupo(GenerarSesionesGrupoRepositoryDTO dto);

    record GeneracionSesiones(int sesionesCanceladasPorFestivo, int sesionesFestivoConAsistencia) {
    }
}
