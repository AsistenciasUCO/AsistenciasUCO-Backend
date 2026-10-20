package co.edu.uco.asistenciasuco.application.secondaryports.storage;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto secundario neutral de la metadata durable de un soporte de revision (LB-004D, ONE_TABLE).
 *
 * <p>SQL Server es la autoridad institucional del vinculo soporte - estudiante - solicitud; los bytes
 * viven en {@link FileStoragePort}. Application solo conoce el {@code fileId} opaco: nunca bucket,
 * objectKey, URL ni ruta fisica, de modo que cambiar de proveedor de archivos no altera esta
 * identidad ni las relaciones academicas.</p>
 */
public interface SoporteArchivoMetadataPort {

    /**
     * Registra el soporte en estado borrador (DRAFT). Idempotente por {@code fileId}: repetir el mismo
     * registro (mismo propietario y misma metadata) es un exito sin escritura.
     *
     * @throws SoporteMetadataRechazadaException  la DB rechazo el registro de forma definitiva (nada se escribio)
     * @throws SoporteMetadataNoDisponibleException resultado desconocido (transporte/timeout): puede haber o no fila
     */
    void registrarBorrador(RegistrarSoporteBorrador comando);

    /**
     * Consulta autorizada en SQL: solo el estudiante propietario o el docente titular de la sesion de la
     * solicitud (si el soporte esta ATTACHED) obtienen una fila. Cualquier otro actor, o un soporte
     * inexistente u huerfano, devuelve vacio sin distinguir casos.
     *
     * @throws SoporteMetadataNoDisponibleException fallo tecnico de la consulta
     */
    Optional<SoporteArchivoAutorizado> consultarAutorizado(UUID fileId, UUID usuarioEjecutor);

    record RegistrarSoporteBorrador(
            UUID fileId,
            UUID usuarioPropietario,
            String nombreOriginal,
            String tipoContenido,
            long tamanioBytes,
            String checksumSha256
    ) {
    }

    /** Soporte visible para el ejecutor; los instantes son UTC (DATETIME2(7) en SQL). */
    record SoporteArchivoAutorizado(
            UUID fileId,
            EstadoSoporte estado,
            String nombreOriginal,
            String tipoContenido,
            long tamanioBytes,
            String checksumSha256,
            Instant fechaRegistroUtc,
            Instant fechaAdjuncionUtc,
            UUID solicitudRevisionAsistencia,
            RelacionAcceso relacionAcceso
    ) {
    }

    enum EstadoSoporte { DRAFT, ATTACHED }

    enum RelacionAcceso { PROPIETARIO, DOCENTE_TITULAR }

    /** Rechazo definitivo: la DB respondio y no escribio nada. Es seguro compensar borrando los bytes. */
    class SoporteMetadataRechazadaException extends RuntimeException {
        public SoporteMetadataRechazadaException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    /** Resultado desconocido o DB no disponible: NO es seguro borrar los bytes sin conocer el resultado. */
    class SoporteMetadataNoDisponibleException extends RuntimeException {
        public SoporteMetadataNoDisponibleException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
