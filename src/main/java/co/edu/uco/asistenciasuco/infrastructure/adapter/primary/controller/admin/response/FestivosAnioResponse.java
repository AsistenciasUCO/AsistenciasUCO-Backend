package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

import co.edu.uco.asistenciasuco.application.features.festivos.consultarfestivos.primaryports.ConsultarFestivosAnioInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpUtcInstantCodec;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** CULM-001 B.1 (§3.2.2): festivos de un anio con su ultima carga y la atribucion de la fuente. */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record FestivosAnioResponse(
        int anio,
        String fuenteUltimaCarga,
        String fechaUltimaCargaUtc,
        Integer diferenciasCalculoLocal,
        Atribucion atribucion,
        List<Festivo> festivos,
        long totalFestivos
) {

    public record Atribucion(String texto, String url) {
    }

    /** {@code fecha} es una fecha civil {@code YYYY-MM-DD} (sin hora ni zona). */
    public record Festivo(String fecha, String nombre, String fuente, boolean estaActivo) {
    }

    public static FestivosAnioResponse from(final ConsultarFestivosAnioInputPort.Resultado resultado) {
        return new FestivosAnioResponse(
                resultado.anio(),
                resultado.fuenteUltimaCarga(),
                resultado.fechaUltimaCargaUtc() == null
                        ? null : HttpUtcInstantCodec.fromUtcDatabaseDateTime(resultado.fechaUltimaCargaUtc()),
                resultado.diferenciasCalculoLocal(),
                new Atribucion(resultado.atribucionTexto(), resultado.atribucionUrl()),
                resultado.festivos().stream()
                        .map(f -> new Festivo(f.fecha() == null ? null : f.fecha().toString(), f.nombre(), f.fuente(),
                                f.estaActivo()))
                        .toList(),
                resultado.totalFestivos());
    }
}
