package co.edu.uco.asistenciasuco.infrastructure.config.security;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.InstitutionalJwtAuthenticationConverter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
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

    private static final String ROLE_ADMINISTRADOR = "ADMINISTRADOR";
    private static final String ROLE_DECANO = "DECANO";
    private static final String ROLE_COORDINADOR = "COORDINADOR";
    private static final String ROLE_DOCENTE = "DOCENTE";
    private static final String ROLE_ESTUDIANTE = "ESTUDIANTE";

    @Value("${app.security.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200,http://localhost:5174,http://localhost:5173,http://localhost:5175}")
    private String allowedOrigins;

    @Value("${app.security.azure-events.webhook-token:}")
    private String azureWebhookToken;

    @Value("${security.pep.enforcement.enabled:false}")
    private boolean pepEnforcementEnabled;

    @Value("${security.pep.enforcement.pep-url:http://localhost:18081}")
    private String pepUrl;

    @Value("${security.pep.enforcement.application-name:asistencias-backend}")
    private String pepApplicationName;

    @Value("${security.pep.enforcement.environment:local}")
    private String pepEnvironment;

    @Value("${security.pep.enforcement.application-credential:}")
    private String pepApplicationCredential;

    @Value("${security.pep.enforcement.public-paths:/actuator/health,/swagger-ui/**,/v3/api-docs/**}")
    private String pepPublicPaths;

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
            final AccessDeniedHandler accessDeniedHandler
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
            );

        if (pepEnforcementEnabled) {
            http.addFilterAfter(
                    new SecurityPepEnforcementFilter(
                            pepEnforcementEnabled,
                            pepUrl,
                            pepApplicationName,
                            pepEnvironment,
                            pepApplicationCredential,
                            pepPublicPaths
                    ),
                    org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class
            );
        }

        http
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
                // Catálogos institucionales de solo lectura: sedes y áreas de conocimiento
                .requestMatchers(HttpMethod.GET, "/api/v1/admin/sedes", "/api/v1/admin/sedes/**", "/api/v1/admin/areas", "/api/v1/admin/areas/**")
                    .hasAnyRole(ROLE_ADMINISTRADOR, ROLE_DECANO, ROLE_COORDINADOR)
                .requestMatchers("/api/v1/admin/**").hasRole(ROLE_ADMINISTRADOR)
                .requestMatchers("/api/v1/decano/**").hasAnyRole(ROLE_DECANO, ROLE_ADMINISTRADOR)
                .requestMatchers(HttpMethod.GET, "/api/v1/coordinador/planes-estudio", "/api/v1/coordinador/planes-estudio/**", "/api/v1/coordinador/periodos-academicos", "/api/v1/coordinador/periodos-academicos/**", "/api/v1/coordinador/asignaturas", "/api/v1/coordinador/asignaturas/**")
                    .hasAnyRole(ROLE_DECANO, ROLE_COORDINADOR, ROLE_ADMINISTRADOR)
                .requestMatchers("/api/v1/coordinador/**").hasAnyRole(ROLE_COORDINADOR, ROLE_ADMINISTRADOR)
                .requestMatchers("/api/v1/docente/**").hasRole(ROLE_DOCENTE)
                // Directorio general de docentes (crear/consultar/asignar a grupo): hoy sin
                // contrato funcional explícito de quién debe administrarlo; se restringe a los
                // roles que ya administran docentes en otros endpoints (ver duda documentada
                // en docs/security/runtime-security-provider-architecture.md).
                .requestMatchers("/api/v1/docentes/**").hasAnyRole(ROLE_COORDINADOR, ROLE_ADMINISTRADOR)
                .requestMatchers("/api/v1/estudiante/**").hasRole(ROLE_ESTUDIANTE)
                // Directorio general/institucional de estudiantes: permite a DOCENTE consultar y validar
                // la existencia del estudiante a matricular en su grupo (junto con COORDINADOR y ADMINISTRADOR).
                .requestMatchers("/api/v1/estudiantes/**").hasAnyRole(ROLE_DOCENTE, ROLE_COORDINADOR, ROLE_ADMINISTRADOR)
                .requestMatchers(HttpMethod.POST, "/api/v1/usuarios").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/sesiones/**").hasRole(ROLE_DOCENTE)
                .requestMatchers(HttpMethod.PUT, "/api/v1/sesiones/**").hasRole(ROLE_DOCENTE)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/sesiones/**").hasRole(ROLE_DOCENTE)
                // Generación del QR/PIN de una sesión: acción del docente que dicta la sesión.
                .requestMatchers(HttpMethod.GET, "/api/v1/sesiones/*/qr-token").hasRole(ROLE_DOCENTE)
                // Sesiones agrupadas por grupo: usadas por el docente para elegir la sesion sobre
                // la que va a tomar asistencia. La titularidad sobre el grupo se valida en
                // Application via InstitutionalScopePort (ver ConsultarSesionesPorGrupoUseCaseImpl).
                .requestMatchers(HttpMethod.GET, "/api/v1/sesiones/grupo/*").hasRole(ROLE_DOCENTE)
                // Contrato UTC v2 de sesiones (UTC-D05/D08): escrituras y lecturas solo DOCENTE; la
                // titularidad sobre grupo/sesion se valida en Application (InstitutionalScopePort) y,
                // en escrituras, tambien en la DB (usp_*_sesion_v2).
                .requestMatchers("/api/v2/sesiones/**").hasRole(ROLE_DOCENTE)
                .requestMatchers(HttpMethod.POST, "/api/v1/asistencias/lote").hasRole(ROLE_DOCENTE)
                .requestMatchers(HttpMethod.POST, "/api/v1/asistencias/revisiones").hasRole(ROLE_ESTUDIANTE)
                // Subida de soportes de revision: unicamente el estudiante propietario sube en su
                // propio nombre (OWNERSHIP_DECISION.md: DOCENTE upload = OUT_OF_SCOPE, sin
                // capability). La descarga GET /api/v1/archivos/{fileId} queda en la regla
                // catch-all authenticated() de abajo: el ownership real (404 para no-propietario,
                // incluido cualquier rol) se resuelve en Application, no aqui.
                .requestMatchers(HttpMethod.POST, "/api/v1/archivos/subir").hasRole(ROLE_ESTUDIANTE)
                .requestMatchers(HttpMethod.POST, "/api/v1/asistencias").hasRole(ROLE_DOCENTE)
                // Consulta de asistencias por grupo (ruta legacy vía POST): mismo criterio que
                // GET /api/v1/grupos/{grupoId}/asistencias.
                .requestMatchers(HttpMethod.POST, "/api/v1/asistencias/consultas/grupo")
                    .hasAnyRole(ROLE_DOCENTE, ROLE_COORDINADOR, ROLE_ADMINISTRADOR)
                // Listado institucional completo de grupos: NO es la fuente de "mis grupos" del
                // docente (esa es GET /api/v1/docente/horarios, ya scopeada por Usuario->Docente).
                // Abrir este listado a DOCENTE permitiria enumerar grupos de otros docentes.
                .requestMatchers(HttpMethod.GET, "/api/v1/grupos")
                    .hasAnyRole(ROLE_COORDINADOR, ROLE_ADMINISTRADOR)
                // Sub-recursos de un grupo especifico (estudiantes, asistencias): el DOCENTE si
                // los necesita, pero scopeados a su propio grupo en Application via
                // InstitutionalScopePort (ver ConsultarEstudiantesGrupoUseCaseImpl /
                // ConsultarAsistenciasPorGrupoUseCaseImpl).
                .requestMatchers(HttpMethod.GET, "/api/v1/grupos/**")
                    .hasAnyRole(ROLE_DOCENTE, ROLE_COORDINADOR, ROLE_ADMINISTRADOR)
                // Matricula de estudiante en grupo: la DB controla titularidad (DOCENTE titular del
                // grupo O COORDINADOR del programa, ver usp_registrar_estudiante_en_grupo, SEC_002).
                // ADMINISTRADOR no tiene bypass en la DB para esta operacion; por eso no se incluye
                // aqui, a diferencia de crear/actualizar grupo.
                .requestMatchers(HttpMethod.POST, "/api/v1/grupos/*/estudiantes", "/api/v1/grupos/{grupoId}/estudiantes")
                    .hasAnyRole(ROLE_DOCENTE, ROLE_COORDINADOR)
                .requestMatchers(HttpMethod.POST, "/api/v1/grupos/**").hasAnyRole(ROLE_COORDINADOR, ROLE_ADMINISTRADOR)
                .requestMatchers(HttpMethod.PUT, "/api/v1/grupos/**").hasAnyRole(ROLE_COORDINADOR, ROLE_ADMINISTRADOR)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/grupos/**").hasAnyRole(ROLE_COORDINADOR, ROLE_ADMINISTRADOR)
                // Utilidad de desarrollo para forzar eventos SSE manualmente: no tiene todavía
                // un rol funcional propio; se restringe a ADMINISTRADOR hasta que Realtime
                // tenga su propio Port (ver documentación).
                .requestMatchers(HttpMethod.POST, "/api/v1/realtime/emit").hasRole(ROLE_ADMINISTRADOR)
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
        // Security Baseline UCO (PDP/BFF): permite cookies de sesión del navegador (credentials: "include")
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
