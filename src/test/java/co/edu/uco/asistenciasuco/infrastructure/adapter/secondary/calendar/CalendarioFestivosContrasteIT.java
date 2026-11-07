package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.calendar;

import co.edu.uco.asistenciasuco.application.secondaryports.calendar.CalendarioFestivosPort.CalendarioFestivosException;
import co.edu.uco.asistenciasuco.application.secondaryports.calendar.FestivoColombia;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CULM-001 B.1 (CONTRACT 3.4.1): contraste del calculo local con la fuente principal real (calendariosnacionales.com)
 * para 2026 y 2027. Necesita red: sin ella la prueba se omite con {@code Assumptions} y queda como verificacion manual
 * de la Fase 3.
 */
@Tag("integration")
class CalendarioFestivosContrasteIT {

    @ParameterizedTest
    @ValueSource(ints = {2026, 2027})
    void el_calculo_local_coincide_con_calendarios_nacionales(final int anio) {
        final ParameterCatalogPort catalogo = mock(ParameterCatalogPort.class);
        when(catalogo.getParameter(anyString(), anyString())).thenReturn(Optional.empty());
        final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL).build();

        final List<FestivoColombia> remotos;
        try {
            remotos = new CalendariosNacionalesFestivosAdapter(http, catalogo).obtenerFestivos(anio);
        } catch (final CalendarioFestivosException sinRed) {
            assumeTrue(false, "Sin acceso a calendariosnacionales.com: " + sinRed.getMessage());
            return;
        }

        final List<LocalDate> fechasRemotas = remotos.stream().map(FestivoColombia::fecha).distinct().sorted().toList();
        final List<LocalDate> fechasLocales = new CalculoLocalFestivosColombiaAdapter().obtenerFestivos(anio).stream()
                .map(FestivoColombia::fecha).toList();
        assertEquals(fechasLocales, fechasRemotas);
    }
}
