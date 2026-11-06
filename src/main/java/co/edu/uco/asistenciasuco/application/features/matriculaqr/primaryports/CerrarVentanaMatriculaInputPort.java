package co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/** HU176B (CA-02): el docente titular cierra la ventana activa de su grupo (idempotente). */
public interface CerrarVentanaMatriculaInputPort
        extends InteractorWithReturn<CerrarVentanaMatriculaInputPort.Comando, Void> {

    record Comando(UUID grupoId, UUID usuarioEjecutor) {
    }
}
