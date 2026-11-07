package co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarasistenciastoma.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.ParametrosPaginacion;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.AsistenciaTomaFila;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaAcceso;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarasistenciastoma.usecase.ConsultarAsistenciasTomaUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;

import java.util.Objects;

/** CULM-001 B.2: matriculas activas del grupo con su detalle en la toma. Titularidad en Application y en el SP; los limites de paginacion los valida el SP. */
public final class ConsultarAsistenciasTomaUseCaseImpl implements ConsultarAsistenciasTomaUseCase {

    private final TomaAsistenciaAcceso acceso;
    private final TomaAsistenciaRepositoryPort repositorio;

    public ConsultarAsistenciasTomaUseCaseImpl(final TomaAsistenciaAcceso acceso, final TomaAsistenciaRepositoryPort repositorio) {
        this.acceso = Objects.requireNonNull(acceso, "TomaAsistenciaAcceso es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "TomaAsistenciaRepositoryPort es obligatorio.");
    }

    @Override
    public PaginaResultado<AsistenciaTomaFila> execute(final Consulta consulta) {
        Objects.requireNonNull(consulta, "La consulta de la toma es obligatoria.");
        acceso.exigirTitular(consulta.tomaId(), consulta.usuarioEjecutor());
        return repositorio.consultarAsistencias(consulta.tomaId(), ParametrosPaginacion.pagina(consulta.pagina()),
                ParametrosPaginacion.tamano(consulta.tamano()), consulta.usuarioEjecutor());
    }
}
