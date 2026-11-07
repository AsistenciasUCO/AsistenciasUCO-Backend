package co.edu.uco.asistenciasuco.application.features.festivos.cargarfestivosautomatico.primaryports;

/**
 * CULM-001 B.1: lo invoca el job diario del servidor. Carga el anio actual y los {@code FESTIVOS/ANIOS_ADELANTE}
 * siguientes que aun no tienen festivos y los aplica a las sesiones. Devuelve cuantos anios cargo.
 */
public interface CargarFestivosAutomaticoInputPort {

    int execute();
}
