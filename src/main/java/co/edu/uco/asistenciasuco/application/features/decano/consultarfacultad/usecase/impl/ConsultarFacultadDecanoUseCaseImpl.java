package co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.common.rule.ConsultaPaginadaRules;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.usecase.ConsultarFacultadDecanoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.EstructuraFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.EstudianteFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.MatriculaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadDecanoQueryPort.SesionFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * HU104/HU105/HU109/HU119/HU145. Exige un decano con facultad (403 si no la tiene) y valida paginacion y filtros;
 * el aislamiento por facultad lo garantiza el provider DB, que filtra por el decano ejecutor.
 */
public final class ConsultarFacultadDecanoUseCaseImpl implements ConsultarFacultadDecanoUseCase {

    private final FacultadDecanoQueryPort queryPort;
    private final InstitutionalScopePort scopePort;

    public ConsultarFacultadDecanoUseCaseImpl(final FacultadDecanoQueryPort queryPort,
                                              final InstitutionalScopePort scopePort) {
        this.queryPort = Objects.requireNonNull(queryPort, "FacultadDecanoQueryPort es obligatorio.");
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
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
    public Pagina<SesionFacultadProjection> consultarSesiones(final UUID usuario, final LocalDate desde,
                                                              final LocalDate hasta, final UUID idPrograma,
                                                              final Integer pagina, final Integer tamano) {
        exigirDecano(usuario);
        if (!ObjectHelper.isNull(desde) && !ObjectHelper.isNull(hasta) && desde.isAfter(hasta)) {
            throw new ValidationException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
        return queryPort.consultarSesiones(usuario, desde, hasta, idPrograma, ConsultaPaginadaRules.pagina(pagina),
                ConsultaPaginadaRules.tamano(tamano));
    }

    private void exigirDecano(final UUID usuario) {
        if (ObjectHelper.isNull(usuario) || scopePort.findFacultadIdByDecanoUsuario(usuario).isEmpty()) {
            throw new ForbiddenException("No fue posible resolver la facultad del decano autenticado.");
        }
    }
}
