package co.edu.uco.asistenciasuco.application.features.festivos.consultarsesionesfestivo.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.festivos.common.FestivosParametros;
import co.edu.uco.asistenciasuco.application.features.festivos.consultarsesionesfestivo.usecase.ConsultarSesionesFestivoConAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort;

import java.util.Objects;

/**
 * CULM-001 B.1 (§3.2.2): {@code usp_consultar_sesiones_festivo_con_asistencia_paginado}. Aqui solo se completan los
 * valores por defecto de la pagina; los limites de pagina ({@code PAG_001}) y el ambito los valida la DB.
 */
public final class ConsultarSesionesFestivoConAsistenciaUseCaseImpl implements ConsultarSesionesFestivoConAsistenciaUseCase {

    private final FestivosParametros parametros;
    private final DiaFestivoRepositoryPort repositorio;

    public ConsultarSesionesFestivoConAsistenciaUseCaseImpl(final FestivosParametros parametros,
                                                            final DiaFestivoRepositoryPort repositorio) {
        this.parametros = Objects.requireNonNull(parametros, "FestivosParametros es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "DiaFestivoRepositoryPort es obligatorio.");
    }

    @Override
    public Resultado execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para consultar sesiones en festivo es obligatorio.");
        parametros.exigirAnioValido(comando.anio());
        final int pagina = comando.numeroPagina() == null ? PAGINA_POR_DEFECTO : comando.numeroPagina();
        final int tamano = comando.tamanoPagina() == null ? TAMANO_POR_DEFECTO : comando.tamanoPagina();
        final DiaFestivoRepositoryPort.PaginaSesionesFestivo resultado =
                repositorio.consultarSesionesConAsistencia(comando.anio(), pagina, tamano, comando.usuarioEjecutor());
        return new Resultado(
                resultado.elementos().stream()
                        .map(s -> new SesionFestivo(s.idSesion(), s.idGrupo(), s.nombreGrupo(), s.nombreAsignatura(),
                                s.nombreDocente(), s.fechaHoraInicioUtc(), s.fecha(), s.nombreFestivo(), s.totalTomas(),
                                s.totalRegistros()))
                        .toList(),
                pagina, tamano, resultado.totalRegistros());
    }
}
