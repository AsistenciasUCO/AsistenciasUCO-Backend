package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.common.mapper.ProgramaAcademicoMapper;
import co.edu.uco.asistenciasuco.application.features.decano.common.rule.ProgramaAcademicoRules;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.ConsultarProgramasUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain.ConsultarProgramasDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain.ProgramasPaginaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaAcademicoPaginaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.Objects;

public final class ConsultarProgramasUseCaseImpl implements ConsultarProgramasUseCase {

    static final int PAGINA_POR_DEFECTO = 1;
    static final int TAMANO_POR_DEFECTO = 10;

    private final InstitutionalScopePort scopePort;
    private final ProgramaAcademicoQueryPort queryPort;

    public ConsultarProgramasUseCaseImpl(final InstitutionalScopePort scopePort, final ProgramaAcademicoQueryPort queryPort) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.queryPort = Objects.requireNonNull(queryPort, "ProgramaAcademicoQueryPort es obligatorio.");
    }

    @Override
    public ProgramasPaginaDomain execute(final ConsultarProgramasDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los filtros de consulta son obligatorios.");
        }
        final int pagina = domain.pagina() == null ? PAGINA_POR_DEFECTO : domain.pagina();
        final int tamano = domain.tamano() == null ? TAMANO_POR_DEFECTO : domain.tamano();
        if (pagina < 1 || tamano < 1) {
            throw new ValidationException("Los parametros de paginacion deben ser mayores o iguales a 1.");
        }
        // HU136 (certificación de Fase 3): el ADMINISTRADOR consulta los programas de su institución para filtrar el
        // directorio de docentes. El ámbito lo fuerza uv_auth_programa en la DB (institución del administrador, facultad
        // del decano); el decano conserva su regla.
        if (!scopePort.isAdministradorActivo(domain.actorUsuarioId())) {
            ProgramaAcademicoRules.facultadDelDecano(scopePort, domain.actorUsuarioId());
        }
        final String nombre = domain.nombre() == null || domain.nombre().isBlank() ? null : domain.nombre().trim();
        final ProgramaAcademicoPaginaProjection resultado = queryPort.consultar(new ProgramaAcademicoQueryPort.ConsultaProgramas(
                domain.actorUsuarioId(), nombre, domain.estaActivo(), domain.idTipoPrograma(), pagina, tamano));
        return new ProgramasPaginaDomain(
                resultado.programas().stream().map(ProgramaAcademicoMapper::toDomain).toList(),
                resultado.totalRegistros());
    }
}
