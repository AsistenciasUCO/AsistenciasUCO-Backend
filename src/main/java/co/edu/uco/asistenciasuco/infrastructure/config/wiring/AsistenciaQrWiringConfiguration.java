package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.asistenciaqr.abrirqrtoma.primaryports.AbrirQrTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.abrirqrtoma.usecase.impl.AbrirQrTomaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.cerrarqrtoma.primaryports.CerrarQrTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.cerrarqrtoma.usecase.impl.CerrarQrTomaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrAccesoPolicy;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrCredencialesEmisor;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrEventos;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrPresentador;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.EsperaReintento;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.registrarasistenciaportoken.primaryports.RegistrarAsistenciaPorTokenInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.registrarasistenciaportoken.usecase.impl.RegistrarAsistenciaPorTokenUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotarqrtoma.primaryports.RotarQrTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotarqrtoma.usecase.impl.RotarQrTomaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotartokensasistenciaautomatico.primaryports.RotarTokensAsistenciaAutomaticoInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotartokensasistenciaautomatico.usecase.impl.RotarTokensAsistenciaAutomaticoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartomasvencidas.primaryports.CerrarTomasVencidasInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaAcceso;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaEventos;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.network.RedInstitucionalPort;
import co.edu.uco.asistenciasuco.application.secondaryports.qr.CodigoQrGeneratorPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaQrRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.LimitadorIntentosPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.TokenGeneratorPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.AsistenciaQrRateLimitFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.NetworkClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler.RotacionTokenAsistenciaScheduler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security.SlidingWindowLimitadorIntentosAdapter;
import co.edu.uco.asistenciasuco.infrastructure.ratelimit.SlidingWindowRateLimiter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.time.Clock;
import java.time.Duration;

/**
 * Composition Root del QR/PIN rotativo de asistencia (CULM-001 B.2, HU175, HU176): casos de uso, limitador de intentos del
 * caso de uso, filtro de IP (solo rechazos) y job de rotacion y cierre automatico. Reutiliza los puertos de generacion de
 * token, QR y red del campus que cablea {@link MatriculaQrWiringConfiguration}.
 */
@Configuration(proxyBeanMethods = false)
public class AsistenciaQrWiringConfiguration {

    @Bean
    public AsistenciaQrAccesoPolicy asistenciaQrAccesoPolicy(final ParameterCatalogPort parameterCatalogPort,
                                                             final RedInstitucionalPort redInstitucionalPort) {
        return new AsistenciaQrAccesoPolicy(parameterCatalogPort, redInstitucionalPort);
    }

    @Bean
    public AsistenciaQrCredencialesEmisor asistenciaQrCredencialesEmisor(final TokenGeneratorPort tokenGeneratorPort) {
        return new AsistenciaQrCredencialesEmisor(tokenGeneratorPort);
    }

    @Bean
    public AsistenciaQrPresentador asistenciaQrPresentador(final ParameterCatalogPort parameterCatalogPort,
                                                           final CodigoQrGeneratorPort codigoQrGeneratorPort) {
        return new AsistenciaQrPresentador(parameterCatalogPort, codigoQrGeneratorPort);
    }

    @Bean
    public AsistenciaQrEventos asistenciaQrEventos(final TomaAsistenciaEventos tomaAsistenciaEventos) {
        return new AsistenciaQrEventos(tomaAsistenciaEventos);
    }

    @Bean
    public LimitadorIntentosPort limitadorIntentosPort(
            @Value("${app.asistencia-qr.rate-limit.max-claves:10000}") final int maxClaves
    ) {
        return new SlidingWindowLimitadorIntentosAdapter(new SlidingWindowRateLimiter(Duration.ofSeconds(60), maxClaves, Clock.systemUTC()));
    }

    @Bean
    public AbrirQrTomaInputPort abrirQrTomaInputPort(final AsistenciaQrAccesoPolicy acceso, final TomaAsistenciaAcceso tomas,
                                                     final AsistenciaQrCredencialesEmisor emisor, final AsistenciaQrPresentador presentador,
                                                     final AsistenciaQrRepositoryPort repositorio, final AsistenciaQrEventos eventos) {
        return new AbrirQrTomaUseCaseImpl(acceso, tomas, emisor, presentador, repositorio, eventos);
    }

