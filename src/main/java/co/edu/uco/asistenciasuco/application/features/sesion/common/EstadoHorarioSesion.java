package co.edu.uco.asistenciasuco.application.features.sesion.common;

import java.util.Set;

/**
 * Estado temporal de una sesion leida por el contrato v2 (UTC-D06/D07).
 *
 * <p>Solo una procedencia conocida y confirmada permite tratar los DATETIME2 almacenados como
 * instantes UTC. Cualquier otro caso (NULL o valor desconocido) es INDETERMINADA: las horas no se
 * exponen, porque un DATETIME2 sin marca no prueba que el valor historico sea UTC.</p>
 */
public enum EstadoHorarioSesion {

    CONFIRMADA,
    INDETERMINADA;

    private static final Set<String> PROCEDENCIAS_CONFIRMADAS = Set.of("UTC_V2", "UTC_GENERADOR", "UTC_OWNER");

    public static EstadoHorarioSesion desdeProcedencia(final String procedenciaTemporal) {
        return procedenciaTemporal != null && PROCEDENCIAS_CONFIRMADAS.contains(procedenciaTemporal)
                ? CONFIRMADA
                : INDETERMINADA;
    }
}
