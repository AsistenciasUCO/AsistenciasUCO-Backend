package co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.common.rule.ConsultaPaginadaRules;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase.ConsultarFacultadDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase.domain.SesionFacultadHorarioDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.SesionProcedenciaEntity;
import co.edu.uco.asistenciasuco.application.features.sesion.common.mapper.SesionProcedenciaMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.EstructuraFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.EstudianteFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.MatriculaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.SesionFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * HU104/HU105/HU109/HU119/HU145. Exige un decano con facultad (403 si no la tiene) y valida paginacion y filtros;
 * el aislamiento por facultad lo garantiza el provider DB, que filtra por el decano ejecutor. En HU145 cada sesion
 * se completa con su procedencia temporal (mismo criterio APP-04 del coordinador) para no mostrar como hora local un
 * valor almacenado en UTC.
 */
public final class ConsultarFacultadDecanoUseCaseImpl implements ConsultarFacultadDecanoUseCase {

    private final FacultadDecanoQueryPort queryPort;
    private final InstitutionalScopePort scopePort;
    private final SesionProcedenciaQueryPort procedenciaPort;

    public ConsultarFacultadDecanoUseCaseImpl(final FacultadDecanoQueryPort queryPort,
                                              final InstitutionalScopePort scopePort,
                                              final SesionProcedenciaQueryPort procedenciaPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "FacultadDecanoQueryPort es obligatorio.");
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.procedenciaPort = Objects.requireNonNull(procedenciaPort, "SesionProcedenciaQueryPort es obligatorio.");
    }

    @Override
    public Pagina<EstudianteFacultadProjection> consultarEstudiantes(final UUID usuario, final String busqueda,
                                                                     final UUID idPrograma, final Boolean estaActivo,
                                                                     final Integer pagina, final Integer tamano) {
        exigirDecano(usuario);
        return queryPort.consultarEstudiantes(usuario, ConsultaPaginadaRules.busqueda(busqueda), idPrograma, estaActivo,
                ConsultaPaginadaRules.pagina(pagina), ConsultaPaginadaRules.tamano(tamano));
    }

    @Override
    public List<MatriculaEstudianteProjection> consultarDetalleEstudiante(final UUID usuario, final UUID idEstudiante) {
        exigirDecano(usuario);
        if (ObjectHelper.isNull(idEstudiante)) {
            throw new ValidationException("El estudiante es obligatorio.");
        }
        final List<MatriculaEstudianteProjection> matriculas = queryPort.consultarDetalleEstudiante(usuario, idEstudiante);
        if (matriculas.isEmpty()) {
            throw new ResourceNotFoundException("El estudiante no existe en la facultad del decano.");
        }
        return matriculas;
    }

    @Override
    public List<EstructuraFacultadProjection> consultarEstructura(final UUID usuario) {
        exigirDecano(usuario);
        return queryPort.consultarEstructura(usuario);
    }

    @Override
    public Pagina<SesionFacultadHorarioDomain> consultarSesiones(final UUID usuario, final LocalDate desde,
                                                                 final LocalDate hasta, final UUID idPrograma,
                                                                 final Integer pagina, final Integer tamano) {
        exigirDecano(usuario);
        if (!ObjectHelper.isNull(desde) && !ObjectHelper.isNull(hasta) && desde.isAfter(hasta)) {
            throw new ValidationException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
        final Pagina<SesionFacultadProjection> sesiones = queryPort.consultarSesiones(usuario, desde, hasta, idPrograma,
                ConsultaPaginadaRules.pagina(pagina), ConsultaPaginadaRules.tamano(tamano));
        return new Pagina<>(conHorario(sesiones.elementos()), sesiones.total());
    }

    /**
     * La pagina ya viene aislada por facultad (provider DB). La procedencia se consulta una vez por grupo de la pagina
     * y solo se aplica a la sesion del mismo grupo e id; sin procedencia CONFIRMADA no se ofrecen instantes UTC.
     */
    private List<SesionFacultadHorarioDomain> conHorario(final List<SesionFacultadProjection> sesiones) {
        if (sesiones.isEmpty()) {
            return List.of();
        }
        final Set<UUID> grupos = new LinkedHashSet<>();
        sesiones.stream().map(SesionFacultadProjection::idGrupo).filter(Objects::nonNull).forEach(grupos::add);
        final Map<UUID, SesionProcedenciaEntity> procedencias = new HashMap<>();
        for (final UUID grupo : grupos) {
            procedenciaPort.consultarSesionesPorGrupo(grupo).stream()
                    .map(SesionProcedenciaMapper::toEntity)
                    .filter(p -> grupo.equals(p.grupo()))
                    .forEach(p -> procedencias.putIfAbsent(p.sesion(), p));
        }
        return sesiones.stream().map(s -> conHorario(s, procedencias.get(s.id()))).toList();
    }

    private static SesionFacultadHorarioDomain conHorario(final SesionFacultadProjection sesion,
                                                          final SesionProcedenciaEntity procedencia) {
        final boolean aplica = procedencia != null && Objects.equals(procedencia.grupo(), sesion.idGrupo());
        return new SesionFacultadHorarioDomain(sesion.id(), sesion.nombre(), sesion.codigo(), sesion.numeroSemana(),
                sesion.fechaHoraInicio(), sesion.fechaHoraFin(), sesion.idGrupo(), sesion.codigoGrupo(),
                sesion.nombreGrupo(), sesion.idAsignatura(), sesion.nombreAsignatura(), sesion.idPrograma(),
                sesion.nombrePrograma(),
                aplica ? procedencia.fechaHoraInicioUtc() : null,
                aplica ? procedencia.fechaHoraFinUtc() : null,
                aplica ? procedencia.estadoTemporal() : EstadoHorarioSesion.INDETERMINADA);
    }

    private void exigirDecano(final UUID usuario) {
        if (ObjectHelper.isNull(usuario) || scopePort.findFacultadIdByDecanoUsuario(usuario).isEmpty()) {
            throw new ForbiddenException("No fue posible resolver la facultad del decano autenticado.");
        }
    }
}
