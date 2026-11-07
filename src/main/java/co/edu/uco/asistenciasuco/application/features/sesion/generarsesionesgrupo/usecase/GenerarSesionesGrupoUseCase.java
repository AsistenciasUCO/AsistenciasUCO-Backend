package co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase;

import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.usecase.domain.GenerarSesionesGrupoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort.GeneracionSesiones;

public interface GenerarSesionesGrupoUseCase {

    GeneracionSesiones execute(GenerarSesionesGrupoDomain domain);
}
