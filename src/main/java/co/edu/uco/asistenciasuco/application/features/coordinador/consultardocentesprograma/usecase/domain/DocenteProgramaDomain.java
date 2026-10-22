package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.domain;

import java.util.UUID;

public record DocenteProgramaDomain(
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
