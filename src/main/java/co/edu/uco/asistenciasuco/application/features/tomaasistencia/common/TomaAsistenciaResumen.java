package co.edu.uco.asistenciasuco.application.features.tomaasistencia.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * CULM-001 B.2 (DEC-013): una toma de asistencia de una sesion tal como la expone {@code uv_toma_asistencia}. Los codigos
 * de modo y estado son los del catalogo ({@code ModoTomaAsistencia}, {@code EstadoTomaAsistencia}).
 */
public record TomaAsistenciaResumen(
        UUID idToma,
        UUID idSesion,
        UUID idGrupo,
        int numero,
        String modo,
        String estado,
        boolean estaAbierta,
        boolean esImplicita,
        Instant fechaAperturaUtc,
        Instant fechaCierreUtc,
        BigDecimal horasToma,
        int totalRegistros,
        int totalPresentes,
        int totalAusentes,
        int totalExcusas,
        boolean tieneQrActivo
) {
}
