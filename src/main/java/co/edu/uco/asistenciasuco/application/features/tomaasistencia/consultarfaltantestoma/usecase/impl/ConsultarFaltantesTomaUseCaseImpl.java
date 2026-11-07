package co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarfaltantestoma.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.ParametrosPaginacion;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.FaltanteTomaFila;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaAcceso;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarfaltantestoma.usecase.ConsultarFaltantesTomaUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;

import java.util.Objects;

/** CULM-001 B.2: faltantes de la toma (activos sin registro presente). Titularidad en Application y en el SP; los limites de paginacion los valida el SP. */
public final class ConsultarFaltantesTomaUseCaseImpl implements ConsultarFaltantesTomaUseCase {

    private final TomaAsistenciaAcceso acceso;
    private final TomaAsistenciaRepositoryPort repositorio;

    public ConsultarFaltantesTomaUseCaseImpl(final TomaAsistenciaAcceso acceso, final TomaAsistenciaRepositoryPort repositorio) {
        this.acceso = Objects.requireNonNull(acceso, "TomaAsistenciaAcceso es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "TomaAsistenciaRepositoryPort es obligatorio.");
    }

    @Override
    public PaginaResultado<FaltanteTomaFila> execute(final Consulta consulta) {
        Objects.requireNonNull(consulta, "La consulta de la toma es obligatoria.");
        acceso.exigirTitular(consulta.tomaId(), consulta.usuarioEjecutor());
        return repositorio.consultarFaltantes(consulta.tomaId(), ParametrosPaginacion.pagina(consulta.pagina()),
                ParametrosPaginacion.tamano(consulta.tamano()), consulta.usuarioEjecutor());
    }
}
