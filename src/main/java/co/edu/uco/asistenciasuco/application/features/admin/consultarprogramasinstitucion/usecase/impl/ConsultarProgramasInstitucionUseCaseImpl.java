package co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.usecase.ConsultarProgramasInstitucionUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaInstitucionQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaInstitucionProjection;

import java.util.Objects;
import java.util.UUID;

/**
 * Programas activos de la institucion del administrador para el selector de la admision (CULM-001 D.4, HU156). El ambito
 * (institucion del administrador) y la validacion de la paginacion los hace el SP; aqui solo se normalizan los valores por defecto.
 */
public final class ConsultarProgramasInstitucionUseCaseImpl implements ConsultarProgramasInstitucionUseCase {

    static final int PAGINA_POR_DEFECTO = 1;
    static final int TAMANO_POR_DEFECTO = 50;

    private final ProgramaInstitucionQueryPort queryPort;

    public ConsultarProgramasInstitucionUseCaseImpl(final ProgramaInstitucionQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "ProgramaInstitucionQueryPort es obligatorio.");
    }

    @Override
    public PaginaResultado<ProgramaInstitucionProjection> execute(
            final UUID usuarioEjecutor,
            final String texto,
            final UUID idFacultad,
            final Integer pagina,
            final Integer tamano
    ) {
        Objects.requireNonNull(usuarioEjecutor, "El usuario ejecutor es obligatorio.");
        final String filtro = texto == null || texto.isBlank() ? null : texto.strip();
        return queryPort.consultarProgramas(usuarioEjecutor, filtro, idFacultad,
                pagina == null ? PAGINA_POR_DEFECTO : pagina, tamano == null ? TAMANO_POR_DEFECTO : tamano);
    }
}
