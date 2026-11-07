package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.NetworkClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.OperacionAutenticadaRateLimitFilter;
import co.edu.uco.asistenciasuco.infrastructure.ratelimit.SlidingWindowRateLimiter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.composite.ParametrosPorDefecto;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/**
 * CULM-001 A.2-4 (F-TRX-003): limite de intentos de la subida de archivos y de la asistencia por QR. El filtro va despues
 * de la cadena de Spring Security ({@code DEFAULT_FILTER_ORDER + 10}) para conocer al principal; los filtros publicos de
 * matricula y verificacion siguen antes de la seguridad.
 */
@Configuration(proxyBeanMethods = false)
public class RateLimitWiringConfiguration {

    static final String GRUPO_ARCHIVOS = "ARCHIVOS";
    static final String GRUPO_ASISTENCIA_QR = "ASISTENCIA_QR";

    @Bean
    public FilterRegistrationBean<OperacionAutenticadaRateLimitFilter> operacionAutenticadaRateLimitFilter(
            final ObjectProvider<ParameterCatalogPort> parameterCatalogPort,
            final NetworkClientIpResolver networkClientIpResolver,
            final SecurityErrorResponseWriter securityErrorResponseWriter,
            @Value("${app.rate-limit.operaciones.max-claves:10000}") final int maxClaves
    ) {
        final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(Duration.ofSeconds(60), maxClaves, Clock.systemUTC());
        final OperacionAutenticadaRateLimitFilter filtro = new OperacionAutenticadaRateLimitFilter(
                limiter,
                networkClientIpResolver,
                securityErrorResponseWriter,
                () -> limite(parameterCatalogPort.getObject(), GRUPO_ARCHIVOS, "LIMITE_SUBIDAS_POR_MINUTO"),
                () -> limite(parameterCatalogPort.getObject(), GRUPO_ASISTENCIA_QR, "LIMITE_IP_POR_MINUTO"),
                () -> limite(parameterCatalogPort.getObject(), GRUPO_ASISTENCIA_QR, "LIMITE_ESTUDIANTE_POR_MINUTO"));
        final FilterRegistrationBean<OperacionAutenticadaRateLimitFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.addUrlPatterns(OperacionAutenticadaRateLimitFilter.RUTA_SUBIR_ARCHIVO, OperacionAutenticadaRateLimitFilter.RUTA_ASISTENCIA_QR);
        registro.setOrder(SecurityFilterProperties.DEFAULT_FILTER_ORDER + 10);
        return registro;
    }

    /** Limite por minuto del catalogo; un valor ausente, no numerico o un catalogo caido usan {@link ParametrosPorDefecto}. */
    public static int limite(final ParameterCatalogPort parameterCatalogPort, final String grupo, final String clave) {
        final int porDefecto = ParametrosPorDefecto.entero(grupo, clave);
        try {
            return parameterCatalogPort.getParameter(grupo, clave)
                    .map(valor -> {
                        try {
                            return Integer.parseInt(valor.strip());
                        } catch (final NumberFormatException _) {
                            return porDefecto;
                        }
                    })
                    .orElse(porDefecto);
        } catch (final ParameterCatalogPort.ParameterCatalogException _) {
            return porDefecto;
        }
    }
}
