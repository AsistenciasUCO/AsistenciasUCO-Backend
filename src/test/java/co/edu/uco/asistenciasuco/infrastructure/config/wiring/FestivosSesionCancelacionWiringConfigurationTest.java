package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.festivos.asegurarfestivos.usecase.impl.AsegurarFestivosPeriodoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.festivos.cargarfestivosautomatico.usecase.impl.CargarFestivosAutomaticoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.festivos.common.CargaAnualFestivosService;
import co.edu.uco.asistenciasuco.application.features.festivos.common.FestivosParametros;
import co.edu.uco.asistenciasuco.application.features.festivos.common.ObtenerFestivosConRespaldoService;
import co.edu.uco.asistenciasuco.application.features.festivos.consultarfestivos.usecase.impl.ConsultarFestivosAnioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.festivos.consultarsesionesfestivo.usecase.impl.ConsultarSesionesFestivoConAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.festivos.recargarfestivos.usecase.impl.RecargarFestivosAnioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesioncancelacion.cancelarsesion.usecase.impl.CancelarSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.DiaFestivoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.vault.SecretVaultPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler.CargaFestivosScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** CULM-001 B.1: composition roots de festivos y de la cancelacion de sesiones (HU047). */
class FestivosSesionCancelacionWiringConfigurationTest {

    @Test
    @SuppressWarnings("unchecked")
    void los_beans_de_festivos_se_construyen_con_sus_implementaciones() {
        final FestivosWiringConfiguration config = new FestivosWiringConfiguration();
        final ParameterCatalogPort catalogo = mock(ParameterCatalogPort.class);
        when(catalogo.getParameter(anyString(), anyString())).thenReturn(Optional.empty());
        final ObjectProvider<SecretVaultPort> vault = mock(ObjectProvider.class);
        final DiaFestivoRepositoryPort repositorio = mock(DiaFestivoRepositoryPort.class);

        final FestivosParametros parametros = config.festivosParametros(catalogo);
        final ObtenerFestivosConRespaldoService obtener = config.obtenerFestivosConRespaldoService(catalogo, vault, parametros);
        final CargaAnualFestivosService cargaAnual = config.cargaAnualFestivosService(obtener, repositorio);
        final var cargarAutomatico = config.cargarFestivosAutomaticoInputPort(parametros, cargaAnual);

        assertInstanceOf(ConsultarFestivosAnioUseCaseImpl.class, config.consultarFestivosAnioInputPort(parametros, repositorio));
        assertInstanceOf(RecargarFestivosAnioUseCaseImpl.class,
                config.recargarFestivosAnioInputPort(parametros, obtener, repositorio));
        assertInstanceOf(ConsultarSesionesFestivoConAsistenciaUseCaseImpl.class,
                config.consultarSesionesFestivoConAsistenciaInputPort(parametros, repositorio));
        assertInstanceOf(AsegurarFestivosPeriodoUseCaseImpl.class,
                config.asegurarFestivosPeriodoInputPort(parametros, repositorio, cargaAnual));
        assertInstanceOf(CargarFestivosAutomaticoUseCaseImpl.class, cargarAutomatico);
        final CargaFestivosScheduler scheduler =
                config.cargaFestivosScheduler(cargarAutomatico, Duration.ofMinutes(1), Duration.ofHours(24));
        assertFalse(scheduler.isRunning());
    }

    @Test
    void el_bean_de_cancelacion_se_construye_con_su_caso_de_uso() {
        assertInstanceOf(CancelarSesionUseCaseImpl.class, new SesionCancelacionWiringConfiguration().cancelarSesionInputPort(
                mock(InstitutionalScopePort.class), mock(SesionProcedenciaQueryPort.class),
                mock(SesionCancelacionRepositoryPort.class), mock(NotificacionPort.class), mock(RealtimePublisherPort.class)));
    }
}
