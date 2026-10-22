package co.edu.uco.asistenciasuco.application.features.coordinador.common.dto;

import java.util.UUID;

public record DocenteProgramaDTO(
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
