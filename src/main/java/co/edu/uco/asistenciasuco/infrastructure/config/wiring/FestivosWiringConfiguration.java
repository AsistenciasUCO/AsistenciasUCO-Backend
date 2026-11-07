package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.festivos.asegurarfestivos.primaryports.AsegurarFestivosPeriodoInputPort;
import co.edu.uco.asistenciasuco.application.features.festivos.asegurarfestivos.usecase.impl.AsegurarFestivosPeriodoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.festivos.cargarfestivosautomatico.primaryports.CargarFestivosAutomaticoInputPort;
import co.edu.uco.asistenciasuco.application.features.festivos.cargarfestivosautomatico.usecase.impl.CargarFestivosAutomaticoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.festivos.common.CargaAnualFestivosService;
import co.edu.uco.asistenciasuco.application.features.festivos.common.FestivosParametros;
import co.edu.uco.asistenciasuco.application.features.festivos.common.ObtenerFestivosConRespaldoService;
import co.edu.uco.asistenciasuco.application.features.festivos.consultarfestivos.primaryports.ConsultarFestivosAnioInputPort;
import co.edu.uco.asistenciasuco.application.features.festivos.consultarfestivos.usecase.impl.ConsultarFestivosAnioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.festivos.consultarsesionesfestivo.primaryports.ConsultarSesionesFestivoConAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.festivos.consultarsesionesfestivo.usecase.impl.ConsultarSesionesFestivoConAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.festivos.recargarfestivos.primaryports.RecargarFestivosAnioInputPort;
import co.edu.uco.asistenciasuco.application.features.festivos.recargarfestivos.usecase.impl.RecargarFestivosAnioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler.CargaFestivosScheduler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.calendar.CalculoLocalFestivosColombiaAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.calendar.CalendariosNacionalesFestivosAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.calendar.FeriadosIoFestivosAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

/**
 * Composition Root de los dias festivos (CULM-001 B.1, DEC-017): los tres proveedores de calendario, el servicio de
 * obtencion con respaldo, los casos de uso del administrador y de la carga automatica, y el job diario del servidor.
 * Los proveedores se registran como objetos internos (no como beans del puerto) para que el orden lo decida
 * exclusivamente {@code FESTIVOS/PROVEEDOR_PRINCIPAL} + {@code FESTIVOS/ORDEN_RESPALDO}.
 */
@Configuration(proxyBeanMethods = false)
public class FestivosWiringConfiguration {

    /** Zona institucional para decidir el anio actual del job (la DB usa TIEMPO/ZONA_HORARIA_SQLSERVER para las fechas). */
    static final ZoneId ZONA_INSTITUCIONAL = ZoneId.of("America/Bogota");

    @Bean
    public FestivosParametros festivosParametros(final ParameterCatalogPort parameterCatalogPort) {
        return new FestivosParametros(parameterCatalogPort);
    }

    @Bean
    public ObtenerFestivosConRespaldoService obtenerFestivosConRespaldoService(
            final ParameterCatalogPort parameterCatalogPort,
            final ObjectProvider<SecretVaultPort> secretVaultPort,
            final FestivosParametros festivosParametros
    ) {
        final HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        return new ObtenerFestivosConRespaldoService(List.of(
                new CalendariosNacionalesFestivosAdapter(httpClient, parameterCatalogPort),
                new FeriadosIoFestivosAdapter(httpClient, parameterCatalogPort, secretVaultPort.getIfAvailable()),
                new CalculoLocalFestivosColombiaAdapter()
        ), festivosParametros);
    }

    @Bean
    public CargaAnualFestivosService cargaAnualFestivosService(
            final ObtenerFestivosConRespaldoService obtenerFestivosConRespaldoService,
            final DiaFestivoRepositoryPort diaFestivoRepositoryPort
    ) {
        return new CargaAnualFestivosService(obtenerFestivosConRespaldoService, diaFestivoRepositoryPort);
    }

    @Bean
    public ConsultarFestivosAnioInputPort consultarFestivosAnioInputPort(
            final FestivosParametros festivosParametros,
            final DiaFestivoRepositoryPort diaFestivoRepositoryPort
    ) {
        return new ConsultarFestivosAnioUseCaseImpl(festivosParametros, diaFestivoRepositoryPort);
    }

    @Bean
    public RecargarFestivosAnioInputPort recargarFestivosAnioInputPort(
            final FestivosParametros festivosParametros,
            final ObtenerFestivosConRespaldoService obtenerFestivosConRespaldoService,
            final DiaFestivoRepositoryPort diaFestivoRepositoryPort
    ) {
        return new RecargarFestivosAnioUseCaseImpl(festivosParametros, obtenerFestivosConRespaldoService,
                diaFestivoRepositoryPort);
    }

    @Bean
    public ConsultarSesionesFestivoConAsistenciaInputPort consultarSesionesFestivoConAsistenciaInputPort(
            final FestivosParametros festivosParametros,
            final DiaFestivoRepositoryPort diaFestivoRepositoryPort
    ) {
        return new ConsultarSesionesFestivoConAsistenciaUseCaseImpl(festivosParametros, diaFestivoRepositoryPort);
    }

    @Bean
    public AsegurarFestivosPeriodoInputPort asegurarFestivosPeriodoInputPort(
            final FestivosParametros festivosParametros,
            final DiaFestivoRepositoryPort diaFestivoRepositoryPort,
            final CargaAnualFestivosService cargaAnualFestivosService
    ) {
        return new AsegurarFestivosPeriodoUseCaseImpl(festivosParametros, diaFestivoRepositoryPort, cargaAnualFestivosService);
    }

    @Bean
    public CargarFestivosAutomaticoInputPort cargarFestivosAutomaticoInputPort(
            final FestivosParametros festivosParametros,
            final CargaAnualFestivosService cargaAnualFestivosService
    ) {
        return new CargarFestivosAutomaticoUseCaseImpl(festivosParametros, cargaAnualFestivosService,
                Clock.system(ZONA_INSTITUCIONAL));
    }

    @Bean
    public CargaFestivosScheduler cargaFestivosScheduler(
            final CargarFestivosAutomaticoInputPort cargarFestivosAutomaticoInputPort,
            @Value("${app.festivos.carga.retardo-inicial:PT1M}") final Duration retardoInicial,
            @Value("${app.festivos.carga.tick:PT24H}") final Duration tick
    ) {
        return new CargaFestivosScheduler(cargarFestivosAutomaticoInputPort, retardoInicial, tick);
    }
}
