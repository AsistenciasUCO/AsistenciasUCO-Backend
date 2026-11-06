package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.domain;

import java.util.UUID;

/**
 * Modelo de resultado interno de la consulta de coordinadores.
 */
public record CoordinadorDomain(UUID id, UUID idUsuario, String numeroIdentificacion, String nombreCompleto,
                                 UUID idPrograma, String nombrePrograma, boolean estaActivoCoordinador,
                                 String primerNombre, String segundoNombre, String primerApellido,
                                 String segundoApellido, String correo, boolean estaActivoRolCoordinador,
                                String nombreFacultad, int totalDocentes, int totalGrupos) {

    /** Compatibilidad: sin facultad ni conteos (certificacion F3). */
    public CoordinadorDomain(final UUID id, final UUID idUsuario, final String numeroIdentificacion, final String nombreCompleto,
            final UUID idPrograma, final String nombrePrograma, final boolean estaActivoCoordinador,
            final String primerNombre, final String segundoNombre, final String primerApellido,
            final String segundoApellido, final String correo, final boolean estaActivoRolCoordinador) {
        this(id, idUsuario, numeroIdentificacion, nombreCompleto, idPrograma, nombrePrograma, estaActivoCoordinador,
                primerNombre, segundoNombre, primerApellido, segundoApellido, correo, estaActivoRolCoordinador,
                null, 0, 0);
    }

    /** Modelo sin los datos editables. */
    public CoordinadorDomain(final UUID id, final UUID idUsuario, final String numeroIdentificacion,
                             final String nombreCompleto, final UUID idPrograma, final String nombrePrograma,
                             final boolean estaActivoCoordinador) {
        this(id, idUsuario, numeroIdentificacion, nombreCompleto, idPrograma, nombrePrograma, estaActivoCoordinador,
                null, null, null, null, null, estaActivoCoordinador);
    }
}
