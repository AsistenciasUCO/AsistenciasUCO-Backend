package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CULM-001 B.2 (HU175, HU176; DEC-015): QR/PIN rotativo de asistencia por toma. El token llega en claro como valor opaco:
 * el adaptador calcula su SHA-256 y la base de datos solo conoce el hash. Ni el token ni el PIN se registran en logs.
 */
public interface AsistenciaQrRepositoryPort {

    record QrEmitido(UUID idTokenAsistencia, UUID idToma, UUID idSesion, UUID idGrupo, Instant fechaRotacionUtc,
                     Instant fechaExpiracionUtc, int rotacionSegundos) {
    }

    record QrRotado(UUID idTokenAsistencia, UUID idToma, UUID idGrupo, UUID idDocenteEmisor, Instant fechaRotacionUtc,
                    Instant fechaExpiracionUtc) {
    }

    record TomaPorRotar(UUID idToma, UUID idGrupo, UUID idDocenteEmisor) {
    }

    record AsistenciaPorToken(UUID idToma, UUID idSesion, UUID idGrupo, String nombreAsignatura, String nombreGrupo,
                              int numeroSesion, int numeroToma, boolean yaRegistrado, Instant fechaRegistroUtc,
                              String mensaje) {
    }

    /**
     * La base de datos eligio esta transaccion como victima de un interbloqueo (SQL 1205). La operacion es idempotente:
     * el caso de uso puede reintentarla.
     */
    final class ConflictoConcurrenciaException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        public ConflictoConcurrenciaException(final String message) {
            super(message);
        }
    }

    QrEmitido abrir(UUID tomaId, String token, String pin, UUID usuarioEjecutor);

    QrRotado rotarManual(UUID tomaId, String token, String pin, UUID usuarioEjecutor);

    /** Rotacion del job: vacio si otra instancia ya roto, si no tocaba rotar o si el token ya se cerro o vencio. */
    Optional<QrRotado> rotarAutomatico(UUID tomaId, String token, String pin);

    List<TomaPorRotar> consultarTomasPorRotar();

    /** Cierra el token activo de la toma (la toma sigue abierta). Devuelve el mensaje de usuario del SP ({@code AQR_013}). */
    String cerrar(UUID tomaId, UUID usuarioEjecutor);

    /** Exactamente uno de {@code token} o {@code pin} viene informado. */
    AsistenciaPorToken registrarPorToken(String token, String pin, UUID usuarioEjecutor);
}
