package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Puerto secundario de la matricula por QR/PIN rotativo (HU176B). El token llega en claro como valor opaco: el adaptador
 * calcula su SHA-256 y la base de datos solo conoce el hash. Los rechazos de negocio (MQR_*) se propagan como
 * excepciones de aplicacion clasificadas por el adaptador.
 */
public interface MatriculaQrRepositoryPort {

    /** Exactamente uno de los dos valores debe venir informado. */
    record CredencialMatricula(String token, String pin) {
    }

    record VentanaAbierta(UUID idVentana, Instant fechaExpiracionUtc) {
    }

    record VentanaRotada(UUID idVentana, UUID idDocenteEmisor, Instant fechaRotacionUtc, Instant fechaExpiracionUtc) {
    }

    record VentanaPorRotar(UUID idGrupo, UUID idDocenteEmisor) {
    }

    record CursoMatricula(
            UUID idGrupo,
            String nombreAsignatura,
            String nombreGrupo,
            String nombreDocente,
            Instant fechaExpiracionUtc,
            int cupoDisponible
    ) {
    }

    record MatriculaRealizada(UUID idEstudiante, UUID idUsuario, UUID idGrupo) {
    }

    record RegistroPublico(
            CredencialMatricula credencial,
            UUID tipoIdentificacionId,
            int numeroIdentificacion,
            String primerNombre,
            String segundoNombre,
            String primerApellido,
            String segundoApellido,
            String correo,
            String passwordCodificada
    ) {
    }

    VentanaAbierta abrirVentana(UUID grupoId, String token, String pin, UUID usuarioEjecutor);

    void cerrarVentana(UUID grupoId, UUID usuarioEjecutor);

    VentanaRotada rotarManual(UUID grupoId, String token, String pin, UUID usuarioEjecutor);

    /** Rotacion del job: sin ejecutor; rota solo si la ventana ya vencio su periodo de rotacion. */
    VentanaRotada rotarAutomatico(UUID grupoId, String token, String pin);

    List<VentanaPorRotar> consultarVentanasPorRotar();

    CursoMatricula validar(CredencialMatricula credencial);

    MatriculaRealizada registrarPublico(RegistroPublico datos);

    MatriculaRealizada matricularAutenticado(CredencialMatricula credencial, UUID usuarioEjecutor);
}
