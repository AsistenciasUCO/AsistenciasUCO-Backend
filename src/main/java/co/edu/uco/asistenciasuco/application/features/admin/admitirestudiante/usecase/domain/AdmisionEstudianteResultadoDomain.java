package co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.domain;

import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.EstadoCuentaAprovisionada;

import java.util.UUID;

/** Resultado de la admision (CULM-001 D.4): ids, si el Usuario era nuevo, estado de la cuenta de identidad y si se reparo. */
public record AdmisionEstudianteResultadoDomain(
        UUID idEstudiante,
        UUID idUsuario,
        boolean esUsuarioNuevo,
        EstadoCuentaAprovisionada estadoCuenta,
        boolean cuentaReparada
) {
}
