package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.matriculaqr.MatriculaQrAccesoPolicy;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.AbrirVentanaMatriculaInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.CerrarVentanaMatriculaInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.MatricularEstudianteAutenticadoInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.RegistrarEstudiantePorTokenInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.RotarTokensMatriculaAutomaticoInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.RotarVentanaMatriculaInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.ValidarTokenMatriculaInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.impl.AbrirVentanaMatriculaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.impl.CerrarVentanaMatriculaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.MatriculaQrCredencialesEmisor;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.MatriculaQrPresentador;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.impl.MatricularEstudianteAutenticadoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.impl.RegistrarEstudiantePorTokenUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.impl.RotarTokensMatriculaAutomaticoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.impl.RotarVentanaMatriculaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.impl.ValidarTokenMatriculaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.network.RedInstitucionalPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.qr.CodigoQrGeneratorPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.TokenGeneratorPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.MatriculaQrRateLimitFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.NetworkClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.SlidingWindowRateLimiter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr.MatriculaQrPublicaController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler.RotacionTokenMatriculaScheduler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.network.CidrRedInstitucionalAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.qr.ZxingCodigoQrGeneratorAdapter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.security.SecureRandomTokenGeneratorAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.time.Clock;
import java.time.Duration;

/**
 * Composition Root de la matricula por QR/PIN rotativo (HU176B). Cablea puertos, casos de uso, el filtro publico de
 * limite de intentos y el job de rotacion del servidor.
 */
@Configuration(proxyBeanMethods = false)
public class MatriculaQrWiringConfiguration {

    private static final int LIMITE_IP_POR_DEFECTO = 20;

    @Bean
    public TokenGeneratorPort tokenGeneratorPort() {
        return new SecureRandomTokenGeneratorAdapter();
    }

    @Bean
    public CodigoQrGeneratorPort codigoQrGeneratorPort() {
        return new ZxingCodigoQrGeneratorAdapter();
    }

    @Bean
    public RedInstitucionalPort redInstitucionalPort(final ParameterCatalogPort parameterCatalogPort) {
        return CidrRedInstitucionalAdapter.desdeCatalogo(parameterCatalogPort);
    }

    /**
     * El catalogo se resuelve de forma perezosa (por peticion): este bean lo usa el filtro, que se crea durante el arranque de
     * Tomcat, antes de que exista el EntityManager si el proveedor de parametros es {@code sqlserver}.
     */
    @Bean
    public NetworkClientIpResolver networkClientIpResolver(final ObjectProvider<ParameterCatalogPort> parameterCatalogPort) {
        return new NetworkClientIpResolver(() -> parameterCatalogPort.getObject().getParameter("RED", "PROXIES_CONFIABLES"));
    }

    @Bean
    public MatriculaQrAccesoPolicy matriculaQrAccesoPolicy(
            final ParameterCatalogPort parameterCatalogPort,
            final RedInstitucionalPort redInstitucionalPort
    ) {
        return new MatriculaQrAccesoPolicy(parameterCatalogPort, redInstitucionalPort);
    }

    @Bean
    public MatriculaQrCredencialesEmisor matriculaQrCredencialesEmisor(final TokenGeneratorPort tokenGeneratorPort) {
        return new MatriculaQrCredencialesEmisor(tokenGeneratorPort);
    }

    @Bean
    public MatriculaQrPresentador matriculaQrPresentador(
            final ParameterCatalogPort parameterCatalogPort,
            final CodigoQrGeneratorPort codigoQrGeneratorPort
    ) {
        return new MatriculaQrPresentador(parameterCatalogPort, codigoQrGeneratorPort);
    }

    @Bean
    public AbrirVentanaMatriculaInputPort abrirVentanaMatriculaInputPort(
            final MatriculaQrAccesoPolicy acceso,
            final InstitutionalScopePort scope,
            final MatriculaQrCredencialesEmisor emisor,
            final MatriculaQrPresentador presentador,
            final MatriculaQrRepositoryPort repositorio
    ) {
        return new AbrirVentanaMatriculaUseCaseImpl(acceso, scope, emisor, presentador, repositorio, Clock.systemUTC());
    }

    @Bean
    public RotarVentanaMatriculaInputPort rotarVentanaMatriculaInputPort(
            final MatriculaQrAccesoPolicy acceso,
            final InstitutionalScopePort scope,
            final MatriculaQrCredencialesEmisor emisor,
            final MatriculaQrPresentador presentador,
            final MatriculaQrRepositoryPort repositorio
    ) {
        return new RotarVentanaMatriculaUseCaseImpl(acceso, scope, emisor, presentador, repositorio);
    }

