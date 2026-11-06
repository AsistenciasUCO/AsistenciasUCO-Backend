package co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/** HU176B (CA-04): un estudiante con sesion iniciada se matricula con un token o PIN vigente. */
public interface MatricularEstudianteAutenticadoInputPort
        extends InteractorWithReturn<MatricularEstudianteAutenticadoInputPort.Comando, MatricularEstudianteAutenticadoInputPort.Resultado> {

    record Comando(String token, String pin, String direccionIp, UUID usuarioEjecutor) {
    }

    record Resultado(UUID idGrupo) {
    }
}
