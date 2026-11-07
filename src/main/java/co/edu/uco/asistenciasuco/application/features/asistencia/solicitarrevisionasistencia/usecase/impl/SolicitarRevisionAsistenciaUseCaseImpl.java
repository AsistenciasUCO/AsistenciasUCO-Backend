package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.SolicitarRevisionAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.domain.SolicitarRevisionAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.mapper.SolicitarRevisionAsistenciaRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DestinatariosReclamoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioReclamoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Implementacion del caso de uso solicitar revision de asistencia.
 */
public final class SolicitarRevisionAsistenciaUseCaseImpl implements SolicitarRevisionAsistenciaUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolicitarRevisionAsistenciaUseCaseImpl.class);

    private final AsistenciaRepositoryPort asistenciaRepositoryPort;
    private final InstitutionalScopePort institutionalScopePort;
    private final NotificacionPort notificacionPort;
    private final DestinatariosReclamoPort destinatariosPort;

    public SolicitarRevisionAsistenciaUseCaseImpl(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        this(asistenciaRepositoryPort, institutionalScopePort, notificacion -> {}, null);
    }

    public SolicitarRevisionAsistenciaUseCaseImpl(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final InstitutionalScopePort institutionalScopePort,
            final NotificacionPort notificacionPort,
            final DestinatariosReclamoPort destinatariosPort
    ) {
        this.asistenciaRepositoryPort = Objects.requireNonNull(asistenciaRepositoryPort, "El puerto de salida AsistenciaRepositoryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
        this.notificacionPort = notificacionPort != null ? notificacionPort : notificacion -> {};
        this.destinatariosPort = destinatariosPort;
    }

    @Override
    public void execute(final SolicitarRevisionAsistenciaDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para solicitar revision de asistencia es obligatorio.");
        }
        final var estudianteId = institutionalScopePort.findEstudianteIdByUsuario(domain.getUsuario())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));
        asistenciaRepositoryPort.solicitarRevisionAsistencia(
                SolicitarRevisionAsistenciaRepositoryMapper.toRepositoryDTO(domain, estudianteId)
        );
        avisar(domain, estudianteId);
    }

    private void avisar(final SolicitarRevisionAsistenciaDomain domain, final UUID estudianteId) {
        if (destinatariosPort == null) {
            return;
        }
        try {
            final List<DestinatarioReclamoProjection> destinatarios =
                    destinatariosPort.consultarDestinatariosPorSesion(domain.getSesion(), estudianteId);
            if (destinatarios == null || destinatarios.isEmpty()) {
                return;
            }
            final DestinatarioReclamoProjection contexto = destinatarios.getFirst();
            final String grupo = contexto.nombreGrupo() != null && !contexto.nombreGrupo().isBlank()
                    ? contexto.nombreGrupo() : "el grupo";
            final String fecha = contexto.fechaSesion() != null && !contexto.fechaSesion().isBlank()
                    ? contexto.fechaSesion() : "la sesión";
            final String estudiante = destinatarios.stream()
                    .filter(d -> "ESTUDIANTE".equalsIgnoreCase(d.rol()))
                    .map(DestinatarioReclamoProjection::nombreCompleto)
                    .findFirst()
                    .orElse("el estudiante");

            final List<Notificacion.Destinatario> dests = destinatarios.stream()
                    .filter(d -> "DOCENTE".equalsIgnoreCase(d.rol()))
                    .map(d -> new Notificacion.Destinatario(d.rol(), d.idUsuario(), d.nombreCompleto(), d.correo()))
                    .toList();

            if (!dests.isEmpty()) {
                final String asunto = "Nueva solicitud de revisión de asistencia en " + grupo;
                final String mensaje = "El estudiante " + estudiante + " radicó una solicitud de revisión de asistencia para la sesión del "
                        + fecha + " en el grupo " + grupo + ". Motivo: " + domain.getJustificacion();
                final Map<String, String> datos = new LinkedHashMap<>();
                datos.put("grupo", grupo);
                datos.put("estudiante", estudiante);
                datos.put("fecha", fecha);
                if (domain.getJustificacion() != null) {
                    datos.put("motivo", domain.getJustificacion());
                }
                notificacionPort.notificar(new Notificacion(
                        Notificacion.TIPO_RECLAMO_ASISTENCIA_RADICADO,
                        asunto,
                        mensaje,
                        dests,
                        datos
                ));
            }
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible enviar el aviso de reclamo de asistencia para la sesión {}: {}",
                    domain.getSesion(), exception.getClass().getSimpleName());
        }
    }
}
