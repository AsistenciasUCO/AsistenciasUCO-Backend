package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente.response;

import co.edu.uco.asistenciasuco.application.features.sesioncancelacion.cancelarsesion.primaryports.CancelarSesionInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.CancelacionSesionResponse;

import java.util.UUID;

/** CULM-001 B.1 (§3.2.1, HU047): respuesta de {@code PATCH /api/v1/docente/sesiones/{sesionId}/cancelar}. */
public record SesionCanceladaResponse(
        UUID idSesion,
        UUID idGrupo,
        String estado,
        CancelacionSesionResponse cancelacion,
        int estudiantesNotificados
) {

    public static SesionCanceladaResponse from(final CancelarSesionInputPort.Resultado resultado) {
        return new SesionCanceladaResponse(resultado.idSesion(), resultado.idGrupo(), resultado.estado().name(),
                CancelacionSesionResponse.from(resultado.cancelacion()), resultado.estudiantesNotificados());
    }
}
