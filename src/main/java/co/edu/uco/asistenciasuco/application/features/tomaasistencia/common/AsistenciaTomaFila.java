package co.edu.uco.asistenciasuco.application.features.tomaasistencia.common;

import java.time.Instant;
import java.util.UUID;

/** CULM-001 B.2: matricula activa del grupo con su detalle en la toma ({@code estado} nulo si aun no tiene registro). */
public record AsistenciaTomaFila(
        UUID estudianteId,
        String nombreCompleto,
        String estado,
        boolean registradoPorEstudiante,
        Instant fechaRegistroUtc
) {
}
