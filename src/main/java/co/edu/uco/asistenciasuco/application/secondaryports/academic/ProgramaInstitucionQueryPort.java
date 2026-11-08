package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaInstitucionProjection;

import java.util.UUID;

public interface ProgramaInstitucionQueryPort {

    /** Programas activos de la institucion del administrador (el ambito lo aplica la base de datos), paginados. */
    PaginaResultado<ProgramaInstitucionProjection> consultarProgramas(UUID usuarioEjecutor, String texto, UUID idFacultad,
                                                                      int numeroPagina, int tamanoPagina);
}
