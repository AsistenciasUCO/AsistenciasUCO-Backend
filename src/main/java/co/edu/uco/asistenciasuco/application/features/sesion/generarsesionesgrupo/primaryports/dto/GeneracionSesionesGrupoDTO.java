package co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.dto;

/**
 * CULM-001 B.1 (§3.2.3): conteos de la generacion v2. Las sesiones que caen en un festivo nacen canceladas; las que ya
 * tenian asistencia se conservan y se reportan.
 */
public record GeneracionSesionesGrupoDTO(int sesionesCanceladasPorFestivo, int sesionesFestivoConAsistencia) {
}
