package co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.time.Instant;
import java.util.UUID;

/** HU176B (CA-01): el docente titular abre la ventana de matricula de su grupo y recibe el QR, el PIN y la URL. */
public interface AbrirVentanaMatriculaInputPort
        extends InteractorWithReturn<AbrirVentanaMatriculaInputPort.Comando, AbrirVentanaMatriculaInputPort.Resultado> {

    record Comando(UUID grupoId, UUID usuarioEjecutor) {
    }

    /** El token en claro solo existe dentro de {@code urlMatricula} y del QR; no se vuelve a consultar. */
    record Resultado(
            UUID idVentana,
            String pin,
            String urlMatricula,
            String qrPngBase64,
            Instant fechaRotacionUtc,
            Instant fechaExpiracionUtc
    ) {
    }
}
