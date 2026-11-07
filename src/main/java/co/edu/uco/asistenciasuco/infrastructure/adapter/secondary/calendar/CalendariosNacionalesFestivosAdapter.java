package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.calendar;

import co.edu.uco.asistenciasuco.application.secondaryports.calendar.CalendarioFestivosPort;
import co.edu.uco.asistenciasuco.application.secondaryports.calendar.FestivoColombia;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

/**
 * CULM-001 B.1 (DEC-017, §3.4.1): fuente principal. {@code GET FESTIVOS/URL_CALENDARIOS_NACIONALES} con {@code {anio}}
 * (archivo {@code nacionales.json}); lee {@code holidays[].date} y {@code holidays[].name} e ignora el resto de campos.
 * Sin credenciales.
 */
public final class CalendariosNacionalesFestivosAdapter implements CalendarioFestivosPort {

    static final String CODIGO_FUENTE = "CALENDARIOS_NACIONALES";
    static final String CLAVE_URL = "URL_CALENDARIOS_NACIONALES";
    static final String URL_POR_DEFECTO = "https://calendariosnacionales.com/co/v1/{anio}/nacionales.json";

    private final FestivosHttpSupport http;

    public CalendariosNacionalesFestivosAdapter(final HttpClient httpClient, final ParameterCatalogPort parametros) {
        this.http = new FestivosHttpSupport(httpClient, parametros);
    }

    @Override
    public String fuente() {
        return CODIGO_FUENTE;
    }

    @Override
    public List<FestivoColombia> obtenerFestivos(final int anio) {
        return FestivosHttpSupport.leerFestivos(CODIGO_FUENTE,
                http.get(CODIGO_FUENTE, http.parametro(CLAVE_URL, URL_POR_DEFECTO), anio, Map.of()).get("holidays"), anio);
    }
}
