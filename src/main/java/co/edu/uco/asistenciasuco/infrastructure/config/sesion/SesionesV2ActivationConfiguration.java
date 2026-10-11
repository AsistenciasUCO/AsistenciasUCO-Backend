package co.edu.uco.asistenciasuco.infrastructure.config.sesion;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2.SesionesV2DisabledInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Puerta OFF de {@code /api/v2/sesiones/**}: activa por defecto y retirada solo con
 * {@code app.sesiones.v2.enabled=true}, que a su vez activa la verificacion del contrato DB
 * ({@code SesionV2SchemaCompatibilityVerifier}).
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = SesionesV2Activation.PREFIX,
        name = SesionesV2Activation.NAME,
        havingValue = "false",
        matchIfMissing = true
)
public class SesionesV2ActivationConfiguration implements WebMvcConfigurer {

    static final String[] RUTAS_V2 = {"/api/v2/sesiones", "/api/v2/sesiones/**"};

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(new SesionesV2DisabledInterceptor()).addPathPatterns(RUTAS_V2);
    }
}
