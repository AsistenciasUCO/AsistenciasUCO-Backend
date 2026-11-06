package co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/** HU176B (D2): el docente titular regenera de inmediato el codigo (boton "Generar nuevo codigo" y reconexion SSE). */
public interface RotarVentanaMatriculaInputPort
        extends InteractorWithReturn<RotarVentanaMatriculaInputPort.Comando, AbrirVentanaMatriculaInputPort.Resultado> {

    record Comando(UUID grupoId, UUID usuarioEjecutor) {
    }
}
