package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/**
 * Orden de {@code usp_admitir_estudiante} (CULM-001 D.4). {@code passwordHash} es el HASH de la clave inicial (nulo si la persona ya
 * existe); su representacion de texto no lo imprime.
 */
public record AdmisionEstudianteCommand(
        UUID tipoIdentificacionId,
        Integer numeroIdentificacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        String passwordHash,
        UUID idPrograma,
        UUID usuarioEjecutor
) {

    @Override
    public String toString() {
        return "AdmisionEstudianteCommand[idPrograma=" + idPrograma + ", usuarioEjecutor=" + usuarioEjecutor + "]";
    }
}
