package co.edu.uco.asistenciasuco.application.features.asistenciaqr.cerrarqrtoma.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/**
 * CULM-001 B.2 (contrato 3.2.6): el docente titular deja de proyectar el QR (cierra el token activo; la toma sigue
 * abierta). Idempotente. Devuelve el mensaje de usuario ({@code AQR_013}).
 */
public interface CerrarQrTomaInputPort extends InteractorWithReturn<CerrarQrTomaInputPort.Comando, String> {

    record Comando(UUID tomaId, UUID usuarioEjecutor) {
    }
}
