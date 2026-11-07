package co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotarqrtoma.primaryports;

import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.QrTomaResumen;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/** CULM-001 B.2 (HU175, contrato 3.2.6): el docente titular regenera a demanda el QR vigente de la toma. */
public interface RotarQrTomaInputPort extends InteractorWithReturn<RotarQrTomaInputPort.Comando, QrTomaResumen> {

    record Comando(UUID tomaId, UUID usuarioEjecutor) {
    }
}
