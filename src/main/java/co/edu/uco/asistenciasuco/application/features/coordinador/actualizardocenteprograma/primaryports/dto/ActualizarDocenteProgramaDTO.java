package co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.primaryports.dto;

import java.util.UUID;

public record ActualizarDocenteProgramaDTO(
        UUID actorUsuarioId,
        UUID idDocente,
        UUID idTipoVinculacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        UUID idTipoIdentificacion,
        Integer numeroIdentificacion,
        String motivo
) {

    /** Edicion sin correccion de documento (contrato original de HU092). */
    public ActualizarDocenteProgramaDTO(final UUID actorUsuarioId, final UUID idDocente, final UUID idTipoVinculacion,
            final String primerNombre, final String segundoNombre, final String primerApellido, final String segundoApellido) {
        this(actorUsuarioId, idDocente, idTipoVinculacion, primerNombre, segundoNombre, primerApellido, segundoApellido,
                null, null, null);
    }
}
