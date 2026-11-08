package co.edu.uco.asistenciasuco.application.features.admin.actualizarparametro.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.time.Instant;
import java.util.UUID;

/**
 * CULM-001 D.3 (HU168): el administrador edita un parametro de la lista blanca con motivo y concurrencia optimista
 * ({@code fechaModificacionEsperada}: la version que leyo, UTC con milisegundos).
 */
public interface ActualizarParametroInputPort
        extends InteractorWithReturn<ActualizarParametroInputPort.Comando, ActualizarParametroInputPort.Resultado> {

    record Comando(UUID idParametro, String valor, Instant fechaModificacionEsperada, String motivo, UUID usuarioEjecutor) {
    }

    /** {@code sinCambios}: el valor propuesto era igual al almacenado ({@code PRM_011}); no se escribio nada. */
    record Resultado(UUID id, String grupo, String clave, String valorAnterior, String valorNuevo, Instant fechaModificacion,
                     boolean sinCambios) {
    }
}
