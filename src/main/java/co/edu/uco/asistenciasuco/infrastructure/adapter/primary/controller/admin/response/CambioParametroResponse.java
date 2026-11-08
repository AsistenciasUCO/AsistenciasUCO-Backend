package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcambiosparametro.primaryports.ConsultarCambiosParametroInputPort.Cambio;

import java.util.UUID;

/** CULM-001 D.3 (CONTRACT v2 §4.3, diferible): {@code CambioParametroDTO}. */
public record CambioParametroResponse(UUID id, String valorAnterior, String valorNuevo, String motivo, UUID idUsuarioEjecutor,
                                      String nombreEjecutor, UUID idCorrelacion, String fechaCambioUtc) {

    public static CambioParametroResponse from(final Cambio c) {
        return new CambioParametroResponse(c.id(), c.valorAnterior(), c.valorNuevo(), c.motivo(), c.idUsuarioEjecutor(),
                c.nombreEjecutor(), c.idTraza(), FechaIso.formatear(c.fechaCambioUtc()));
    }
}
