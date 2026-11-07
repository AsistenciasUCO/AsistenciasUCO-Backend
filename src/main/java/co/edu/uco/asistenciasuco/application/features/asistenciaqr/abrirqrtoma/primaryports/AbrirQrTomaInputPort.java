package co.edu.uco.asistenciasuco.application.features.asistenciaqr.abrirqrtoma.primaryports;

import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.QrTomaResumen;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/** CULM-001 B.2 (HU175, contrato 3.2.6): el docente titular abre (o re-proyecta) el QR de una toma QR abierta. */
public interface AbrirQrTomaInputPort extends InteractorWithReturn<AbrirQrTomaInputPort.Comando, QrTomaResumen> {

    record Comando(UUID tomaId, UUID usuarioEjecutor) {
    }
}
