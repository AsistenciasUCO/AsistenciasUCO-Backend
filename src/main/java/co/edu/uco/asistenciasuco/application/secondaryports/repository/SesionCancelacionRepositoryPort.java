package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CULM-001 B.1 (HU047): cancelacion de una sesion por su docente titular y destinatarios del aviso. La DB valida RBAC
 * DOCENTE, titularidad, longitud del motivo y que la sesion no tenga asistencia ({@code SES_011..SES_016}).
 */
public interface SesionCancelacionRepositoryPort {

    /** {@code usp_cancelar_sesion}. */
    SesionCancelada cancelar(UUID sesionId, String motivo, UUID usuarioEjecutor);

    /** {@code usp_consultar_destinatarios_sesion_cancelada}: estudiantes con matricula activa del grupo. */
    List<DestinatarioSesionCancelada> consultarDestinatarios(UUID sesionId, UUID usuarioEjecutor);

    /**
     * @param fechaCancelacionUtc DATETIME2 en UTC sin zona
     */
    record SesionCancelada(UUID idSesion, UUID idGrupo, LocalDateTime fechaCancelacionUtc, int totalEstudiantesActivos) {
    }

    /**
     * @param fechaHoraInicio DATETIME2 de la sesion tal como lo guarda la DB (UTC si su procedencia esta confirmada)
     */
    record DestinatarioSesionCancelada(UUID idEstudiante, UUID idUsuario, String nombreCompleto, String correo,
                                       String nombreAsignatura, String nombreGrupo, String nombreDocente,
                                       LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, String motivoCancelacion) {
    }
}
