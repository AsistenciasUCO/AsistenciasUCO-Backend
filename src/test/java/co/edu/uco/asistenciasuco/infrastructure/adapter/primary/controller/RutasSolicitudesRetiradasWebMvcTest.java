package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller;

import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.GlobalExceptionHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.ClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.CorrelationIdFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.RequestActorResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAccessDeniedHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAuthenticationEntryPoint;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.SecurityContextAuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.keycloak.KeycloakJwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.audit.contract.AuditEventPublisher;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CULM-001 A.1 (B-RED-05): las 10 rutas de solicitudes de ingreso y de matricula del coordinador quedan sin handler y
 * responden 404 con {@code ApiErrorResponse}, aun con el token del rol que hoy tiene acceso. Usa los controladores
 * reales, filtrados por nombre (no por clase) para que la prueba compile despues de borrar los dos controladores de
 * solicitudes; sus dependencias se simulan sin nombrar tipos que se borran (N8).
 */
@WebMvcTest(useDefaultFilters = false, includeFilters = @ComponentScan.Filter(type = FilterType.REGEX,
        pattern = "co\\.edu\\.uco\\.asistenciasuco\\.infrastructure\\.adapter\\.primary\\.controller\\..*\\."
                + "(EstudianteSolicitudIngreso|DocenteSolicitudIngreso|CoordinadorPortal|Grupo)Controller"))
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        CorrelationIdFilter.class,
        ClientIpResolver.class,
        RequestActorResolver.class,
        SecurityContextAuthenticatedUserResolver.class,
        RutasSolicitudesRetiradasWebMvcTest.SoporteSeguridad.class
})
@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class RutasSolicitudesRetiradasWebMvcTest {

    private static final String AUDIENCE = "asistencias-api";
    private static final String ID = "11111111-1111-1111-1111-111111111111";
    private static final String OTRO_ID = "33333333-3333-3333-3333-333333333333";
    private static final List<String> ROLES = List.of("COORDINADOR", "ADMINISTRADOR", "DOCENTE", "ESTUDIANTE", "DECANO");

    private record Ruta(HttpMethod metodo, String path, String rol) {
    }

    private static final List<Ruta> RUTAS_RETIRADAS = List.of(
            new Ruta(HttpMethod.POST, "/api/v1/estudiante/solicitudes-matricula", "ESTUDIANTE"),
            new Ruta(HttpMethod.GET, "/api/v1/estudiante/solicitudes-matricula", "ESTUDIANTE"),
            new Ruta(HttpMethod.POST, "/api/v1/estudiante/solicitudes-matricula/" + ID + "/retiro", "ESTUDIANTE"),
            new Ruta(HttpMethod.GET, "/api/v1/docente/grupos/" + ID + "/solicitudes-ingreso", "DOCENTE"),
            new Ruta(HttpMethod.PATCH, "/api/v1/docente/solicitudes-ingreso/" + ID, "DOCENTE"),
            new Ruta(HttpMethod.GET, "/api/v1/coordinador/solicitudes-matricula", "COORDINADOR"),
            new Ruta(HttpMethod.PATCH, "/api/v1/coordinador/solicitudes-matricula/" + ID, "COORDINADOR"),
            new Ruta(HttpMethod.POST, "/api/v1/coordinador/grupos/" + ID + "/estudiantes", "COORDINADOR"),
            new Ruta(HttpMethod.DELETE, "/api/v1/coordinador/grupos/" + ID + "/estudiantes/" + OTRO_ID, "COORDINADOR"),
            new Ruta(HttpMethod.DELETE, "/api/v1/grupos/" + ID + "/estudiantes/" + OTRO_ID, "COORDINADOR"));

    @Autowired
    private MockMvc mockMvc;

    @Test
    void las_rutas_de_solicitudes_retiradas_responden_404_con_api_error_response() throws Exception {
        for (final Ruta ruta : RUTAS_RETIRADAS) {
            mockMvc.perform(MockMvcRequestBuilders.request(ruta.metodo(), ruta.path())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + ruta.rol())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"grupoId\":\"" + ID + "\",\"decision\":\"APROBAR\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.path").value(ruta.path()));
        }
    }

    /**
     * Registra un mock (RETURNS_DEFAULTS) por cada parametro de constructor de los {@code @RestController} incluidos
     * que no tenga bean. Asi la prueba no nombra tipos de produccion que A.1 borra.
     */
    static final class MocksParaControladoresIncluidosPostProcessor implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) {
            for (final String nombre : beanFactory.getBeanDefinitionNames()) {
                final BeanDefinition definicion = beanFactory.getBeanDefinition(nombre);
                final Optional<Class<?>> clase = cargar(definicion.getBeanClassName(), beanFactory.getBeanClassLoader());
                if (clase.isEmpty() || !clase.get().isAnnotationPresent(RestController.class)) {
                    continue;
                }
                for (final Constructor<?> constructor : clase.get().getDeclaredConstructors()) {
                    for (final Class<?> tipo : constructor.getParameterTypes()) {
                        if (beanFactory.getBeanNamesForType(tipo, true, false).length == 0) {
                            beanFactory.registerSingleton("mock#" + tipo.getName(), Mockito.mock(tipo));
                        }
                    }
                }
            }
        }

        private static Optional<Class<?>> cargar(final String nombreClase, final ClassLoader classLoader) {
            if (nombreClase == null) {
                return Optional.empty();
            }
            try {
                return Optional.of(ClassUtils.forName(nombreClase, classLoader));
            } catch (ClassNotFoundException | LinkageError error) {
                return Optional.empty();
            }
        }
    }

    @TestConfiguration
    static class SoporteSeguridad {

        @Bean
        static BeanFactoryPostProcessor mocksParaControladoresIncluidosPostProcessor() {
            return new MocksParaControladoresIncluidosPostProcessor();
        }

        @Bean
        JwtClaimsExtractor jwtClaimsExtractor() {
            return new KeycloakJwtClaimsExtractor(AUDIENCE, "idUsuario");
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                if (!ROLES.contains(token)) {
                    throw new BadJwtException("Invalid test token.");
                }
                final Instant now = Instant.now();
                return Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .issuer("http://127.0.0.1:8081/realms/asistencias-uco")
                        .subject("keycloak-sub")
                        .issuedAt(now.minusSeconds(60))
                        .expiresAt(now.plusSeconds(300))
                        .audience(List.of(AUDIENCE))
                        .claim("idUsuario", "22222222-2222-2222-2222-222222222222")
                        .claim("resource_access", Map.of(AUDIENCE, Map.of("roles", List.of(token))))
                        .build();
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
