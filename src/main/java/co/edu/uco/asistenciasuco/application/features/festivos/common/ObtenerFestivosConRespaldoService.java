package co.edu.uco.asistenciasuco.application.features.festivos.common;

import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.festivos.exception.FestivoErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.calendar.CalendarioFestivosPort;
import co.edu.uco.asistenciasuco.application.secondaryports.calendar.FestivoColombia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * CULM-001 B.1 (DEC-017): obtiene los festivos de un anio probando los proveedores en el orden
 * {@code FESTIVOS/PROVEEDOR_PRINCIPAL} + {@code FESTIVOS/ORDEN_RESPALDO}. Usa el primero que responde con un conjunto
 * valido (entre {@code MINIMO_POR_ANIO} y {@code MAXIMO_POR_ANIO}, todas las fechas del anio), fusiona los festivos de
 * la misma fecha ({@code " / "}, [SUPUESTO-B-22]) y registra en el log (con la correlacion del contexto de logging) las fechas que
 * difieren del calculo local. Gana la fuente que respondio; si todas fallan, el calculo local siempre responde.
 */
public final class ObtenerFestivosConRespaldoService {

    public static final String FUENTE_CALCULO_LOCAL = "CALCULO_LOCAL";
    static final String SEPARADOR_NOMBRES = " / ";
    private static final int LONGITUD_MAXIMA_NOMBRE = 120;
    private static final Logger LOGGER = LoggerFactory.getLogger(ObtenerFestivosConRespaldoService.class);

    private final Map<String, CalendarioFestivosPort> proveedores;
    private final FestivosParametros parametros;

    public ObtenerFestivosConRespaldoService(final List<CalendarioFestivosPort> proveedores,
                                             final FestivosParametros parametros) {
        Objects.requireNonNull(proveedores, "Los proveedores de festivos son obligatorios.");
        this.proveedores = new LinkedHashMap<>();
        proveedores.forEach(proveedor -> this.proveedores.putIfAbsent(normalizar(proveedor.fuente()), proveedor));
        this.parametros = Objects.requireNonNull(parametros, "FestivosParametros es obligatorio.");
    }

    public ResultadoObtencionFestivos obtener(final int anio) {
        final List<String> orden = orden();
        final List<FestivoColombia> local = consultar(FUENTE_CALCULO_LOCAL, anio);
        final List<String> fallidas = new ArrayList<>();
        for (final String fuente : orden) {
            final List<FestivoColombia> festivos = FUENTE_CALCULO_LOCAL.equals(fuente) ? local : consultar(fuente, anio);
            if (festivos == null || !esValido(anio, festivos)) {
                fallidas.add(fuente);
                continue;
            }
            final boolean usoRespaldo = !fuente.equals(orden.getFirst());
            if (usoRespaldo) {
                LOGGER.warn("Festivos obtenidos de un respaldo. codigo={}, anio={}, fuente={}, fuentesFallidas={}",
                        FestivoErrorCode.FES_010.code(), anio, fuente, fallidas);
            }
            return new ResultadoObtencionFestivos(fuente, festivos, usoRespaldo, diferencias(anio, fuente, festivos, local));
        }
        if (local != null && esValido(anio, local)) {
            LOGGER.error("Ningun proveedor de festivos respondio; se usa el calculo local. codigo={}, anio={}, fuentesFallidas={}",
                    FestivoErrorCode.FES_010.code(), anio, fallidas);
            return new ResultadoObtencionFestivos(FUENTE_CALCULO_LOCAL, local, true, 0);
        }
        throw new InternalApplicationException(FestivoErrorCode.FES_010,
                "Ningun proveedor de festivos entrego un conjunto valido para el anio " + anio + ".");
    }

    /** Fusiona los festivos de la misma fecha (nombres distintos unidos por " / ") y los ordena por fecha. */
    static List<FestivoColombia> fusionar(final List<FestivoColombia> festivos) {
        if (festivos == null) {
            return List.of();
        }
        final Map<LocalDate, Set<String>> porFecha = new TreeMap<>();
        for (final FestivoColombia festivo : festivos) {
            if (festivo != null) {
                porFecha.computeIfAbsent(festivo.fecha(), fecha -> new LinkedHashSet<>()).add(festivo.nombre().strip());
            }
        }
        final List<FestivoColombia> resultado = new ArrayList<>(porFecha.size());
        porFecha.forEach((fecha, nombres) -> resultado.add(new FestivoColombia(fecha, String.join(SEPARADOR_NOMBRES, nombres))));
        return List.copyOf(resultado);
    }

