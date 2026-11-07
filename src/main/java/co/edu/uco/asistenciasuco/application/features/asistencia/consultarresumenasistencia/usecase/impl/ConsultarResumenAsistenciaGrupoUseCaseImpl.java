package co.edu.uco.asistenciasuco.application.features.asistencia.consultarresumenasistencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.ParametrosPaginacion;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.ResumenAsistenciaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarresumenasistencia.usecase.ConsultarResumenAsistenciaGrupoUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.ResumenAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;

import java.util.Objects;

/**
 * CULM-001 B.2: resumen por horas del grupo. La titularidad se valida aqui (403) y de nuevo en
 * {@code usp_consultar_resumen_asistencia_grupo_docente_paginado}.
 */
public final class ConsultarResumenAsistenciaGrupoUseCaseImpl implements ConsultarResumenAsistenciaGrupoUseCase {

    private final InstitutionalScopePort scope;
    private final ResumenAsistenciaQueryPort resumen;

    public ConsultarResumenAsistenciaGrupoUseCaseImpl(final InstitutionalScopePort scope, final ResumenAsistenciaQueryPort resumen) {
        this.scope = Objects.requireNonNull(scope, "InstitutionalScopePort es obligatorio.");
        this.resumen = Objects.requireNonNull(resumen, "ResumenAsistenciaQueryPort es obligatorio.");
    }

    @Override
    public PaginaResultado<ResumenAsistenciaEstudianteDTO> execute(final Consulta consulta) {
        Objects.requireNonNull(consulta, "La consulta del resumen de asistencia es obligatoria.");
        if (!scope.canDocenteAccessGrupo(consulta.usuarioEjecutor(), consulta.grupoId())) {
            throw new ForbiddenException("El docente autenticado no tiene titularidad sobre el grupo solicitado.");
        }
        final PaginaResultado<ResumenAsistenciaQueryPort.FilaResumen> pagina = resumen.consultarResumenGrupoDocente(
                consulta.grupoId(), ParametrosPaginacion.pagina(consulta.pagina()),
                ParametrosPaginacion.tamano(consulta.tamano()), consulta.usuarioEjecutor());
        return new PaginaResultado<>(
                pagina.elementos().stream()
                        .map(fila -> new ResumenAsistenciaEstudianteDTO(fila.idEstudiante(), fila.nombreCompleto(),
                                fila.codigoEstadoEstudiante(), fila.indicadores()))
                        .toList(),
                pagina.numeroPagina(), pagina.tamanoPagina(), pagina.totalElementos());
    }
}
