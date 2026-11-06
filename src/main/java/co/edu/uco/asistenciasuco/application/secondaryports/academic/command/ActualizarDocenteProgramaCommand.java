package co.edu.uco.asistenciasuco.application.secondaryports.academic.command;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO;

import java.util.UUID;

/**
 * Edicion de un vinculo docente-programa (HU092). Los campos nulos no se modifican; "" en segundo nombre/apellido los
 * limpia. HU159: {@code documento} corrige el tipo y el numero de documento (opcional, con motivo).
 */
public record ActualizarDocenteProgramaCommand(
        UUID idDocente,
        UUID idPrograma,
        UUID idTipoVinculacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        UUID usuarioEjecutor,
        CorreccionDocumentoDTO documento
) {

    /** Edicion sin correccion de documento (contrato original de HU092). */
    public ActualizarDocenteProgramaCommand(final UUID idDocente, final UUID idPrograma, final UUID idTipoVinculacion,
            final String primerNombre, final String segundoNombre, final String primerApellido,
            final String segundoApellido, final UUID usuarioEjecutor) {
        this(idDocente, idPrograma, idTipoVinculacion, primerNombre, segundoNombre, primerApellido, segundoApellido,
                usuarioEjecutor, CorreccionDocumentoDTO.ninguna());
    }
}
