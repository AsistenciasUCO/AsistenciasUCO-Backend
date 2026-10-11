package co.edu.uco.asistenciasuco.application.features.sesion.common;

/**
 * Contrato temporal con el que llegan las horas de una escritura de sesion (MAINT-003F / UTC-D06).
 *
 * <p>No es un dato del cliente: lo fija el adaptador HTTP segun la version de la ruta. v1 recibe
 * fecha-hora local sin zona, por lo que la persistencia deja la procedencia indeterminada; v2 recibe
 * RFC3339 con offset explicito normalizado al instante UTC, por lo que la persistencia la confirma.</p>
 */
public enum ContratoHorarioSesion {

    /** /api/v1: fecha-hora sin zona; la DB registra procedenciaTemporal = NULL. */
    LOCAL_SIN_ZONA_V1,

    /** /api/v2: instante UTC confirmado (perfil UTC-D02); la DB registra procedenciaTemporal = UTC_V2. */
    UTC_CONFIRMADO_V2
}
