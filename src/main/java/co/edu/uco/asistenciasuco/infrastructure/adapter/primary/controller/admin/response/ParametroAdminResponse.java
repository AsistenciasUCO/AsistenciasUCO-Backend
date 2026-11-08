package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.ConsultarParametrosInputPort.Parametro;

import java.util.List;
import java.util.UUID;

/** CULM-001 D.3 (CONTRACT v2 §4.3): {@code ParametroAdminDTO}. Fechas en UTC con milisegundos. */
public record ParametroAdminResponse(UUID id, String grupo, String clave, String valor, String valorDefecto, String tipoDato,
                                     boolean esEditable, Number valorMinimo, Number valorMaximo, List<String> valoresPermitidos,
                                     String etiqueta, String descripcion, String unidad, String categoriaVisual, int orden,
                                     String fechaModificacion, String fechaUltimoCambioUtc, String nombreUltimoActor) {

    public static ParametroAdminResponse from(final Parametro p) {
        return new ParametroAdminResponse(p.id(), p.grupo(), p.clave(), p.valor(), p.valorDefecto(), p.tipoDato(), p.esEditable(),
                FechaIso.numero(p.valorMinimo()), FechaIso.numero(p.valorMaximo()), p.valoresPermitidos(), p.etiqueta(),
                p.descripcion(), p.unidad(), p.categoriaVisual(), p.orden(), FechaIso.formatear(p.fechaModificacion()),
                FechaIso.formatear(p.fechaUltimoCambioUtc()), p.nombreUltimoActor());
    }
}
