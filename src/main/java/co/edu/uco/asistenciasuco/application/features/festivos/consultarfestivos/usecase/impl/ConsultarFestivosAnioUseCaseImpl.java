package co.edu.uco.asistenciasuco.application.features.festivos.consultarfestivos.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.festivos.common.FestivosParametros;
import co.edu.uco.asistenciasuco.application.features.festivos.consultarfestivos.usecase.ConsultarFestivosAnioUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort;

import java.util.Objects;

/**
 * CULM-001 B.1 (§3.2.2): {@code usp_consultar_dias_festivos} (RBAC ADMINISTRADOR en la DB). Un anio tiene a lo sumo
 * {@code FESTIVOS/MAXIMO_POR_ANIO} festivos activos, asi que se pide una sola pagina amplia.
 */
public final class ConsultarFestivosAnioUseCaseImpl implements ConsultarFestivosAnioUseCase {

    static final int TAMANO_PAGINA = 100;

    private final FestivosParametros parametros;
    private final DiaFestivoRepositoryPort repositorio;

    public ConsultarFestivosAnioUseCaseImpl(final FestivosParametros parametros, final DiaFestivoRepositoryPort repositorio) {
        this.parametros = Objects.requireNonNull(parametros, "FestivosParametros es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "DiaFestivoRepositoryPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para consultar festivos es obligatorio.");
        parametros.exigirAnioValido(comando.anio());
        final DiaFestivoRepositoryPort.PaginaFestivos pagina = repositorio.consultarFestivos(
                comando.anio(), comando.incluirInactivos(), 1, TAMANO_PAGINA, comando.usuarioEjecutor());
        return new Resultado(
                comando.anio(),
                pagina.fuenteUltimaCarga(),
                pagina.fechaUltimaCargaUtc(),
                pagina.diferenciasCalculoLocal(),
                parametros.texto(FestivosParametros.ATRIBUCION_TEXTO, FestivosParametros.DEFECTO_ATRIBUCION_TEXTO),
                parametros.texto(FestivosParametros.ATRIBUCION_URL, FestivosParametros.DEFECTO_ATRIBUCION_URL),
                pagina.festivos().stream()
                        .map(f -> new Festivo(f.fecha(), f.nombre(), f.fuente(), f.estaActivo()))
                        .toList(),
                pagina.totalRegistros());
    }
}
