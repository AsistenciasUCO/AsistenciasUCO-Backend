package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

import co.edu.uco.asistenciasuco.application.features.admin.actualizarparametro.primaryports.ActualizarParametroInputPort.Resultado;

import java.util.UUID;

/** CULM-001 D.3 (CONTRACT v2 §4.3): {@code ParametroActualizadoDTO}. {@code fechaModificacion} es la nueva version a reenviar. */
public record ParametroActualizadoResponse(UUID id, String grupo, String clave, String valorAnterior, String valorNuevo,
                                           String fechaModificacion, boolean sinCambios) {

    public static ParametroActualizadoResponse from(final Resultado r) {
        return new ParametroActualizadoResponse(r.id(), r.grupo(), r.clave(), r.valorAnterior(), r.valorNuevo(),
                FechaIso.formatear(r.fechaModificacion()), r.sinCambios());
    }
}