    private List<String> orden() {
        final Set<String> orden = new LinkedHashSet<>();
        orden.add(normalizar(parametros.texto(FestivosParametros.PROVEEDOR_PRINCIPAL,
                FestivosParametros.DEFECTO_PROVEEDOR_PRINCIPAL)));
        final String respaldo = parametros.texto(FestivosParametros.ORDEN_RESPALDO, FestivosParametros.DEFECTO_ORDEN_RESPALDO);
        for (final String fuente : respaldo.split(",")) {
            final String normalizada = normalizar(fuente);
            if (!normalizada.isEmpty()) {
                orden.add(normalizada);
            }
        }
        return List.copyOf(orden);
    }

    private List<FestivoColombia> consultar(final String fuente, final int anio) {
        final CalendarioFestivosPort proveedor = proveedores.get(fuente);
        if (proveedor == null || !proveedor.disponible()) {
            LOGGER.info("Proveedor de festivos no disponible. anio={}, fuente={}", anio, fuente);
            return null;
        }
        try {
            return fusionar(proveedor.obtenerFestivos(anio));
        } catch (final RuntimeException exception) {
            LOGGER.warn("Fallo el proveedor de festivos. anio={}, fuente={}, error={}", anio, fuente,
                    exception.getClass().getSimpleName());
            return null;
        }
    }

    private boolean esValido(final int anio, final List<FestivoColombia> festivos) {
        final int minimo = parametros.entero(FestivosParametros.MINIMO_POR_ANIO, FestivosParametros.DEFECTO_MINIMO_POR_ANIO);
        final int maximo = parametros.entero(FestivosParametros.MAXIMO_POR_ANIO, FestivosParametros.DEFECTO_MAXIMO_POR_ANIO);
        if (festivos.size() < minimo || festivos.size() > maximo) {
            LOGGER.warn("Conjunto de festivos con cantidad invalida. anio={}, total={}", anio, festivos.size());
            return false;
        }
        final boolean valido = festivos.stream().allMatch(festivo -> festivo.fecha().getYear() == anio
                && !festivo.nombre().isBlank() && festivo.nombre().length() <= LONGITUD_MAXIMA_NOMBRE);
        if (!valido) {
            LOGGER.warn("Conjunto de festivos con fechas o nombres invalidos. anio={}", anio);
        }
        return valido;
    }

    private static int diferencias(final int anio, final String fuente, final List<FestivoColombia> festivos,
                                   final List<FestivoColombia> local) {
        if (local == null || FUENTE_CALCULO_LOCAL.equals(fuente)) {
            return 0;
        }
        final Set<LocalDate> fechasFuente = fechas(festivos);
        final Set<LocalDate> fechasLocal = fechas(local);
        final Set<LocalDate> soloEnFuente = new TreeSet<>(fechasFuente);
        soloEnFuente.removeAll(fechasLocal);
        final Set<LocalDate> soloEnLocal = new TreeSet<>(fechasLocal);
        soloEnLocal.removeAll(fechasFuente);
        final int total = soloEnFuente.size() + soloEnLocal.size();
        if (total > 0) {
            LOGGER.warn("Festivos con diferencias frente al calculo local. anio={}, fuente={}, diferencias={}, "
                    + "soloEnFuente={}, soloEnCalculoLocal={}", anio, fuente, total, soloEnFuente, soloEnLocal);
        }
        return total;
    }

    private static Set<LocalDate> fechas(final List<FestivoColombia> festivos) {
        final Set<LocalDate> fechas = new TreeSet<>();
        festivos.forEach(festivo -> fechas.add(festivo.fecha()));
        return fechas;
    }

    private static String normalizar(final String fuente) {
        return fuente == null ? "" : fuente.strip().toUpperCase(Locale.ROOT);
    }
}
