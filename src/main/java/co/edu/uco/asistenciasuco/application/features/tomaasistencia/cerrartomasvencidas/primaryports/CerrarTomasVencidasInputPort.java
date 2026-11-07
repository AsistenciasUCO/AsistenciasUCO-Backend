package co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartomasvencidas.primaryports;

/**
 * CULM-001 B.2 ([SUPUESTO-B-09], contrato 3.5): el job del servidor cierra las tomas abiertas cuya ventana vencio o cuya
 * sesion se cancelo. Devuelve cuantas tomas cerro en la pasada.
 */
public interface CerrarTomasVencidasInputPort {

    int execute();
}
