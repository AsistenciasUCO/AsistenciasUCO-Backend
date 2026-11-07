package co.edu.uco.asistenciasuco.application.features.tomaasistencia.common;

import java.util.UUID;

/**
 * CULM-001 B.2 ({@code uv_faltantes_toma}): estudiante activo sin registro presente en la toma. {@code marcadoAlCerrar}
 * indica que el cierre lo dejo como ausente ({@code SJC}).
 */
public record FaltanteTomaFila(
        UUID estudianteId,
        String nombreCompleto,
        boolean tieneRegistro,
        String estado,
        boolean marcadoAlCerrar
) {
}
