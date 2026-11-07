package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.calendar;

import co.edu.uco.asistenciasuco.application.secondaryports.calendar.CalendarioFestivosPort;
import co.edu.uco.asistenciasuco.application.secondaryports.calendar.FestivoColombia;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * CULM-001 B.1 (DEC-017, §3.4.2): respaldo 1 con implementacion minima. {@code GET FESTIVOS/URL_FERIADOS_IO} con
 * {@code Authorization: Bearer} y la API key leida de {@link SecretVaultPort} con el nombre
 * {@code FESTIVOS/NOMBRE_SECRETO_FERIADOS_IO}. Sin la key el adaptador se declara no disponible y se salta. La key nunca
 * se registra en logs ni en mensajes de error. Lee {@code date}/{@code name} de un arreglo raiz o de
 * {@code holidays}/{@code data}.
 */
public final class FeriadosIoFestivosAdapter implements CalendarioFestivosPort {

    static final String CODIGO_FUENTE = "FERIADOS_IO";
    static final String CLAVE_URL = "URL_FERIADOS_IO";
    static final String URL_POR_DEFECTO = "https://api.feriados.io/v1/CO/holidays/{anio}";
    static final String CLAVE_NOMBRE_SECRETO = "NOMBRE_SECRETO_FERIADOS_IO";
    static final String NOMBRE_SECRETO_POR_DEFECTO = "feriados-io-api-key";

    private final FestivosHttpSupport http;
    private final SecretVaultPort secretos;

    public FeriadosIoFestivosAdapter(final HttpClient httpClient, final ParameterCatalogPort parametros,
                                     final SecretVaultPort secretos) {
        this.http = new FestivosHttpSupport(httpClient, parametros);
        // Sin baul de secretos configurado el proveedor queda no disponible (se salta, no falla el flujo).
        this.secretos = secretos;
    }

    @Override
    public String fuente() {
        return CODIGO_FUENTE;
    }

    @Override
    public boolean disponible() {
        return apiKey().isPresent();
    }

    @Override
    public List<FestivoColombia> obtenerFestivos(final int anio) {
        final String apiKey = apiKey().orElseThrow(() -> new CalendarioFestivosException(CODIGO_FUENTE + " no tiene API key."));
        final JsonNode raiz = http.get(CODIGO_FUENTE, http.parametro(CLAVE_URL, URL_POR_DEFECTO), anio,
                Map.of("Authorization", "Bearer " + apiKey));
        final JsonNode arreglo = raiz.isArray() ? raiz : raiz.has("holidays") ? raiz.get("holidays") : raiz.get("data");
        return FestivosHttpSupport.leerFestivos(CODIGO_FUENTE, arreglo, anio);
    }

    private Optional<String> apiKey() {
        if (secretos == null) {
            return Optional.empty();
        }
        try {
            return Objects.requireNonNullElse(
                            secretos.getSecret(http.parametro(CLAVE_NOMBRE_SECRETO, NOMBRE_SECRETO_POR_DEFECTO)),
                            Optional.<String>empty())
                    .filter(key -> !key.isBlank());
        } catch (final RuntimeException exception) {
            return Optional.empty();
        }
    }
}
