package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.ConsultarSesionesMateriaEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.EstadoAsistenciaSesion;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.mapper.ConsultarSesionesMateriaEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaSesionEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SesionMateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaSesionEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Sesiones de una materia del estudiante autenticado, con su estado real de asistencia.
 * El estudiante se resuelve en el servidor desde el usuario autenticado. Una sesion sin cabecera o sin detalle de
 * asistencia es {@code NO_DISPONIBLE}: jamas se asume ausencia por falta de registro.
 */
public final class ConsultarSesionesMateriaEstudianteUseCaseImpl implements ConsultarSesionesMateriaEstudianteUseCase {
    private static final String RAZON_ASISTENCIA_NORMAL = "AN";
    private static final String RAZON_SIN_JUSTA_CAUSA = "SJC";
    private static final String RAZON_EXCUSA = "EX";
    private static final String SOLICITUD_ESTADO_DESCONOCIDO = "DESCONOCIDO";

    private final InstitutionalScopePort scopePort;
    private final SesionMateriaEstudianteQueryPort queryPort;
    private final AsistenciaSesionEstudianteQueryPort asistenciaPort;

    public ConsultarSesionesMateriaEstudianteUseCaseImpl(final InstitutionalScopePort scopePort,
                                                         final SesionMateriaEstudianteQueryPort queryPort,
                                                         final AsistenciaSesionEstudianteQueryPort asistenciaPort) {
        this.scopePort = Objects.requireNonNull(scopePort);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.asistenciaPort = Objects.requireNonNull(asistenciaPort);
    }

    @Override public List<SesionMateriaEstudianteDomain> execute(final UUID actorUsuarioId, final UUID idAsignatura) {
        final UUID idEstudiante = scopePort.findEstudianteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));
        final Map<UUID, List<AsistenciaSesionEstudianteProjection>> hechosPorSesion = new HashMap<>();
        for (final var hecho : asistenciaPort.consultarAsistenciaSesiones(idEstudiante, idAsignatura)) {
            hechosPorSesion.computeIfAbsent(hecho.idSesion(), k -> new ArrayList<>()).add(hecho);
        }
        return queryPort.consultarSesionesMateria(idEstudiante, idAsignatura).stream()
                .map(sesion -> {
                    final var hechos = hechosPorSesion.getOrDefault(sesion.id(), List.of());
                    final EstadoAsistenciaSesion estado = resolverEstado(hechos);
                    final String estadoSolicitud = resolverEstadoSolicitud(hechos);
                    final boolean reclamable = estado == EstadoAsistenciaSesion.AUSENTE && estadoSolicitud == null;
                    return ConsultarSesionesMateriaEstudianteRepositoryMapper.toDomain(
                            sesion, estado, estadoSolicitud, reclamable);
                })
                .toList();
    }

    /** Usa el detalle de mayor codigo (la ultima escritura); sin detalle o con pareja no definida, no hay estado. */
    static EstadoAsistenciaSesion resolverEstado(final List<AsistenciaSesionEstudianteProjection> hechos) {
        return hechos.stream()
                .filter(h -> h.asistio() != null)
                .max(Comparator.comparing(h -> h.codigoDetalle() == null ? Integer.MIN_VALUE : h.codigoDetalle()))
                .map(h -> clasificar(h.asistio(), h.codigoRazonCausa()))
                .orElse(EstadoAsistenciaSesion.NO_DISPONIBLE);
    }

    private static EstadoAsistenciaSesion clasificar(final boolean asistio, final String codigoRazonCausa) {
        final String razon = codigoRazonCausa == null ? "" : codigoRazonCausa.trim().toUpperCase(Locale.ROOT);
        if (asistio) {
            return RAZON_ASISTENCIA_NORMAL.equals(razon)
                    ? EstadoAsistenciaSesion.PRESENTE : EstadoAsistenciaSesion.NO_DISPONIBLE;
        }
        return switch (razon) {
            case RAZON_SIN_JUSTA_CAUSA -> EstadoAsistenciaSesion.AUSENTE;
            case RAZON_EXCUSA -> EstadoAsistenciaSesion.JUSTIFICADA;
            default -> EstadoAsistenciaSesion.NO_DISPONIBLE;
        };
    }

    /**
     * Estado de la solicitud mas reciente, o nulo si la asistencia no tiene ninguna. Cualquier solicitud existente
     * (pendiente, aceptada o rechazada) impide ofrecer un nuevo reclamo desde la UI.
     */
    private static String resolverEstadoSolicitud(final List<AsistenciaSesionEstudianteProjection> hechos) {
        return hechos.stream()
                .filter(h -> h.idSolicitud() != null)
                .max(Comparator.comparing(h -> h.fechaSolicitud() == null ? LocalDate.MIN : h.fechaSolicitud()))
                .map(h -> h.estadoSolicitud() == null || h.estadoSolicitud().isBlank()
                        ? SOLICITUD_ESTADO_DESCONOCIDO : h.estadoSolicitud())
                .orElse(null);
    }
}
