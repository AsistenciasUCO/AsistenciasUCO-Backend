package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto secundario de lectura v2 de sesiones (UTC-D06/D07): cada fila trae sus horas y su
 * procedencia temporal leidas juntas, sin decidir todavia si son instantes confirmados.
 */
public interface SesionProcedenciaQueryPort {

    Optional<SesionProcedenciaRepositoryProjection> consultarSesion(UUID sesionId);

    List<SesionProcedenciaRepositoryProjection> consultarSesionesPorGrupo(UUID grupoId);
}
