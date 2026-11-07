package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia.request;

import java.util.List;
import java.util.UUID;

/** CULM-001 B.2 (contrato 3.2.5): cuerpos HTTP de las tomas. */
public final class TomaAsistenciaRequests {

    private TomaAsistenciaRequests() {
    }

    /** {@code MANUAL} o {@code QR} (codigo del catalogo {@code ModoTomaAsistencia}). */
    public record AbrirToma(String modo) {
    }

    /** {@code null} aplica el valor del modo (MANUAL no marca, QR marca). */
    public record CerrarToma(Boolean marcarAusentes) {
    }

    public record RegistroToma(UUID estudianteId, String estado) {
    }

    public record LoteToma(List<RegistroToma> registros) {
    }
}
