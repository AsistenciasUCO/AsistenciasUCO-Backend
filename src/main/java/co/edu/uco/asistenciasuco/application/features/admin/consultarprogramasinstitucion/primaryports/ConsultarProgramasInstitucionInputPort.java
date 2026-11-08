package co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports.dto.ProgramaInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;

import java.util.UUID;

public interface ConsultarProgramasInstitucionInputPort {

    PaginaResultado<ProgramaInstitucionDTO> execute(UUID usuarioEjecutor, String texto, UUID idFacultad, Integer pagina, Integer tamano);
}
