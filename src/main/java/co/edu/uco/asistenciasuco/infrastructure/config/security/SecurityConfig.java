package co.edu.uco.asistenciasuco.infrastructure.config.security;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.InstitutionalJwtAuthenticationConverter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de seguridad HTTP, deliberadamente neutral respecto al proveedor de identidad:
 * no importa nada de {@code keycloak} ni conoce cómo un proveedor concreto estructura sus
 * claims. Depende únicamente de {@link JwtDecoder} y {@link JwtClaimsExtractor}, ambos
 * registrados por el Composition Root de seguridad según
 * {@code app.adapters.security.provider} (hoy {@code KeycloakSecurityAdapterConfiguration}).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.security.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    @Value("${app.security.azure-events.webhook-token:}")
    private String azureWebhookToken;

    @Bean
    public InstitutionalJwtAuthenticationConverter institutionalJwtAuthenticationConverter(
            final JwtClaimsExtractor jwtClaimsExtractor
    ) {
        return new InstitutionalJwtAuthenticationConverter(jwtClaimsExtractor);
    }

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain securityFilterChain(
            final HttpSecurity http,
            final JwtDecoder jwtDecoder,
            final InstitutionalJwtAuthenticationConverter institutionalJwtAuthenticationConverter,
            final AuthenticationEntryPoint authenticationEntryPoint,
            final AccessDeniedHandler accessDeniedHandler,
            final ObjectProvider<SecurityBaselinePepFilter> securityBaselinePepFilter
    ) throws Exception {
        http
            .cors(Customizer.withDefaults())
            // Política CSRF explícita (Sonar java:S4502, hotspot revisado como Safe):
            // /api/v1/** es una API REST stateless. La API de negocio se autentica solo con un JWT
            // en Authorization: Bearer (credencial explícita que el navegador NO adjunta solo, a
            // diferencia de una cookie) y el webhook /api/v1/internal/azure-events, con el header
            // aeg-sas-token validado por AzureEventGridAuthFilter (fail-closed). Ninguna ruta
            // /api/v1/** autentica por cookie ni HttpSession (SessionCreationPolicy.STATELESS), así
            // que no hay credencial ambiente que un sitio ajeno pueda hacer valer y el token CSRF
            // no protege nada. Además, una petición insegura sin Bearer debe llegar al mecanismo de
            // autenticación y responder 401, no 403 de CSRF.
            // REVISAR ESTA DECISIÓN antes de introducir autenticación por cookie, BFF con cookie
            // HttpOnly o sesión de navegador: CSRF deberá volver a habilitarse para ese flujo.
            // Fuera de /api/v1/** la protección CSRF de Spring Security sigue activa.
            // /api/v2/sesiones/** (MAINT-003F) es la misma API REST stateless Bearer-only: aplica
            // exactamente la misma justificación y se excluye con el mismo alcance acotado.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/**", "/api/v2/sesiones/**"))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(
                    new AzureEventGridAuthFilter(azureWebhookToken),
                    org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class
            )
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                // permitAll de AUTORIZACION: la autenticacion la exige AzureEventGridAuthFilter (header
                // aeg-sas-token, fail-closed). No equivale a un endpoint sin autenticacion.
                .requestMatchers(AzureEventGridAuthFilter.WEBHOOK_PATH).permitAll()
                // Solo lectura de documentación runtime. La propiedad de Swagger UI controla
                // si estos handlers existen; ningún endpoint de negocio se abre por esta regla.
                .requestMatchers(
                        HttpMethod.GET,
                        "/swagger-ui",
                        "/swagger-ui/**",
                        "/openapi/openapi-golden-path.yaml"
                ).permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/v1/decano/**").hasAnyRole("DECANO", "ADMINISTRADOR")
                .requestMatchers("/api/v1/coordinador/**").hasAnyRole("COORDINADOR", "ADMINISTRADOR")
                .requestMatchers("/api/v1/docente/**").hasRole("DOCENTE")
                // Directorio general de docentes (crear/consultar/asignar a grupo): hoy sin
                // contrato funcional explícito de quién debe administrarlo; se restringe a los
                // roles que ya administran docentes en otros endpoints (ver duda documentada
                // en docs/security/runtime-security-provider-architecture.md).
                .requestMatchers("/api/v1/docentes/**").hasAnyRole("COORDINADOR", "ADMINISTRADOR")
                .requestMatchers("/api/v1/estudiante/**").hasRole("ESTUDIANTE")
                // Directorio general/institucional de estudiantes (distinto del portal propio
                // en /estudiante/**): expone datos de CUALQUIER estudiante filtrando por
                // grupo/programa/facultad. El docente ya tiene su vertical específica y
                // acotada por grupo en GET /api/v1/grupos/{grupoId}/estudiantes; por eso este
                // directorio general NO se abre a DOCENTE (ver política documentada en
                // docs/security/runtime-security-provider-architecture.md).
                .requestMatchers("/api/v1/estudiantes/**").hasAnyRole("COORDINADOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.POST, "/api/v1/usuarios").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/sesiones/**").hasRole("DOCENTE")
                .requestMatchers(HttpMethod.PUT, "/api/v1/sesiones/**").hasRole("DOCENTE")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/sesiones/**").hasRole("DOCENTE")
                // Generación del QR/PIN de una sesión: acción del docente que dicta la sesión.
                .requestMatchers(HttpMethod.GET, "/api/v1/sesiones/*/qr-token").hasRole("DOCENTE")
                // Sesiones agrupadas por grupo: usadas por el docente para elegir la sesion sobre
                // la que va a tomar asistencia. La titularidad sobre el grupo se valida en
                // Application via InstitutionalScopePort (ver ConsultarSesionesPorGrupoUseCaseImpl).
                .requestMatchers(HttpMethod.GET, "/api/v1/sesiones/grupo/*").hasRole("DOCENTE")
                // Contrato UTC v2 de sesiones (UTC-D05/D08): escrituras y lecturas solo DOCENTE; la
                // titularidad sobre grupo/sesion se valida en Application (InstitutionalScopePort) y,
                // en escrituras, tambien en la DB (usp_*_sesion_v2).
                .requestMatchers("/api/v2/sesiones/**").hasRole("DOCENTE")
                .requestMatchers(HttpMethod.POST, "/api/v1/asistencias/lote").hasRole("DOCENTE")
                .requestMatchers(HttpMethod.POST, "/api/v1/asistencias/revisiones").hasRole("ESTUDIANTE")
                // Subida de soportes de revision: unicamente el estudiante propietario sube en su
                // propio nombre (OWNERSHIP_DECISION.md: DOCENTE upload = OUT_OF_SCOPE, sin
                // capability). La descarga GET /api/v1/archivos/{fileId} queda en la regla
                // catch-all authenticated() de abajo: el ownership real (404 para no-propietario,
                // incluido cualquier rol) se resuelve en Application, no aqui.
                .requestMatchers(HttpMethod.POST, "/api/v1/archivos/subir").hasRole("ESTUDIANTE")
                .requestMatchers(HttpMethod.POST, "/api/v1/asistencias").hasRole("DOCENTE")
                // Consulta de asistencias por grupo (ruta legacy vía POST): mismo criterio que
                // GET /api/v1/grupos/{grupoId}/asistencias.
                .requestMatchers(HttpMethod.POST, "/api/v1/asistencias/consultas/grupo")
                    .hasAnyRole("DOCENTE", "COORDINADOR", "ADMINISTRADOR")
                // Listado institucional completo de grupos: NO es la fuente de "mis grupos" del
                // docente (esa es GET /api/v1/docente/horarios, ya scopeada por Usuario->Docente).
                // Abrir este listado a DOCENTE permitiria enumerar grupos de otros docentes.
                .requestMatchers(HttpMethod.GET, "/api/v1/grupos")
                    .hasAnyRole("COORDINADOR", "ADMINISTRADOR")
                // Sub-recursos de un grupo especifico (estudiantes, asistencias): el DOCENTE si
                // los necesita, pero scopeados a su propio grupo en Application via
                // InstitutionalScopePort (ver ConsultarEstudiantesGrupoUseCaseImpl /
                // ConsultarAsistenciasPorGrupoUseCaseImpl).
                .requestMatchers(HttpMethod.GET, "/api/v1/grupos/**")
                    .hasAnyRole("DOCENTE", "COORDINADOR", "ADMINISTRADOR")
                // Matricula de estudiante en grupo: la DB controla titularidad (DOCENTE titular del
                // grupo O COORDINADOR del programa, ver usp_registrar_estudiante_en_grupo, SEC_002).
                // ADMINISTRADOR no tiene bypass en la DB para esta operacion; por eso no se incluye
                // aqui, a diferencia de crear/actualizar grupo.
                .requestMatchers(HttpMethod.POST, "/api/v1/grupos/*/estudiantes")
                    .hasAnyRole("DOCENTE", "COORDINADOR")
                .requestMatchers(HttpMethod.POST, "/api/v1/grupos/**").hasAnyRole("COORDINADOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/api/v1/grupos/**").hasAnyRole("COORDINADOR", "ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/grupos/**").hasAnyRole("COORDINADOR", "ADMINISTRADOR")
                // Utilidad de desarrollo para forzar eventos SSE manualmente: no tiene todavía
                // un rol funcional propio; se restringe a ADMINISTRADOR hasta que Realtime
                // tenga su propio Port (ver documentación).
                .requestMatchers(HttpMethod.POST, "/api/v1/realtime/emit").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/v1/realtime/**").authenticated()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder)
                    .jwtAuthenticationConverter(institutionalJwtAuthenticationConverter)
                )
            );

        securityBaselinePepFilter.ifAvailable(filter ->
                http.addFilterAfter(
                        filter,
                        org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class
                ));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList());
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                "X-Correlation-Id",
                HttpHeaders.ACCEPT
        ));
        configuration.setExposedHeaders(List.of("X-Correlation-Id"));
        // La API es Bearer-only: Authorization es un header explícito y no requiere credenciales
        // de navegador (cookies, auth HTTP, certificados TLS de cliente).
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
