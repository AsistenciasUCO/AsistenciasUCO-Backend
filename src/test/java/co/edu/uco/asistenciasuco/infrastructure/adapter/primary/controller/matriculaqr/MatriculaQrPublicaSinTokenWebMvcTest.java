package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr;

import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.ClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.CorrelationIdFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.MatriculaQrRateLimitFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.NetworkClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.RequestActorResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAccessDeniedHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAuthenticationEntryPoint;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.keycloak.KeycloakJwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.audit.contract.AuditEventPublisher;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityConfig;
import co.edu.uco.asistenciasuco.infrastructure.config.wiring.MatriculaQrWiringConfiguration;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CULM-001 A.2-5 (A2-B-08, N1): causa raiz del 401 en la ruta publica de matricula QR sin token. El catalogo de parametros
 * caido lanzaba {@code ParameterCatalogException} dentro de {@code MatriculaQrRateLimitFilter.limiteIp} (antes de la
 * cadena de seguridad) y el despacho de error a /error se respondia como no autenticado. Ahora el filtro usa el valor por
 * defecto, el despacho {@code ERROR} esta permitido y el cuerpo de error no expone traza.
 */
@WebMvcTest(controllers = MatriculaQrPublicaSinTokenWebMvcTest.ProbePublica.class)
@Import({
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        CorrelationIdFilter.class,
        ClientIpResolver.class,
        RequestActorResolver.class,
        MatriculaQrPublicaSinTokenWebMvcTest.ProbePublica.class,
        MatriculaQrPublicaSinTokenWebMvcTest.Soporte.class
})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class MatriculaQrPublicaSinTokenWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void la_ruta_publica_sin_token_con_el_catalogo_caido_no_responde_401() throws Exception {
        final int estado = mockMvc.perform(post(MatriculaQrPublicaController.RUTA_VALIDAR)
                        .contentType("application/json").content("{\"token\":\"000000\"}"))
                .andReturn().getResponse().getStatus();

        assertEquals(200, estado, "El filtro debe usar el limite por defecto y dejar pasar la peticion publica.");
    }

    @Test
    void el_despacho_error_de_un_fallo_en_filtro_muestra_su_estado_real_y_no_un_401_falso() throws Exception {
        mockMvc.perform(get("/error").with(request -> {
                    request.setDispatcherType(DispatcherType.ERROR);
                    return request;
                }))
                .andExpect(result -> assertTrue(result.getResponse().getStatus() != 401,
                        "El despacho ERROR no debe exigir autenticacion."));
        // Sin despacho de error, una ruta de negocio sin token sigue respondiendo 401.
        mockMvc.perform(get("/error")).andExpect(status().isUnauthorized());
    }

    @Test
    void los_limites_y_proxies_usan_el_valor_por_defecto_si_el_catalogo_falla() {
        final ParameterCatalogPort caido = mock(ParameterCatalogPort.class);
        when(caido.getParameter(anyString(), anyString())).thenThrow(new ParameterCatalogPort.ParameterCatalogException("401 de Azure"));
        final ParameterCatalogPort invalido = mock(ParameterCatalogPort.class);
        when(invalido.getParameter(anyString(), anyString())).thenReturn(Optional.of("veinte"));
        final ParameterCatalogPort configurado = mock(ParameterCatalogPort.class);
        when(configurado.getParameter(anyString(), anyString())).thenReturn(Optional.of(" 7 "));

        assertEquals(20, MatriculaQrWiringConfiguration.limiteIp(caido));
        assertEquals(20, MatriculaQrWiringConfiguration.limiteIp(invalido));
        assertEquals(7, MatriculaQrWiringConfiguration.limiteIp(configurado));
        assertEquals(Optional.of(""), MatriculaQrWiringConfiguration.proxiesConfiables(caido));
        assertEquals(Optional.of(" 7 "), MatriculaQrWiringConfiguration.proxiesConfiables(configurado));
    }

    @Test
    void el_cuerpo_del_despacho_de_error_no_expone_traza_ni_mensaje() throws Exception {
        final String yml = java.nio.file.Files.readString(java.nio.file.Path.of("src", "main", "resources", "application.yml"), StandardCharsets.UTF_8);

        assertTrue(yml.contains("include-stacktrace: never"));
        assertTrue(yml.contains("include-message: never"));
        assertTrue(yml.contains("include-exception: false"));
    }

    @RestController
    @RequestMapping
    static final class ProbePublica {

        @PostMapping(MatriculaQrPublicaController.RUTA_VALIDAR)
        String validar() {
            return "ok";
        }

        @org.springframework.web.bind.annotation.GetMapping("/error")
        String error() {
            return "error";
        }
    }

    @TestConfiguration
    static class Soporte {

        @Bean
        FilterRegistrationBean<MatriculaQrRateLimitFilter> matriculaQrRateLimitFilterDePrueba(final SecurityErrorResponseWriter writer) {
            final ParameterCatalogPort caido = mock(ParameterCatalogPort.class);
            when(caido.getParameter(anyString(), anyString())).thenThrow(new ParameterCatalogPort.ParameterCatalogException("401 de Azure"));
            @SuppressWarnings("unchecked")
            final ObjectProvider<ParameterCatalogPort> provider = mock(ObjectProvider.class);
            when(provider.getObject()).thenReturn(caido);
            return new MatriculaQrWiringConfiguration().matriculaQrRateLimitFilter(
                    provider, new NetworkClientIpResolver(() -> MatriculaQrWiringConfiguration.proxiesConfiables(caido)), writer, 100);
        }

        @Bean
        JwtClaimsExtractor jwtClaimsExtractor() {
            return new KeycloakJwtClaimsExtractor("asistencias-api", "idUsuario");
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new BadJwtException("Invalid test token.");
            };
        }

        @Bean
        ResolverMensajeUsuarioInputPort resolverMensajeUsuarioInputPort() {
            return codigo -> Optional.empty();
        }

        @Bean
        AuditEventPublisher auditEventPublisher() {
            return event -> {
            };
        }
    }
}