    @Bean
    public CerrarVentanaMatriculaInputPort cerrarVentanaMatriculaInputPort(
            final InstitutionalScopePort scope,
            final MatriculaQrRepositoryPort repositorio
    ) {
        return new CerrarVentanaMatriculaUseCaseImpl(scope, repositorio);
    }

    @Bean
    public ValidarTokenMatriculaInputPort validarTokenMatriculaInputPort(
            final MatriculaQrAccesoPolicy acceso,
            final MatriculaQrRepositoryPort repositorio
    ) {
        return new ValidarTokenMatriculaUseCaseImpl(acceso, repositorio);
    }

    @Bean
    public RegistrarEstudiantePorTokenInputPort registrarEstudiantePorTokenInputPort(
            final MatriculaQrAccesoPolicy acceso,
            final CorreoInstitucionalPolicy correoInstitucionalPolicy,
            final PasswordEncoderPort passwordEncoderPort,
            final MatriculaQrRepositoryPort repositorio,
            final IdentityProviderPort identityProviderPort,
            final NotificacionPort notificacionPort,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        return new RegistrarEstudiantePorTokenUseCaseImpl(
                acceso, correoInstitucionalPolicy, passwordEncoderPort, repositorio,
                identityProviderPort, notificacionPort, realtimePublisherPort);
    }

    @Bean
    public MatricularEstudianteAutenticadoInputPort matricularEstudianteAutenticadoInputPort(
            final MatriculaQrAccesoPolicy acceso,
            final MatriculaQrRepositoryPort repositorio,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        return new MatricularEstudianteAutenticadoUseCaseImpl(acceso, repositorio, realtimePublisherPort);
    }

    @Bean
    public RotarTokensMatriculaAutomaticoInputPort rotarTokensMatriculaAutomaticoInputPort(
            final MatriculaQrAccesoPolicy acceso,
            final MatriculaQrCredencialesEmisor emisor,
            final MatriculaQrPresentador presentador,
            final MatriculaQrRepositoryPort repositorio,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        return new RotarTokensMatriculaAutomaticoUseCaseImpl(acceso, emisor, presentador, repositorio, realtimePublisherPort);
    }

    @Bean
    public RotacionTokenMatriculaScheduler rotacionTokenMatriculaScheduler(
            final RotarTokensMatriculaAutomaticoInputPort rotarTokens,
            @Value("${app.matricula-qr.rotacion.tick:PT5S}") final Duration tick
    ) {
        return new RotacionTokenMatriculaScheduler(rotarTokens, tick);
    }

    /** Se registra antes de Spring Security para limitar tambien los intentos que terminan en 400 o 404. */
    @Bean
    public FilterRegistrationBean<MatriculaQrRateLimitFilter> matriculaQrRateLimitFilter(
            final ObjectProvider<ParameterCatalogPort> parameterCatalogPort,
            final NetworkClientIpResolver networkClientIpResolver,
            final SecurityErrorResponseWriter securityErrorResponseWriter,
            @Value("${app.matricula-qr.rate-limit.max-claves:10000}") final int maxClaves
    ) {
        final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(Duration.ofSeconds(60), maxClaves, Clock.systemUTC());
        final MatriculaQrRateLimitFilter filtro = new MatriculaQrRateLimitFilter(
                limiter,
                () -> limiteIp(parameterCatalogPort.getObject()),
                networkClientIpResolver,
                securityErrorResponseWriter);
        final FilterRegistrationBean<MatriculaQrRateLimitFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.addUrlPatterns(MatriculaQrPublicaController.RUTA_VALIDAR, MatriculaQrPublicaController.RUTA_REGISTRO);
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE + 11);
        return registro;
    }

    private static int limiteIp(final ParameterCatalogPort parameterCatalogPort) {
        return parameterCatalogPort.getParameter("MATRICULA_QR", "LIMITE_IP_POR_MINUTO")
                .map(valor -> {
                    try {
                        return Integer.parseInt(valor.strip());
                    } catch (final NumberFormatException _) {
                        return LIMITE_IP_POR_DEFECTO;
                    }
                })
                .orElse(LIMITE_IP_POR_DEFECTO);
    }
}
