package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.calendar;

import co.edu.uco.asistenciasuco.application.secondaryports.calendar.CalendarioFestivosPort;
import co.edu.uco.asistenciasuco.application.secondaryports.calendar.FestivoColombia;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * CULM-001 B.1 (DEC-017, §3.4.3): calculo local de los festivos nacionales de Colombia, sin red. Siempre responde y es
 * el respaldo final de los proveedores HTTP.
 *
 * <ul>
 *   <li>Ley 51 de 1983, fijos: 1-ene, 1-may, 20-jul, 7-ago, 8-dic, 25-dic.</li>
 *   <li>Ley 51 de 1983, trasladables al lunes siguiente si no caen en lunes: 6-ene, 19-mar, 29-jun, 15-ago, 12-oct,
 *       1-nov, 11-nov.</li>
 *   <li>De Pascua (algoritmo gregoriano anonimo de Meeus/Jones/Butcher): Jueves y Viernes Santo; Ascension (Pascua + 43),
 *       Corpus Christi (Pascua + 64) y Sagrado Corazon (Pascua + 71), todos lunes.</li>
 *   <li>Ley 2578 de 2026: Nuestra Senora del Rosario de Chiquinquira, 9-jul trasladable al lunes.</li>
 * </ul>
 *
 * <p>Devuelve 19 festivos, o menos si dos coinciden en la misma fecha; en ese caso ya vienen fusionados con " / "
 * ([SUPUESTO-B-22], por ejemplo Sagrado Corazon y San Pedro el 5-jul-2038).</p>
 */
public final class CalculoLocalFestivosColombiaAdapter implements CalendarioFestivosPort {

    static final String CODIGO_FUENTE = "CALCULO_LOCAL";

    @Override
    public String fuente() {
        return CODIGO_FUENTE;
    }

    @Override
    public List<FestivoColombia> obtenerFestivos(final int anio) {
        final LocalDate pascua = domingoDePascua(anio);
        final Map<String, LocalDate> festivos = new LinkedHashMap<>();
        festivos.put("Año Nuevo", LocalDate.of(anio, 1, 1));
        festivos.put("Día de los Reyes Magos", lunesSiguiente(LocalDate.of(anio, 1, 6)));
        festivos.put("Día de San José", lunesSiguiente(LocalDate.of(anio, 3, 19)));
        festivos.put("Jueves Santo", pascua.minusDays(3));
        festivos.put("Viernes Santo", pascua.minusDays(2));
        festivos.put("Día del Trabajo", LocalDate.of(anio, 5, 1));
        festivos.put("Ascensión del Señor", pascua.plusDays(43));
        festivos.put("Corpus Christi", pascua.plusDays(64));
        festivos.put("Sagrado Corazón de Jesús", pascua.plusDays(71));
        festivos.put("San Pedro y San Pablo", lunesSiguiente(LocalDate.of(anio, 6, 29)));
        festivos.put("Nuestra Señora del Rosario de Chiquinquirá", lunesSiguiente(LocalDate.of(anio, 7, 9)));
        festivos.put("Día de la Independencia", LocalDate.of(anio, 7, 20));
        festivos.put("Batalla de Boyacá", LocalDate.of(anio, 8, 7));
        festivos.put("La Asunción de la Virgen", lunesSiguiente(LocalDate.of(anio, 8, 15)));
        festivos.put("Día de la Raza", lunesSiguiente(LocalDate.of(anio, 10, 12)));
        festivos.put("Todos los Santos", lunesSiguiente(LocalDate.of(anio, 11, 1)));
        festivos.put("Independencia de Cartagena", lunesSiguiente(LocalDate.of(anio, 11, 11)));
        festivos.put("Día de la Inmaculada Concepción", LocalDate.of(anio, 12, 8));
        festivos.put("Día de Navidad", LocalDate.of(anio, 12, 25));

        final Map<LocalDate, List<String>> porFecha = new TreeMap<>();
        festivos.forEach((nombre, fecha) -> porFecha.computeIfAbsent(fecha, f -> new ArrayList<>()).add(nombre));
        final List<FestivoColombia> resultado = new ArrayList<>(porFecha.size());
        porFecha.forEach((fecha, nombres) -> resultado.add(new FestivoColombia(fecha, String.join(" / ", nombres))));
        return List.copyOf(resultado);
    }

    /** Algoritmo gregoriano anonimo (Meeus/Jones/Butcher). */
    static LocalDate domingoDePascua(final int anio) {
        final int a = anio % 19;
        final int b = anio / 100;
        final int c = anio % 100;
        final int d = b / 4;
        final int e = b % 4;
        final int f = (b + 8) / 25;
        final int g = (b - f + 1) / 3;
        final int h = (19 * a + b - d - g + 15) % 30;
        final int i = c / 4;
        final int k = c % 4;
        final int l = (32 + 2 * e + 2 * i - h - k) % 7;
        final int m = (a + 11 * h + 22 * l) / 451;
        final int mes = (h + l - 7 * m + 114) / 31;
        final int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(anio, mes, dia);
    }

    /** Ley 51 de 1983 (regla Emiliani): si la fecha no cae en lunes se traslada al lunes siguiente. */
    static LocalDate lunesSiguiente(final LocalDate fecha) {
        return fecha.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }
}
