package co.edu.uco.asistenciasuco.application.features.festivos.cargarfestivosautomatico.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.festivos.cargarfestivosautomatico.usecase.CargarFestivosAutomaticoUseCase;
import co.edu.uco.asistenciasuco.application.features.festivos.common.CargaAnualFestivosService;
import co.edu.uco.asistenciasuco.application.features.festivos.common.FestivosParametros;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * CULM-001 B.1 (§3.4.5): para el anio actual y los {@code FESTIVOS/ANIOS_ADELANTE} siguientes, si el anio no tiene
 * festivos activos los obtiene con respaldo, los carga y los aplica. Respeta el interruptor {@code FESTIVOS/HABILITADO}.
 * Un anio que falla se registra y no impide intentar el siguiente.
 */
public final class CargarFestivosAutomaticoUseCaseImpl implements CargarFestivosAutomaticoUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(CargarFestivosAutomaticoUseCaseImpl.class);

    private final FestivosParametros parametros;
    private final CargaAnualFestivosService cargaAnual;
    private final Clock clock;

    public CargarFestivosAutomaticoUseCaseImpl(final FestivosParametros parametros,
                                               final CargaAnualFestivosService cargaAnual,
                                               final Clock clock) {
        this.parametros = Objects.requireNonNull(parametros, "FestivosParametros es obligatorio.");
        this.cargaAnual = Objects.requireNonNull(cargaAnual, "CargaAnualFestivosService es obligatorio.");
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
    }

    @Override
    public int execute() {
        if (!parametros.habilitado()) {
            return 0;
        }
        final int anioActual = LocalDate.now(clock).getYear();
        final int aniosAdelante = Math.max(0, parametros.entero(FestivosParametros.ANIOS_ADELANTE,
                FestivosParametros.DEFECTO_ANIOS_ADELANTE));
        int cargados = 0;
        for (int anio = anioActual; anio <= anioActual + aniosAdelante; anio++) {
            try {
                if (cargaAnual.cargarSiFalta(anio)) {
                    cargados++;
                }
            } catch (final RuntimeException exception) {
                LOGGER.error("No fue posible cargar los festivos del anio {}: {}", anio, exception.getClass().getSimpleName());
            }
        }
        return cargados;
    }
}
