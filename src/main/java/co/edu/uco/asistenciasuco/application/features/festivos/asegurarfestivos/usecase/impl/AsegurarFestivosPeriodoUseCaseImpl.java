package co.edu.uco.asistenciasuco.application.features.festivos.asegurarfestivos.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.festivos.asegurarfestivos.usecase.AsegurarFestivosPeriodoUseCase;
import co.edu.uco.asistenciasuco.application.features.festivos.common.CargaAnualFestivosService;
import co.edu.uco.asistenciasuco.application.features.festivos.common.FestivosParametros;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort.RangoFechas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * CULM-001 B.1 (§3.2.3): carga los anios del periodo ({@code fechaInicio..fechaFin}) que aun no tienen festivos. Si un
 * proveedor falla se usa el respaldo (lo decide {@code ObtenerFestivosConRespaldoService}); cualquier otro fallo se
 * registra en el log y no se propaga: la creacion del grupo o la generacion de sesiones siguen su curso.
 */
public final class AsegurarFestivosPeriodoUseCaseImpl implements AsegurarFestivosPeriodoUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsegurarFestivosPeriodoUseCaseImpl.class);

    private final FestivosParametros parametros;
    private final DiaFestivoRepositoryPort repositorio;
    private final CargaAnualFestivosService cargaAnual;

    public AsegurarFestivosPeriodoUseCaseImpl(final FestivosParametros parametros,
                                              final DiaFestivoRepositoryPort repositorio,
                                              final CargaAnualFestivosService cargaAnual) {
        this.parametros = Objects.requireNonNull(parametros, "FestivosParametros es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "DiaFestivoRepositoryPort es obligatorio.");
        this.cargaAnual = Objects.requireNonNull(cargaAnual, "CargaAnualFestivosService es obligatorio.");
    }

    @Override
    public void execute(final Comando comando) {
        if (comando == null || !parametros.habilitado()) {
            return;
        }
        final Optional<RangoFechas> rango;
        try {
            rango = resolverRango(comando);
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible consultar el periodo para asegurar sus festivos: {}", exception.getClass().getSimpleName());
            return;
        }
        rango.filter(r -> r.fechaInicio() != null && r.fechaFin() != null && !r.fechaFin().isBefore(r.fechaInicio()))
                .ifPresent(this::asegurar);
    }

    private Optional<RangoFechas> resolverRango(final Comando comando) {
        if (comando.periodoAcademicoId() != null) {
            return repositorio.consultarRangoPeriodo(comando.periodoAcademicoId());
        }
        if (comando.grupoId() != null) {
            return repositorio.consultarRangoPeriodoDeGrupo(comando.grupoId());
        }
        return Optional.empty();
    }

    private void asegurar(final RangoFechas rango) {
        for (int anio = rango.fechaInicio().getYear(); anio <= rango.fechaFin().getYear(); anio++) {
            if (!parametros.anioValido(anio)) {
                continue;
            }
            try {
                cargaAnual.cargarSiFalta(anio);
            } catch (final RuntimeException exception) {
                LOGGER.warn("No fue posible asegurar los festivos del anio {}: {}", anio, exception.getClass().getSimpleName());
            }
        }
    }
}
