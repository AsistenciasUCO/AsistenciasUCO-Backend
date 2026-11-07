package co.edu.uco.asistenciasuco.application.features.festivos.recargarfestivos.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.festivos.common.FestivosParametros;
import co.edu.uco.asistenciasuco.application.features.festivos.common.ObtenerFestivosConRespaldoService;
import co.edu.uco.asistenciasuco.application.features.festivos.common.ResultadoObtencionFestivos;
import co.edu.uco.asistenciasuco.application.features.festivos.recargarfestivos.usecase.RecargarFestivosAnioUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort;

import java.util.Objects;

/**
 * CULM-001 B.1 (§3.2.2): obtener con respaldo, {@code usp_cargar_dias_festivos} (RBAC ADMINISTRADOR en la DB) y
 * {@code usp_aplicar_festivos_periodo(@anio)} (solo grupos de la institucion del administrador, solo sesiones futuras
 * sin asistencia). El anio se valida aqui (400 {@code FES_001}) antes de llamar a cualquier proveedor.
 */
public final class RecargarFestivosAnioUseCaseImpl implements RecargarFestivosAnioUseCase {

    private final FestivosParametros parametros;
    private final ObtenerFestivosConRespaldoService obtenerFestivos;
    private final DiaFestivoRepositoryPort repositorio;

    public RecargarFestivosAnioUseCaseImpl(final FestivosParametros parametros,
                                           final ObtenerFestivosConRespaldoService obtenerFestivos,
                                           final DiaFestivoRepositoryPort repositorio) {
        this.parametros = Objects.requireNonNull(parametros, "FestivosParametros es obligatorio.");
        this.obtenerFestivos = Objects.requireNonNull(obtenerFestivos, "ObtenerFestivosConRespaldoService es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "DiaFestivoRepositoryPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para recargar festivos es obligatorio.");
        parametros.exigirAnioValido(comando.anio());
        final ResultadoObtencionFestivos obtenidos = obtenerFestivos.obtener(comando.anio());
        final DiaFestivoRepositoryPort.CargaFestivos carga = repositorio.cargar(comando.anio(), obtenidos.festivos(),
                obtenidos.fuente(), obtenidos.diferenciasCalculoLocal(), comando.usuarioEjecutor());
        final DiaFestivoRepositoryPort.AplicacionFestivos aplicacion =
                repositorio.aplicarAnio(comando.anio(), comando.usuarioEjecutor());
        return new Resultado(comando.anio(), obtenidos.fuente(), obtenidos.usoRespaldo(), carga.totalFestivos(),
                carga.festivosInsertados(), carga.festivosActualizados(), carga.festivosDesactivados(),
                obtenidos.diferenciasCalculoLocal(),
                new Aplicacion(aplicacion.gruposProcesados(), aplicacion.sesionesCanceladas(),
                        aplicacion.sesionesConAsistencia()));
    }
}
