package co.edu.uco.asistenciasuco.application.features.asistenciaqr.registrarasistenciaportoken.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.time.Instant;
import java.util.UUID;

/**
 * CULM-001 B.2 (HU176, contrato 3.2.7): el estudiante autenticado registra su asistencia con el token del QR o con el
 * PIN (exactamente uno) desde la red del campus. Idempotente: un registro previo responde {@code yaRegistrado = true} sin
 * cambiar la marca existente.
 */
public interface RegistrarAsistenciaPorTokenInputPort
        extends InteractorWithReturn<RegistrarAsistenciaPorTokenInputPort.Comando, RegistrarAsistenciaPorTokenInputPort.Resultado> {

    record Comando(String token, String pin, String direccionIp, UUID usuarioEjecutor) {
    }

    record Resultado(UUID idToma, UUID idSesion, String nombreAsignatura, String nombreGrupo, int numeroSesion,
                     int numeroToma, boolean yaRegistrado, Instant fechaRegistroUtc, String mensaje) {
    }
}