    @Bean
    public RotarQrTomaInputPort rotarQrTomaInputPort(final AsistenciaQrAccesoPolicy acceso, final TomaAsistenciaAcceso tomas,
                                                     final AsistenciaQrCredencialesEmisor emisor, final AsistenciaQrPresentador presentador,
                                                     final AsistenciaQrRepositoryPort repositorio, final AsistenciaQrEventos eventos) {
        return new RotarQrTomaUseCaseImpl(acceso, tomas, emisor, presentador, repositorio, eventos);
    }

    @Bean
    public CerrarQrTomaInputPort cerrarQrTomaInputPort(final TomaAsistenciaAcceso tomas, final AsistenciaQrRepositoryPort repositorio) {
        return new CerrarQrTomaUseCaseImpl(tomas, repositorio);
    }

    @Bean
    public RotarTokensAsistenciaAutomaticoInputPort rotarTokensAsistenciaAutomaticoInputPort(
            final AsistenciaQrAccesoPolicy acceso, final AsistenciaQrCredencialesEmisor emisor, final AsistenciaQrPresentador presentador,
            final AsistenciaQrRepositoryPort repositorio, final TomaAsistenciaRepositoryPort tomas, final AsistenciaQrEventos eventos) {
        return new RotarTokensAsistenciaAutomaticoUseCaseImpl(acceso, emisor, presentador, repositorio, tomas, eventos);
    }

    @Bean
    public RegistrarAsistenciaPorTokenInputPort registrarAsistenciaPorTokenInputPort(
            final AsistenciaQrAccesoPolicy acceso, final LimitadorIntentosPort limitador, final AsistenciaQrRepositoryPort repositorio,
            final TomaAsistenciaRepositoryPort tomas, final InstitutionalScopePort scope, final AsistenciaQrEventos eventos) {
        return new RegistrarAsistenciaPorTokenUseCaseImpl(acceso, limitador, repositorio, tomas, scope, eventos, EsperaReintento.delHilo());
    }

    @Bean
    public RotacionTokenAsistenciaScheduler rotacionTokenAsistenciaScheduler(
            final RotarTokensAsistenciaAutomaticoInputPort rotarTokens,
            final CerrarTomasVencidasInputPort cerrarVencidas,
            @Value("${app.asistencia-qr.rotacion.tick:PT5S}") final Duration tick
    ) {
        return new RotacionTokenAsistenciaScheduler(rotarTokens, cerrarVencidas, tick);
    }

    /**
     * Antes de Spring Security, para contar tambien los rechazos 401/403. El limite se lee por peticion con valor por
     * defecto ({@code ASISTENCIA_QR/LIMITE_FALLIDOS_IP_POR_MINUTO} = 600) si el catalogo falla.
     */
    @Bean
    public FilterRegistrationBean<AsistenciaQrRateLimitFilter> asistenciaQrRateLimitFilter(
            final ObjectProvider<ParameterCatalogPort> parameterCatalogPort,
            final NetworkClientIpResolver networkClientIpResolver,
            final SecurityErrorResponseWriter securityErrorResponseWriter,
            @Value("${app.asistencia-qr.rate-limit.max-claves:10000}") final int maxClaves
    ) {
        final AsistenciaQrRateLimitFilter filtro = new AsistenciaQrRateLimitFilter(
                new SlidingWindowRateLimiter(Duration.ofSeconds(60), maxClaves, Clock.systemUTC()),
                () -> RateLimitWiringConfiguration.limite(parameterCatalogPort.getObject(), "ASISTENCIA_QR", "LIMITE_FALLIDOS_IP_POR_MINUTO"),
                networkClientIpResolver,
                securityErrorResponseWriter);
        final FilterRegistrationBean<AsistenciaQrRateLimitFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.addUrlPatterns(AsistenciaQrRateLimitFilter.RUTA);
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE + 12);
        return registro;
    }
}
