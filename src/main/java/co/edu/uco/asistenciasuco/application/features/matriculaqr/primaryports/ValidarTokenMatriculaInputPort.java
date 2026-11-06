package co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.time.Instant;
import java.util.UUID;

/** HU176B (CA-03/CA-05): validacion PUBLICA de un token o PIN; solo devuelve los datos minimos del curso. */
public interface ValidarTokenMatriculaInputPort
        extends InteractorWithReturn<ValidarTokenMatriculaInputPort.Comando, ValidarTokenMatriculaInputPort.Resultado> {

    /** Exactamente uno de {@code token} o {@code pin}. {@code direccionIp} ya viene resuelta por la infraestructura. */
    record Comando(String token, String pin, String direccionIp) {
    }

    record Resultado(
            UUID idGrupo,
            String nombreAsignatura,
            String nombreGrupo,
            String nombreDocente,
            Instant fechaExpiracionUtc,
            int cupoDisponible
    ) {
    }
}
