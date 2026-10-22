package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

public record DocenteProgramaProjection(
        UUID id,
        UUID idUsuario,
        String numeroIdentificacion,
        String nombreCompleto,
        String correo,
        UUID idPrograma,
        String nombrePrograma,
        int totalGruposAsignados,
        boolean estaActivoDocente
) {
}
