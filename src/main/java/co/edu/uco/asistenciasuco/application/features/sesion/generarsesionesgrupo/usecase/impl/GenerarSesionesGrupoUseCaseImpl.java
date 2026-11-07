package co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.festivos.asegurarfestivos.primaryports.AsegurarFestivosPeriodoInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.GenerarSesionesGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.domain.GenerarSesionesGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.mapper.GenerarSesionesGrupoRepositoryMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort.GeneracionSesiones;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;

/**
 * Genera las sesiones de un grupo segun su horario. CULM-001 B.1 (§3.2.3): antes asegura los festivos de los anios del
 * periodo (mejor esfuerzo, nunca bloquea) y la generacion v2 cancela por festivo las sesiones que caen en uno.
 */
public final class GenerarSesionesGrupoUseCaseImpl implements GenerarSesionesGrupoUseCase {

    private final SesionRepositoryPort sesionRepositoryPort;
    private final AsegurarFestivosPeriodoInputPort asegurarFestivos;

    public GenerarSesionesGrupoUseCaseImpl(final SesionRepositoryPort sesionRepositoryPort,
                                           final AsegurarFestivosPeriodoInputPort asegurarFestivos) {
        this.sesionRepositoryPort = Objects.requireNonNull(sesionRepositoryPort, "SesionRepositoryPort es obligatorio.");
        this.asegurarFestivos = Objects.requireNonNull(asegurarFestivos, "AsegurarFestivosPeriodoInputPort es obligatorio.");
    }

    @Override
    public GeneracionSesiones execute(final GenerarSesionesGrupoDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para generar sesiones de grupo es obligatorio.");
        }
        asegurarFestivos.execute(AsegurarFestivosPeriodoInputPort.Comando.deGrupo(domain.getGrupo()));
        final GeneracionSesiones generacion =
                sesionRepositoryPort.generarSesionesGrupo(GenerarSesionesGrupoRepositoryMapper.toRepositoryDTO(domain));
        return generacion == null ? new GeneracionSesiones(0, 0) : generacion;
    }
}
