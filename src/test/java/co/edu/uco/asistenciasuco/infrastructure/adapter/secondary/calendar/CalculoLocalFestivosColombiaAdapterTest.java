package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.calendar;

import co.edu.uco.asistenciasuco.application.secondaryports.calendar.FestivoColombia;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CULM-001 B.1 (CONTRACT 3.4.1): oraculo del calculo local (Ley 51 de 1983 + Ley 2578 de 2026). La tabla de 2026 y
 * 2027 es la del contrato, con Chiquinquira el 13-jul-2026 y el 12-jul-2027.
 */
class CalculoLocalFestivosColombiaAdapterTest {

    private final CalculoLocalFestivosColombiaAdapter adapter = new CalculoLocalFestivosColombiaAdapter();

    @Test
    void la_fuente_es_calculo_local_y_siempre_esta_disponible() {
        assertEquals("CALCULO_LOCAL", adapter.fuente());
        assertTrue(adapter.disponible());
    }

    @Test
    void festivos_2026_coinciden_exactamente_con_la_tabla_del_contrato() {
        assertEquals(List.of(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 12), LocalDate.of(2026, 3, 23), LocalDate.of(2026, 4, 2),
                LocalDate.of(2026, 4, 3), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 18), LocalDate.of(2026, 6, 8),
                LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 29), LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 8, 7), LocalDate.of(2026, 8, 17), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 11, 2),
                LocalDate.of(2026, 11, 16), LocalDate.of(2026, 12, 8), LocalDate.of(2026, 12, 25)
        ), fechas(2026));
        final Map<LocalDate, String> nombres = porFecha(2026);
        assertEquals("Nuestra Señora del Rosario de Chiquinquirá", nombres.get(LocalDate.of(2026, 7, 13)));
        assertEquals("Año Nuevo", nombres.get(LocalDate.of(2026, 1, 1)));
    }

    @Test
    void festivos_2027_coinciden_exactamente_con_la_tabla_del_contrato() {
        assertEquals(List.of(
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 11), LocalDate.of(2027, 3, 22), LocalDate.of(2027, 3, 25),
                LocalDate.of(2027, 3, 26), LocalDate.of(2027, 5, 1), LocalDate.of(2027, 5, 10), LocalDate.of(2027, 5, 31),
                LocalDate.of(2027, 6, 7), LocalDate.of(2027, 7, 5), LocalDate.of(2027, 7, 12), LocalDate.of(2027, 7, 20),
                LocalDate.of(2027, 8, 7), LocalDate.of(2027, 8, 16), LocalDate.of(2027, 10, 18), LocalDate.of(2027, 11, 1),
                LocalDate.of(2027, 11, 15), LocalDate.of(2027, 12, 8), LocalDate.of(2027, 12, 25)
        ), fechas(2027));
        assertEquals("Nuestra Señora del Rosario de Chiquinquirá", porFecha(2027).get(LocalDate.of(2027, 7, 12)));
    }

    @ParameterizedTest
    @CsvSource({
            "2024, 2024-03-31", "2025, 2025-04-20", "2026, 2026-04-05", "2027, 2027-03-28", "2028, 2028-04-16",
            "2029, 2029-04-01", "2030, 2030-04-21", "2031, 2031-04-13", "2032, 2032-03-28", "2033, 2033-04-17",
            "2034, 2034-04-09", "2035, 2035-03-25"
    })
    void la_pascua_coincide_con_las_fechas_conocidas(final int anio, final LocalDate pascua) {
        assertEquals(pascua, CalculoLocalFestivosColombiaAdapter.domingoDePascua(anio));
    }

    @ParameterizedTest
    @CsvSource({"2024", "2026", "2027", "2028", "2035"})
    void cada_anio_tiene_19_festivos_del_anio_y_los_trasladables_caen_en_lunes(final int anio) {
        final List<FestivoColombia> festivos = adapter.obtenerFestivos(anio);
        assertEquals(19, festivos.size());
        assertTrue(festivos.stream().allMatch(f -> f.fecha().getYear() == anio));
        final LocalDate pascua = CalculoLocalFestivosColombiaAdapter.domingoDePascua(anio);
        for (final int dias : new int[]{43, 64, 71}) {
            assertEquals(DayOfWeek.MONDAY, pascua.plusDays(dias).getDayOfWeek());
        }
    }

    // NB-5 / SUPUESTO-B-22: en 2038 Sagrado Corazon (Pascua + 71) y San Pedro (29-jun trasladado) caen el 5-jul.
    @Test
    void en_2038_dos_festivos_coinciden_y_se_fusionan_en_una_fila() {
        final List<FestivoColombia> festivos = adapter.obtenerFestivos(2038);
        assertEquals(18, festivos.size());
        assertEquals("Sagrado Corazón de Jesús / San Pedro y San Pablo", porFecha(2038).get(LocalDate.of(2038, 7, 5)));
    }

    @Test
    void lunes_siguiente_conserva_los_lunes_y_traslada_los_demas_dias() {
        assertEquals(LocalDate.of(2026, 1, 12), CalculoLocalFestivosColombiaAdapter.lunesSiguiente(LocalDate.of(2026, 1, 6)));
        assertEquals(LocalDate.of(2026, 6, 29), CalculoLocalFestivosColombiaAdapter.lunesSiguiente(LocalDate.of(2026, 6, 29)));
    }

    private List<LocalDate> fechas(final int anio) {
        return adapter.obtenerFestivos(anio).stream().map(FestivoColombia::fecha).toList();
    }

    private Map<LocalDate, String> porFecha(final int anio) {
        return adapter.obtenerFestivos(anio).stream()
                .collect(Collectors.toMap(FestivoColombia::fecha, FestivoColombia::nombre, (a, b) -> a));
    }

    // En 2025 y 2030 Sagrado Corazon y San Pedro tambien coinciden (30-jun-2025 y 1-jul-2030): 18 filas.
    @ParameterizedTest
    @CsvSource({"2025, 2025-06-30", "2030, 2030-07-01"})
    void otros_anios_con_festivos_coincidentes_tambien_se_fusionan(final int anio, final LocalDate fecha) {
        assertEquals(18, adapter.obtenerFestivos(anio).size());
        assertTrue(porFecha(anio).get(fecha).contains(" / "));
    }
}
