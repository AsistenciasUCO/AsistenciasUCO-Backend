package co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.ResolverSolicitudRevisionAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.domain.ResolverSolicitudRevisionAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.mapper.ResolverSolicitudRevisionAsistenciaRepositoryMapper;
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

public final class ResolverSolicitudRevisionAsistenciaUseCaseImpl implements ResolverSolicitudRevisionAsistenciaUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResolverSolicitudRevisionAsistenciaUseCaseImpl.class);

    private final AsistenciaRepositoryPort asistenciaRepositoryPort;
    private final InstitutionalScopePort institutionalScopePort;
    private final NotificacionPort notificacionPort;
    private final DestinatariosReclamoPort destinatariosPort;

    public ResolverSolicitudRevisionAsistenciaUseCaseImpl(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        this(asistenciaRepositoryPort, institutionalScopePort, notificacion -> {}, null);
    }

    public ResolverSolicitudRevisionAsistenciaUseCaseImpl(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final InstitutionalScopePort institutionalScopePort,
            final NotificacionPort notificacionPort,
            final DestinatariosReclamoPort destinatariosPort
    ) {
        this.asistenciaRepositoryPort = Objects.requireNonNull(asistenciaRepositoryPort, "AsistenciaRepositoryPort es obligatorio.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es obligatorio.");
        this.notificacionPort = notificacionPort != null ? notificacionPort : notificacion -> {};
        this.destinatariosPort = destinatariosPort;
    }

    @Override
    public void execute(final ResolverSolicitudRevisionAsistenciaDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para resolver solicitud de revision es obligatorio.");
        }
        final var docenteId = institutionalScopePort.findDocenteIdByUsuario(domain.getUsuario())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el docente autenticado."));
        asistenciaRepositoryPort.resolverSolicitudRevisionAsistencia(
                ResolverSolicitudRevisionAsistenciaRepositoryMapper.toRepositoryDTO(domain, docenteId)
        );
        avisar(domain);
    }

    private void avisar(final ResolverSolicitudRevisionAsistenciaDomain domain) {
        if (destinatariosPort == null) {
            return;
        }
        try {
            final List<DestinatarioReclamoProjection> destinatarios =
                    destinatariosPort.consultarDestinatariosPorSolicitud(domain.getSolicitud());
            if (destinatarios == null || destinatarios.isEmpty()) {
                return;
            }
            final DestinatarioReclamoProjection contexto = destinatarios.getFirst();
            final String grupo = contexto.nombreGrupo() != null && !contexto.nombreGrupo().isBlank()
                    ? contexto.nombreGrupo() : "el grupo";
            final String fecha = contexto.fechaSesion() != null && !contexto.fechaSesion().isBlank()
                    ? contexto.fechaSesion() : "la sesión";
            final String estado = domain.getAccion();
            final String observacion = domain.getRespuestaDocente() != null ? domain.getRespuestaDocente() : "";

            final List<Notificacion.Destinatario> dests = destinatarios.stream()
                    .filter(d -> "ESTUDIANTE".equalsIgnoreCase(d.rol()))
                    .map(d -> new Notificacion.Destinatario(d.rol(), d.idUsuario(), d.nombreCompleto(), d.correo()))
                    .toList();

            if (!dests.isEmpty()) {
                final String asunto = "Respuesta a tu solicitud de revisión de asistencia en " + grupo;
                final String mensaje = "Tu solicitud de revisión de asistencia para la sesión del " + fecha
                        + " en el grupo " + grupo + " fue resuelta como " + estado + ". Observaciones del docente: " + observacion;
                final Map<String, String> datos = new LinkedHashMap<>();
                datos.put("grupo", grupo);
                datos.put("fecha", fecha);
                datos.put("estado", estado);
                if (!observacion.isBlank()) {
                    datos.put("observacion", observacion);
                }
                notificacionPort.notificar(new Notificacion(
                        Notificacion.TIPO_RECLAMO_ASISTENCIA_RESUELTO,
                        asunto,
                        mensaje,
                        dests,
                        datos
                ));
            }
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible enviar el aviso de resolucion de reclamo {}: {}",
                    domain.getSolicitud(), exception.getClass().getSimpleName());
        }
    }
}
