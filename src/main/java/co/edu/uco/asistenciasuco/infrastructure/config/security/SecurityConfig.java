package co.edu.uco.asistenciasuco.infrastructure.config.security;

import jakarta.servlet.DispatcherType;
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
 * {@code app.adapters.security.provider} (legacy o Security Baseline).
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
            final ObjectProvider<SecurityPepEnforcementFilter> securityPepEnforcementFilter
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
            // HttpOnly o sesión de navegador hacia ESTA API: CSRF deberá volver a habilitarse para ese flujo.
            // El modo baseline no lo hace: la SPA obtiene el Bearer del realm central con PKCE y la cookie BFF
            // solo existe entre el navegador y el PDP.
            // Fuera de /api/v1/** la protección CSRF de Spring Security sigue activa.
            // /api/v2/sesiones/** (MAINT-003F) es la misma API REST stateless Bearer-only: aplica
            // exactamente la misma justificación y se excluye con el mismo alcance acotado.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/**", "/api/v2/sesiones/**"))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(
                    new AzureEventGridAuthFilter(azureWebhookToken),
                    org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class
            );

        http
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                // CULM-001 A.2-5: un error lanzado en un filtro se despacha a /error; ese despacho muestra el estado real (y el
                // PEP ya exime /error, SecurityPublicEndpoints.ERROR_VIEW) en vez de un 401 falso por no estar autenticado.
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(SecurityPublicEndpoints.ACTUATOR).permitAll()
                // permitAll de AUTORIZACION: la autenticacion la exige AzureEventGridAuthFilter (header
                // aeg-sas-token, fail-closed). No equivale a un endpoint sin autenticacion.
                .requestMatchers(AzureEventGridAuthFilter.WEBHOOK_PATH).permitAll()
                // IDENT-001: verificacion publica del correo. permitAll solo para esta ruta POST (sin comodin /public/**): la
                // credencial es el token de un solo uso y el limite de intentos lo aplica VerificacionCorreoRateLimitFilter.
                .requestMatchers(HttpMethod.POST, SecurityPublicEndpoints.VERIFICAR_CORREO).permitAll()
                // HU176B: matricula publica por QR/PIN. Las unicas dos rutas publicas de negocio nuevas, declaradas una por una
                // (sin comodin /public/**): la credencial es el token/PIN rotativo; el limite de intentos lo aplica
                // MatriculaQrRateLimitFilter y la red del campus y el interruptor los exige el caso de uso.
                .requestMatchers(HttpMethod.POST, SecurityPublicEndpoints.MATRICULA_QR_VALIDAR, SecurityPublicEndpoints.MATRICULA_QR_REGISTRO).permitAll()
                // Solo lectura de documentación runtime. La propiedad de Swagger UI controla
                // si estos handlers existen; ningún endpoint de negocio se abre por esta regla.
                .requestMatchers(
                        HttpMethod.GET,
                        "/swagger-ui",
                        "/swagger-ui/**",
                        "/openapi/openapi-golden-path.yaml"
                ).permitAll()
                // Catálogo institucional de solo lectura: áreas de conocimiento (sedes retiradas: HU020/HU140 descartadas)
                .requestMatchers(HttpMethod.GET, "/api/v1/admin/areas", "/api/v1/admin/areas/**")
                    .hasAnyRole(ROLE_ADMINISTRADOR, ROLE_DECANO, ROLE_COORDINADOR)
                .requestMatchers("/api/v1/admin/**").hasRole(ROLE_ADMINISTRADOR)
                // CULM-001 A.2-3 (F-TRX-018, DEC-022 §2): el ADMINISTRADOR no opera las vistas de decano ni de coordinador.
                .requestMatchers("/api/v1/decano/**").hasRole(ROLE_DECANO)
                // Unica excepcion: el catalogo de periodos lo consume la pantalla del administrador (admin-periodos, admin-system).
                .requestMatchers(HttpMethod.GET, "/api/v1/coordinador/periodos-academicos")
                    .hasAnyRole(ROLE_DECANO, ROLE_COORDINADOR, ROLE_ADMINISTRADOR)
                .requestMatchers(HttpMethod.GET, "/api/v1/coordinador/planes-estudio", "/api/v1/coordinador/planes-estudio/**", "/api/v1/coordinador/periodos-academicos/**", "/api/v1/coordinador/asignaturas", "/api/v1/coordinador/asignaturas/**")
                    .hasAnyRole(ROLE_DECANO, ROLE_COORDINADOR)
                .requestMatchers("/api/v1/coordinador/**").hasRole(ROLE_COORDINADOR)
                .requestMatchers("/api/v1/docente/**").hasRole(ROLE_DOCENTE)
                // Directorio general de docentes (crear/consultar/asignar a grupo): hoy sin
                // contrato funcional explícito de quién debe administrarlo; se restringe a los
                // roles que ya administran docentes en otros endpoints (ver duda documentada
                // en docs/security/runtime-security-provider-architecture.md).
                .requestMatchers("/api/v1/docentes/**").hasAnyRole(ROLE_COORDINADOR, ROLE_ADMINISTRADOR)
                .requestMatchers("/api/v1/estudiante/**").hasRole(ROLE_ESTUDIANTE)
                // IDENT-001: dominio institucional vigente para validar el formulario de registro manual del estudiante.
                .requestMatchers(HttpMethod.GET, "/api/v1/configuracion/registro-estudiante")
                    .hasAnyRole(ROLE_DOCENTE, ROLE_COORDINADOR)
                // Directorio de estudiantes (golden path 1.2.0, DEC-027/DEC-032): DOCENTE, COORDINADOR y ADMINISTRADOR; el ambito
                // (grupos propios, programas, institucion) lo aplica usp_consultar_directorio_estudiantes_paginado.
                .requestMatchers("/api/v1/estudiantes/**").hasAnyRole(ROLE_DOCENTE, ROLE_COORDINADOR, ROLE_ADMINISTRADOR)
                .requestMatchers(HttpMethod.POST, "/api/v1/usuarios").authenticated()
                // Estado de acceso y reconciliacion de cuentas: administrador, decano y coordinador; el alcance (facultad /
                // programa) y los roles gestionables se validan en Application (AccesoCuentaService).
                .requestMatchers(HttpMethod.POST, "/api/v1/identidad/cuentas/acceso/*")
                    .hasAnyRole(ROLE_ADMINISTRADOR, ROLE_DECANO, ROLE_COORDINADOR)
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
                // F-COO-004 (CULM-001 A): matricula solo del DOCENTE titular (la DB valida titularidad, SEC_002).
                // COORDINADOR y ADMINISTRADOR no matriculan (usp_registrar_estudiante_en_grupo exige perfil DOCENTE).
                .requestMatchers(HttpMethod.POST, "/api/v1/grupos/*/estudiantes", "/api/v1/grupos/{grupoId}/estudiantes")
                    .hasRole(ROLE_DOCENTE)
                // IDENT-001: reenvio del enlace de verificacion de correo. Mismo criterio que la matricula manual: la DB valida
                // que el ejecutor sea docente titular del grupo o coordinador de su programa.
                // HU176B: el docente abre (POST), regenera (POST /rotacion) y cierra (DELETE) la ventana de matricula por QR de su
                // grupo. La titularidad se valida en Application y en la DB. Va ANTES de los comodines de /grupos/**.
                .requestMatchers(HttpMethod.POST, "/api/v1/grupos/*/matricula-qr", "/api/v1/grupos/*/matricula-qr/rotacion").hasRole(ROLE_DOCENTE)
                .requestMatchers(HttpMethod.DELETE, "/api/v1/grupos/*/matricula-qr").hasRole(ROLE_DOCENTE)
                .requestMatchers(HttpMethod.POST, "/api/v1/grupos/*/estudiantes/*/reenviar-verificacion")
                    .hasAnyRole(ROLE_DOCENTE, ROLE_COORDINADOR)
                // F3: el DOCENTE crea sus propios grupos (la DB exige que el titular sea el propio docente y que
                // este tenga vinculo activo con el programa, ver usp_crear_grupo). PUT/DELETE siguen sin DOCENTE.
                .requestMatchers(HttpMethod.POST, "/api/v1/grupos")
                    .hasAnyRole(ROLE_DOCENTE, ROLE_COORDINADOR, ROLE_ADMINISTRADOR)
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

        // SEC-BASELINE-001: unico punto de registro del filtro PEP (su FilterRegistrationBean servlet
        // esta deshabilitado). Despues de autenticar el Bearer y antes de AuthorizationFilter.
        securityPepEnforcementFilter.ifAvailable(filter ->
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
        configuration.setExposedHeaders(List.of("X-Correlation-Id", "X-Decision-Id"));
        // Bearer-only en ambos modos: ninguna cookie (ni la del BFF del baseline) es credencial de esta API.
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
