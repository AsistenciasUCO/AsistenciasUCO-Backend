package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.ConsultarAsistenciaProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.domain.SesionGrupoHorarioDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.SesionProcedenciaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.common.mapper.SesionProcedenciaMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.AsistenciaSesionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.GrupoProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.SesionGrupoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * HU064/HU060/HU061. Exige un coordinador con programa (403 si no lo tiene) y valida paginacion y filtros; el
 * aislamiento por programa lo garantiza el provider DB, que filtra por el usuario ejecutor.
 */
public final class ConsultarAsistenciaProgramaUseCaseImpl implements ConsultarAsistenciaProgramaUseCase {

    static final int PAGINA_POR_DEFECTO = 1;
    static final int TAMANO_POR_DEFECTO = 10;
    static final int TAMANO_MAXIMO = 100;

    private final AsistenciaProgramaQueryPort queryPort;
    private final InstitutionalScopePort scopePort;
    private final SesionProcedenciaQueryPort procedenciaPort;

    public ConsultarAsistenciaProgramaUseCaseImpl(final AsistenciaProgramaQueryPort queryPort,
                                                  final InstitutionalScopePort scopePort,
                                                  final SesionProcedenciaQueryPort procedenciaPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "AsistenciaProgramaQueryPort es obligatorio.");
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.procedenciaPort = Objects.requireNonNull(procedenciaPort, "SesionProcedenciaQueryPort es obligatorio.");
    }

    @Override
    public Pagina<GrupoProgramaProjection> consultarGrupos(final UUID usuario, final UUID idPeriodoAcademico,
                                                            final String busqueda, final Integer pagina,
                                                            final Integer tamano) {
        exigirCoordinador(usuario);
        final String filtro = ObjectHelper.isNull(busqueda) || busqueda.isBlank() ? null : busqueda.trim();
        return queryPort.consultarGrupos(usuario, idPeriodoAcademico, filtro, pagina(pagina), tamano(tamano));
    }

    @Override
    public Pagina<SesionGrupoHorarioDomain> consultarSesionesGrupo(final UUID usuario, final UUID idGrupo,
                                                                    final LocalDate desde, final LocalDate hasta,
                                                                    final Integer pagina, final Integer tamano) {
        exigirCoordinador(usuario);
        exigirId(idGrupo, "El grupo es obligatorio.");
        if (!ObjectHelper.isNull(desde) && !ObjectHelper.isNull(hasta) && desde.isAfter(hasta)) {
            throw new ValidationException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
        final Pagina<SesionGrupoProjection> sesiones =
                queryPort.consultarSesionesGrupo(usuario, idGrupo, desde, hasta, pagina(pagina), tamano(tamano));
        return new Pagina<>(conHorario(idGrupo, sesiones), sesiones.total());
    }

    /**
     * APP-04: la pagina ya viene aislada por programa (provider DB); la procedencia temporal solo se consulta si hay
     * sesiones y solo se aplica a filas del mismo grupo e id. Sin procedencia CONFIRMADA no se ofrecen instantes.
     */
    private List<SesionGrupoHorarioDomain> conHorario(final UUID idGrupo,
                                                      final Pagina<SesionGrupoProjection> sesiones) {
        if (sesiones.elementos().isEmpty()) {
            return List.of();
        }
        final Map<UUID, SesionProcedenciaEntity> procedencias = procedenciaPort.consultarSesionesPorGrupo(idGrupo)
                .stream()
                .map(SesionProcedenciaMapper::toEntity)
                .filter(p -> idGrupo.equals(p.grupo()))
                .collect(Collectors.toMap(SesionProcedenciaEntity::sesion, Function.identity(), (a, b) -> a));
        return sesiones.elementos().stream().map(s -> conHorario(s, procedencias.get(s.id()))).toList();
    }

    private static SesionGrupoHorarioDomain conHorario(final SesionGrupoProjection s,
                                                       final SesionProcedenciaEntity procedencia) {
        final boolean conProcedencia = procedencia != null;
        return new SesionGrupoHorarioDomain(s.id(), s.idGrupo(), s.codigoGrupo(), s.nombreGrupo(), s.nombre(),
                s.numero(), s.codigo(), s.numeroSemana(), s.fechaHoraInicio(), s.fechaHoraFin(),
                conProcedencia ? procedencia.fechaHoraInicioUtc() : null,
                conProcedencia ? procedencia.fechaHoraFinUtc() : null,
                conProcedencia ? procedencia.estadoTemporal() : EstadoHorarioSesion.INDETERMINADA);
    }

    @Override
    public Pagina<AsistenciaSesionProjection> consultarAsistenciasSesion(final UUID usuario, final UUID idSesion,
                                                                          final Boolean asistio, final Integer pagina,
                                                                          final Integer tamano) {
        exigirCoordinador(usuario);
        exigirId(idSesion, "La sesion es obligatoria.");
        return queryPort.consultarAsistenciasSesion(usuario, idSesion, asistio, pagina(pagina), tamano(tamano));
    }

    private void exigirCoordinador(final UUID usuario) {
        if (ObjectHelper.isNull(usuario) || scopePort.findProgramaIdByCoordinadorUsuario(usuario).isEmpty()) {
            throw new ForbiddenException("No fue posible resolver el programa del coordinador autenticado.");
        }
    }

    private static void exigirId(final UUID id, final String mensaje) {
        if (ObjectHelper.isNull(id)) {
            throw new ValidationException(mensaje);
        }
    }

    private static int pagina(final Integer pagina) {
        if (ObjectHelper.isNull(pagina)) {
            return PAGINA_POR_DEFECTO;
        }
        if (pagina < 1) {
            throw new ValidationException("La pagina debe ser mayor o igual a 1.");
        }
        return pagina;
    }

    private static int tamano(final Integer tamano) {
        if (ObjectHelper.isNull(tamano)) {
            return TAMANO_POR_DEFECTO;
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new ValidationException("El tamano de pagina debe estar entre 1 y " + TAMANO_MAXIMO + ".");
        }
        return tamano;
    }
}
