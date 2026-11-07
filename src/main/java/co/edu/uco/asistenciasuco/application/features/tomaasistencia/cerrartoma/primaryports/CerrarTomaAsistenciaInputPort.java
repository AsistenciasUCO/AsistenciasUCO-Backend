package co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartoma.primaryports;

import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/**
 * CULM-001 B.2 ([SUPUESTO-B-08]): el docente titular cierra una toma. {@code marcarAusentes} nulo aplica el valor del modo
 * (MANUAL no marca, QR marca); con la ventana abierta, los activos sin registro quedan como ausentes ({@code SJC}).
 */
public interface CerrarTomaAsistenciaInputPort
        extends InteractorWithReturn<CerrarTomaAsistenciaInputPort.Comando, CerrarTomaAsistenciaInputPort.Resultado> {

    record Comando(UUID tomaId, Boolean marcarAusentes, UUID usuarioEjecutor) {
    }

    record Resultado(TomaAsistenciaResumen toma, int totalAusentesMarcados, int totalFaltantes, boolean yaEstabaCerrada) {
    }
}
