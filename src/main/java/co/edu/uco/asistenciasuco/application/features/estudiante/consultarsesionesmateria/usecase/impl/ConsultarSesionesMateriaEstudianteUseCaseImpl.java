package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.ConsultarSesionesMateriaEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.EstadoAsistenciaSesion;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.mapper.ConsultarSesionesMateriaEstudianteRepositoryMapper;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaSesionEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SesionMateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaSesionEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.SesionProcedenciaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.common.mapper.SesionProcedenciaMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * CULM-001 D.1 (HU026B): las sesiones salen de {@code usp_consultar_sesiones_materia_estudiante}, que valida el ambito
 * (matricula inscrita del usuario), pagina y trae la fecha limite de justificacion y si el plazo sigue vigente; este caso de uso
 * solo los transporta. Una materia tiene menos sesiones que PAGINACION/TAMANO_MAXIMO; si el total lo supera se registra un
 * aviso y se devuelve la primera pagina (deuda a revisar en CLOSURE).
 */
public final class ConsultarSesionesMateriaEstudianteUseCaseImpl implements ConsultarSesionesMateriaEstudianteUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsultarSesionesMateriaEstudianteUseCaseImpl.class);
    static final String GRUPO_PAGINACION = "PAGINACION";
    static final String CLAVE_TAMANO_MAXIMO = "TAMANO_MAXIMO";
    /** Valor de la semilla PAGINACION/TAMANO_MAXIMO; solo se usa si el catalogo no entrega un entero positivo. */
    static final int TAMANO_MAXIMO_SEMILLA = 100;
    private static final String RAZON_ASISTENCIA_NORMAL = "AN";
    private static final String RAZON_SIN_JUSTA_CAUSA = "SJC";
    private static final String RAZON_EXCUSA = "EX";
    private static final String SOLICITUD_ESTADO_DESCONOCIDO = "DESCONOCIDO";

    private final InstitutionalScopePort scopePort;
    private final SesionMateriaEstudianteQueryPort queryPort;
    private final AsistenciaSesionEstudianteQueryPort asistenciaPort;
    private final SesionProcedenciaQueryPort procedenciaPort;
    private final ParameterCatalogPort parametros;

    public ConsultarSesionesMateriaEstudianteUseCaseImpl(final InstitutionalScopePort scopePort,
                                                         final SesionMateriaEstudianteQueryPort queryPort,
                                                         final AsistenciaSesionEstudianteQueryPort asistenciaPort,
                                                         final SesionProcedenciaQueryPort procedenciaPort,
                                                         final ParameterCatalogPort parametros) {
        this.scopePort = Objects.requireNonNull(scopePort);
        this.queryPort = Objects.requireNonNull(queryPort);
        this.asistenciaPort = Objects.requireNonNull(asistenciaPort);
        this.procedenciaPort = Objects.requireNonNull(procedenciaPort, "SesionProcedenciaQueryPort es obligatorio.");
        this.parametros = Objects.requireNonNull(parametros, "ParameterCatalogPort es obligatorio.");
    }

    @Override public List<SesionMateriaEstudianteDomain> execute(final UUID actorUsuarioId, final UUID idAsignatura,
                                                                 final Integer pagina, final Integer tamano) {
        final UUID idEstudiante = scopePort.findEstudianteIdByUsuario(actorUsuarioId)
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el estudiante autenticado."));
        final Map<UUID, List<AsistenciaSesionEstudianteProjection>> hechosPorSesion = new HashMap<>();
        for (final var hecho : asistenciaPort.consultarAsistenciaSesiones(idEstudiante, idAsignatura)) {
            hechosPorSesion.computeIfAbsent(hecho.idSesion(), k -> new ArrayList<>()).add(hecho);
        }
        final int numeroPagina = pagina == null ? 1 : pagina;
        final int tamanoPagina = tamano == null ? tamanoPorDefecto() : tamano;
        final PaginaResultado<SesionMateriaEstudianteProjection> resultado =
                queryPort.consultarSesionesMateria(actorUsuarioId, idAsignatura, numeroPagina, tamanoPagina);
        if (resultado.totalElementos() > (long) resultado.elementos().size() + (long) (numeroPagina - 1) * tamanoPagina
                && pagina == null && tamano == null) {
            LOGGER.warn("La materia {} del estudiante tiene {} sesiones y se devuelve solo la primera pagina de {}.",
                    idAsignatura, resultado.totalElementos(), tamanoPagina);
        }
        final List<SesionMateriaEstudianteProjection> sesiones = resultado.elementos();
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

    /** PAGINACION/TAMANO_MAXIMO del catalogo; ausente, vacio o mal formado vale 100 (semilla). */
    private int tamanoPorDefecto() {
        try {
            final int valor = parametros.getParameter(GRUPO_PAGINACION, CLAVE_TAMANO_MAXIMO)
                    .map(String::strip).filter(texto -> !texto.isEmpty()).map(Integer::parseInt).orElse(TAMANO_MAXIMO_SEMILLA);
            return valor < 1 ? TAMANO_MAXIMO_SEMILLA : valor;
        } catch (final NumberFormatException | ParameterCatalogPort.ParameterCatalogException exception) {
            return TAMANO_MAXIMO_SEMILLA;
        }
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
