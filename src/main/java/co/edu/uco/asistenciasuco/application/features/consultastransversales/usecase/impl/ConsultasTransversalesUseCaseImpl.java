package co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.ConsultasTransversalesUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.common.rule.ConsultaPaginadaRules;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.AsignaturaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.DocenteFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.EstudianteInstitucionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroConsulta;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.HistorialEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.ProgramaCreditosProjection;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * HU112/HU128/HU143/HU157. Valida la entrada (paginacion, busqueda, estudiante obligatorio) y delega en el provider
 * DB, que exige el perfil (SEC_001 -> 403) y limita las filas al ambito del ejecutor: la facultad del decano o la
 * institucion del administrador. Un historial vacio significa que el estudiante no existe en ese ambito (404).
 */
public final class ConsultasTransversalesUseCaseImpl implements ConsultasTransversalesUseCase {

    private final ConsultasTransversalesQueryPort queryPort;

    public ConsultasTransversalesUseCaseImpl(final ConsultasTransversalesQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "ConsultasTransversalesQueryPort es obligatorio.");
    }

    @Override
    public Pagina<ProgramaCreditosProjection> consultarProgramasCreditos(final UUID usuario, final String busqueda,
                                                                         final Boolean estaActivo, final Integer pagina,
                                                                         final Integer tamano) {
        return queryPort.consultarProgramasCreditos(exigirUsuario(usuario), ConsultaPaginadaRules.busqueda(busqueda),
                estaActivo, ConsultaPaginadaRules.pagina(pagina), ConsultaPaginadaRules.tamano(tamano));
    }

    @Override
    public Pagina<DocenteFacultadProjection> consultarDocentes(final UUID usuario, final FiltroConsulta filtro,
                                                               final Integer pagina, final Integer tamano) {
        return queryPort.consultarDocentes(exigirUsuario(usuario), normalizar(filtro),
                ConsultaPaginadaRules.pagina(pagina), ConsultaPaginadaRules.tamano(tamano));
    }

    @Override
    public Pagina<AsignaturaProjection> buscarAsignaturas(final UUID usuario, final FiltroConsulta filtro,
                                                          final Integer pagina, final Integer tamano) {
        return queryPort.buscarAsignaturas(exigirUsuario(usuario), normalizar(filtro),
                ConsultaPaginadaRules.pagina(pagina), ConsultaPaginadaRules.tamano(tamano));
    }

    @Override
    public Pagina<EstudianteInstitucionProjection> buscarEstudiantes(final UUID usuario, final FiltroConsulta filtro,
                                                                     final Integer pagina, final Integer tamano) {
        return queryPort.buscarEstudiantes(exigirUsuario(usuario), normalizar(filtro),
                ConsultaPaginadaRules.pagina(pagina), ConsultaPaginadaRules.tamano(tamano));
    }

    @Override
    public List<HistorialEstudianteProjection> consultarHistorial(final UUID usuario, final UUID idEstudiante) {
        if (ObjectHelper.isNull(idEstudiante)) {
            throw new ValidationException("El estudiante es obligatorio.");
        }
        final List<HistorialEstudianteProjection> historial = queryPort.consultarHistorial(exigirUsuario(usuario),
                idEstudiante);
        if (historial.isEmpty()) {
            throw new ResourceNotFoundException("El estudiante no existe en su ambito.");
        }
        return historial;
    }

    private static UUID exigirUsuario(final UUID usuario) {
        if (ObjectHelper.isNull(usuario)) {
            throw new ForbiddenException("No fue posible resolver el usuario autenticado.");
        }
        return usuario;
    }

    private static FiltroConsulta normalizar(final FiltroConsulta filtro) {
        if (ObjectHelper.isNull(filtro)) {
            return new FiltroConsulta(null, null, null);
        }
        return new FiltroConsulta(ConsultaPaginadaRules.busqueda(filtro.busqueda()), filtro.idPadre(),
                filtro.estaActivo());
    }
}
