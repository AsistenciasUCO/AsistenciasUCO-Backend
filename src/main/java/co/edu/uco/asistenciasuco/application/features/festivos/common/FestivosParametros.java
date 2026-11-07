package co.edu.uco.asistenciasuco.application.features.festivos.common;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.festivos.exception.FestivoErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;

import java.util.Objects;
import java.util.Optional;

/**
 * CULM-001 B.1 (DEC-018): parametros {@code FESTIVOS/*} con su valor por defecto en codigo igual a la semilla
 * {@code 34_parametros_sesion_festivos.sql}. Un parametro ausente, vacio, mal formado o un catalogo caido usan el valor
 * por defecto: la carga de festivos nunca se detiene por el catalogo.
 */
public final class FestivosParametros {

    public static final String GRUPO = "FESTIVOS";
    public static final String HABILITADO = "HABILITADO";
    public static final String PROVEEDOR_PRINCIPAL = "PROVEEDOR_PRINCIPAL";
    public static final String ORDEN_RESPALDO = "ORDEN_RESPALDO";
    public static final String ANIOS_ADELANTE = "ANIOS_ADELANTE";
    public static final String ANIO_MINIMO = "ANIO_MINIMO";
    public static final String ANIO_MAXIMO = "ANIO_MAXIMO";
    public static final String MINIMO_POR_ANIO = "MINIMO_POR_ANIO";
    public static final String MAXIMO_POR_ANIO = "MAXIMO_POR_ANIO";
    public static final String ATRIBUCION_TEXTO = "ATRIBUCION_TEXTO";
    public static final String ATRIBUCION_URL = "ATRIBUCION_URL";

    public static final String DEFECTO_PROVEEDOR_PRINCIPAL = "CALENDARIOS_NACIONALES";
    public static final String DEFECTO_ORDEN_RESPALDO = "FERIADOS_IO,CALCULO_LOCAL";
    public static final int DEFECTO_ANIOS_ADELANTE = 1;
    public static final int DEFECTO_ANIO_MINIMO = 2000;
    public static final int DEFECTO_ANIO_MAXIMO = 2100;
    public static final int DEFECTO_MINIMO_POR_ANIO = 15;
    public static final int DEFECTO_MAXIMO_POR_ANIO = 25;
    public static final String DEFECTO_ATRIBUCION_TEXTO = "Fuente: Calendarios Nacionales";
    public static final String DEFECTO_ATRIBUCION_URL = "https://calendariosnacionales.com";

    private final ParameterCatalogPort parametros;

    public FestivosParametros(final ParameterCatalogPort parametros) {
        this.parametros = Objects.requireNonNull(parametros, "ParameterCatalogPort es obligatorio.");
    }

    public boolean habilitado() {
        return valor(HABILITADO).map(texto -> Boolean.parseBoolean(texto.strip())).orElse(true);
    }

    public String texto(final String clave, final String defecto) {
        return valor(clave).map(String::strip).filter(texto -> !texto.isEmpty()).orElse(defecto);
    }

    public int entero(final String clave, final int defecto) {
        final Optional<String> texto = valor(clave);
        if (texto.isEmpty()) {
            return defecto;
        }
        try {
            return Integer.parseInt(texto.get().strip());
        } catch (final NumberFormatException exception) {
            return defecto;
        }
    }

    public boolean anioValido(final int anio) {
        return anio >= entero(ANIO_MINIMO, DEFECTO_ANIO_MINIMO) && anio <= entero(ANIO_MAXIMO, DEFECTO_ANIO_MAXIMO);
    }

    /** 400 {@code FES_001} si el anio esta fuera de {@code FESTIVOS/ANIO_MINIMO..ANIO_MAXIMO}. */
    public void exigirAnioValido(final int anio) {
        if (!anioValido(anio)) {
            throw new ValidationException(FestivoErrorCode.FES_001);
        }
    }

    private Optional<String> valor(final String clave) {
        try {
            final Optional<String> valor = parametros.getParameter(GRUPO, clave);
            return valor == null ? Optional.empty() : valor;
        } catch (final RuntimeException exception) {
            return Optional.empty();
        }
    }
}
