package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.ConsultarSesionesMateriaEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.EstadoAsistenciaSesion;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.mapper.ConsultarSesionesMateriaEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaSesionEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SesionMateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaSesionEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.SesionProcedenciaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.common.mapper.SesionProcedenciaMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
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
    private final SesionProcedenciaQueryPort procedenciaPort;

    public ConsultarSesionesMateriaEstudianteUseCaseImpl(final InstitutionalScopePort scopePort,
                                                         final SesionMateriaEstudianteQueryPort queryPort,
                                                         final AsistenciaSesionEstudianteQueryPort asistenciaPort,
                                                         final SesionProcedenciaQueryPort procedenciaPort) {
        this.scopePort = Objects.requireNonNull(scopePort);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.asistenciaPort = Objects.requireNonNull(asistenciaPort);
        this.procedenciaPort = Objects.requireNonNull(procedenciaPort, "SesionProcedenciaQueryPort es obligatorio.");
    }

    @Override public List<SesionMateriaEstudianteDomain> execute(final UUID actorUsuarioId, final UUID idAsignatura) {
        final UUID idEstudiante = scopePort.findEstudianteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));
        final Map<UUID, List<AsistenciaSesionEstudianteProjection>> hechosPorSesion = new HashMap<>();
        for (final var hecho : asistenciaPort.consultarAsistenciaSesiones(idEstudiante, idAsignatura)) {
            hechosPorSesion.computeIfAbsent(hecho.idSesion(), k -> new ArrayList<>()).add(hecho);
        }
        final List<SesionMateriaEstudianteProjection> sesiones = queryPort.consultarSesionesMateria(idEstudiante, idAsignatura);
        final Map<UUID, SesionProcedenciaEntity> procedencias = procedencias(sesiones);
        return sesiones.stream()
                .map(sesion -> {
                    final var hechos = hechosPorSesion.getOrDefault(sesion.id(), List.of());
                    final SesionProcedenciaEntity procedencia = procedencias.get(sesion.id());
                    // CULM-001 B.1 (3.2.4): una sesion cancelada no tiene asistencia que mostrar ni es reclamable.
                    final boolean cancelada = procedencia != null && procedencia.cancelacion() != null;
                    final EstadoAsistenciaSesion estado = cancelada ? EstadoAsistenciaSesion.NO_DISPONIBLE : resolverEstado(hechos);
                    final String estadoSolicitud = resolverEstadoSolicitud(hechos);
                    final boolean reclamable = estado == EstadoAsistenciaSesion.AUSENTE && estadoSolicitud == null;
                    return ConsultarSesionesMateriaEstudianteRepositoryMapper.toDomain(
                            sesion, procedencia, estado, estadoSolicitud, reclamable);
                })
                .toList();
    }

    /**
     * APP-04: misma regla que HU060 coordinador. Las sesiones ya vienen aisladas por estudiante/asignatura; la
     * procedencia se consulta una vez por grupo y solo se aplica a filas del mismo grupo e id. Sin procedencia
     * CONFIRMADA no se ofrecen instantes UTC (estadoTemporal INDETERMINADA).
     */
    private Map<UUID, SesionProcedenciaEntity> procedencias(final List<SesionMateriaEstudianteProjection> sesiones) {
        final Map<UUID, SesionProcedenciaEntity> resultado = new HashMap<>();
        sesiones.stream().map(SesionMateriaEstudianteProjection::idGrupo).filter(Objects::nonNull).distinct()
                .forEach(idGrupo -> procedenciaPort.consultarSesionesPorGrupo(idGrupo).stream()
                        .map(SesionProcedenciaMapper::toEntity)
                        .filter(p -> idGrupo.equals(p.grupo()))
                        .forEach(p -> resultado.putIfAbsent(p.sesion(), p)));
        return resultado;
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
