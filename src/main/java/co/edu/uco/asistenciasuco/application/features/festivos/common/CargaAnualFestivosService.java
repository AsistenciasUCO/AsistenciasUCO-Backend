package co.edu.uco.asistenciasuco.application.features.festivos.common;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * CULM-001 B.1: carga un anio de festivos solo si aun no tiene festivos activos (el job y la creacion de grupos no
 * recargan anios ya cargados: eso es accion del administrador y respeta el uso razonable del proveedor). Tras cada
 * carga se aplican los festivos a los grupos que ya tienen sesiones en ese anio ([SUPUESTO-B-21]).
 */
public final class CargaAnualFestivosService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CargaAnualFestivosService.class);

    private final ObtenerFestivosConRespaldoService obtenerFestivos;
    private final DiaFestivoRepositoryPort repositorio;

    public CargaAnualFestivosService(final ObtenerFestivosConRespaldoService obtenerFestivos,
                                     final DiaFestivoRepositoryPort repositorio) {
        this.obtenerFestivos = Objects.requireNonNull(obtenerFestivos, "ObtenerFestivosConRespaldoService es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "DiaFestivoRepositoryPort es obligatorio.");
    }

    /** Devuelve {@code true} si el anio no tenia festivos y se cargo; {@code false} si ya estaba cargado. */
    public boolean cargarSiFalta(final int anio) {
        if (repositorio.contarFestivosActivos(anio) > 0) {
            return false;
        }
        final ResultadoObtencionFestivos resultado = obtenerFestivos.obtener(anio);
        final DiaFestivoRepositoryPort.CargaFestivos carga = repositorio.cargarAutomatico(
                anio, resultado.festivos(), resultado.fuente(), resultado.diferenciasCalculoLocal());
        final DiaFestivoRepositoryPort.AplicacionFestivos aplicacion = repositorio.aplicarAutomatico(anio);
        LOGGER.info("Festivos cargados automaticamente. anio={}, fuente={}, usoRespaldo={}, total={}, gruposProcesados={}, "
                        + "sesionesCanceladas={}, sesionesConAsistencia={}", anio, resultado.fuente(), resultado.usoRespaldo(),
                carga.totalFestivos(), aplicacion.gruposProcesados(), aplicacion.sesionesCanceladas(),
                aplicacion.sesionesConAsistencia());
        return true;
    }
}
