package co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.dto;

import java.util.UUID;

/**
 * Respuesta 201 de la admision (CULM-001 D.4). {@code estadoCuenta}: ACTIVA | PENDIENTE_VERIFICACION |
 * PENDIENTE_SINCRONIZACION; {@code cuentaReparada} es true cuando la persona ya estaba admitida y se creo su cuenta faltante.
 */
public record AdmisionEstudianteDTO(
        UUID idEstudiante,
        UUID idUsuario,
        boolean esUsuarioNuevo,
        String estadoCuenta,
        boolean cuentaReparada
) {
}
