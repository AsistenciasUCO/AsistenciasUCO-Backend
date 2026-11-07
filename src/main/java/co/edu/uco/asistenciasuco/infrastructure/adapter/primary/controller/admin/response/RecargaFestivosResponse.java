package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

import co.edu.uco.asistenciasuco.application.features.festivos.recargarfestivos.primaryports.RecargarFestivosAnioInputPort;

/** CULM-001 B.1 (§3.2.2): resultado de la recarga de festivos de un anio y de su aplicacion a las sesiones. */
public record RecargaFestivosResponse(
        int anio,
        String fuente,
        boolean usoRespaldo,
        int totalFestivos,
        int festivosInsertados,
        int festivosActualizados,
        int festivosDesactivados,
        int diferenciasCalculoLocal,
        Aplicacion aplicacion
) {

    public record Aplicacion(int gruposProcesados, int sesionesCanceladas, int sesionesConAsistencia) {
    }

    public static RecargaFestivosResponse from(final RecargarFestivosAnioInputPort.Resultado r) {
        return new RecargaFestivosResponse(r.anio(), r.fuente(), r.usoRespaldo(), r.totalFestivos(), r.festivosInsertados(),
                r.festivosActualizados(), r.festivosDesactivados(), r.diferenciasCalculoLocal(),
                new Aplicacion(r.aplicacion().gruposProcesados(), r.aplicacion().sesionesCanceladas(),
                        r.aplicacion().sesionesConAsistencia()));
    }
}
