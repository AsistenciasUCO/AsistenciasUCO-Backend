package co.edu.uco.asistenciasuco.application.features.coordinador.creardocente.primaryports.dto;

import java.util.UUID;

public record CrearDocenteDTO(
        String numeroIdentificacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        String password,
        UUID usuario
) {
}
