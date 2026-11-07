package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * CULM-001 B.1 (§3.2.4): wire de la cancelacion de una sesion en todas las respuestas de sesion. {@code null} en una
 * sesion programada. {@code fechaCancelacionUtc} es un instante UTC ISO-8601 con {@code Z}.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record CancelacionSesionResponse(String tipo, String motivo, String nombreFestivo, String fechaCancelacionUtc) {

    public static CancelacionSesionResponse from(final CancelacionSesionDTO cancelacion) {
        if (cancelacion == null) {
            return null;
        }
        return new CancelacionSesionResponse(cancelacion.tipo(), cancelacion.motivo(), cancelacion.nombreFestivo(),
                cancelacion.fechaCancelacionUtc() == null ? null : cancelacion.fechaCancelacionUtc().toString());
    }
}
