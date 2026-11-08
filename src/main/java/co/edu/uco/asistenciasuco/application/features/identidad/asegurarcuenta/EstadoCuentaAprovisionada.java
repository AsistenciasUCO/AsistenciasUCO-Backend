package co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta;

/** Estado de la cuenta de identidad tras aprovisionarla (CULM-001 D.4). */
public enum EstadoCuentaAprovisionada {
    /** La cuenta existe y esta habilitada. */
    ACTIVA,
    /** Cuenta nueva creada deshabilitada hasta que el titular verifique su correo (IDENT-001). */
    PENDIENTE_VERIFICACION,
    /** El proveedor de identidad fallo: la base de datos quedo confirmada y el desfase registrado; se repara al repetir. */
    PENDIENTE_SINCRONIZACION
}
