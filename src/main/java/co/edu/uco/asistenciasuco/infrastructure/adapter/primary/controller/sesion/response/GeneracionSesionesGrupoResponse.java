package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.response;

/**
 * Respuesta de {@code POST /api/v1/sesiones/grupo/{grupoId}/generacion}: conserva {@code exitoso} y {@code mensaje} de la
 * respuesta anterior y agrega (CULM-001 B.1, §3.2.3) los conteos de la cancelacion por festivo.
 */
public record GeneracionSesionesGrupoResponse(
        boolean exitoso,
        String mensaje,
        int sesionesCanceladasPorFestivo,
        int sesionesFestivoConAsistencia
) {
}
