package co.edu.uco.asistenciasuco.application.features.festivos.recargarfestivos.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/**
 * CULM-001 B.1 (§3.2.2): el administrador recarga los festivos de un anio: obtener con respaldo, cargarlos y aplicarlos
 * a las sesiones futuras de los grupos de su institucion.
 */
public interface RecargarFestivosAnioInputPort
        extends InteractorWithReturn<RecargarFestivosAnioInputPort.Comando, RecargarFestivosAnioInputPort.Resultado> {

    record Comando(int anio, UUID usuarioEjecutor) {
    }

    record Resultado(int anio, String fuente, boolean usoRespaldo, int totalFestivos, int festivosInsertados,
                     int festivosActualizados, int festivosDesactivados, int diferenciasCalculoLocal,
                     Aplicacion aplicacion) {
    }

    record Aplicacion(int gruposProcesados, int sesionesCanceladas, int sesionesConAsistencia) {
    }
}
