package co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.usecase;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaInstitucionProjection;

import java.util.UUID;

public interface ConsultarProgramasInstitucionUseCase {

    /** {@code pagina} (1) y {@code tamano} (50) opcionales; {@code texto} en blanco equivale a sin filtro. */
    PaginaResultado<ProgramaInstitucionProjection> execute(UUID usuarioEjecutor, String texto, UUID idFacultad, Integer pagina,
                                                           Integer tamano);
}
