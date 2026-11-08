package co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta;

/**
 * Resultado de asegurar la cuenta de identidad: su estado y si la cuenta faltaba y se creo al repetir una admision
 * ({@code cuentaReparada}).
 */
public record ResultadoCuentaIdentidad(EstadoCuentaAprovisionada estado, boolean cuentaReparada) {
}
