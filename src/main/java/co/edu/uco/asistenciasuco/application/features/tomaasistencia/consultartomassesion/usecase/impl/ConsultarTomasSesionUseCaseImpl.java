package co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultartomassesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.SesionTitularAcceso;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultartomassesion.usecase.ConsultarTomasSesionUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;

import java.util.Objects;

/** CULM-001 B.2: tomas de una sesion (titularidad en Application y en el SP {@code usp_consultar_tomas_sesion}). */
public final class ConsultarTomasSesionUseCaseImpl implements ConsultarTomasSesionUseCase {

    private final SesionTitularAcceso sesiones;
    private final TomaAsistenciaRepositoryPort repositorio;

    public ConsultarTomasSesionUseCaseImpl(final SesionTitularAcceso sesiones, final TomaAsistenciaRepositoryPort repositorio) {
        this.sesiones = Objects.requireNonNull(sesiones, "SesionTitularAcceso es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "TomaAsistenciaRepositoryPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Consulta consulta) {
        Objects.requireNonNull(consulta, "La consulta de tomas de la sesion es obligatoria.");
        final SesionProcedenciaRepositoryProjection sesion = sesiones.exigirTitular(consulta.sesionId(), consulta.usuarioEjecutor());
        return new Resultado(
                EstadoSesion.de(sesion.cancelacion() != null),
                CancelacionSesionDTO.desde(sesion.cancelacion()),
                repositorio.consultarTomasSesion(consulta.sesionId(), consulta.usuarioEjecutor()));
    }
}
