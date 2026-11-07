package co.edu.uco.asistenciasuco.application.features.tomaasistencia.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * CULM-001 B.2: tomas de una sesion con su ventana de registro ({@code [inicio - TOLERANCIA, fin + MARGEN)}), calculada
 * por la base de datos ({@code uv_sesion_estado}); el cliente la pinta y no la recalcula.
 */
public record TomasSesionResumen(
        UUID idSesion,
        UUID idGrupo,
        BigDecimal horasSesion,
        boolean estaCancelada,
        Instant ventanaAperturaUtc,
        Instant ventanaCierreUtc,
        boolean ventanaAbierta,
        int maximoTomas,
        List<TomaAsistenciaResumen> tomas
) {

    public TomasSesionResumen {
        tomas = tomas == null ? List.of() : List.copyOf(tomas);
    }
}
