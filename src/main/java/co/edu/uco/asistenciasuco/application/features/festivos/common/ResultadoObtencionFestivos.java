package co.edu.uco.asistenciasuco.application.features.festivos.common;

import co.edu.uco.asistenciasuco.application.secondaryports.calendar.FestivoColombia;

import java.util.List;

/**
 * Festivos de un anio listos para cargar: fuente que respondio con un conjunto valido, festivos ya fusionados por
 * fecha, si se uso un respaldo (la fuente no es la principal) y la diferencia simetrica de fechas con el calculo local.
 */
public record ResultadoObtencionFestivos(String fuente, List<FestivoColombia> festivos, boolean usoRespaldo,
                                         int diferenciasCalculoLocal) {

    public ResultadoObtencionFestivos {
        festivos = festivos == null ? List.of() : List.copyOf(festivos);
    }
}
