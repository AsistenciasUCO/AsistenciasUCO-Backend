package co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports;

import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.dto.GeneracionSesionesGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.generarsesionesgrupo.primaryports.dto.GenerarSesionesGrupoDTO;

public interface GenerarSesionesGrupoInputPort {

    GeneracionSesionesGrupoDTO execute(GenerarSesionesGrupoDTO dto);
}
