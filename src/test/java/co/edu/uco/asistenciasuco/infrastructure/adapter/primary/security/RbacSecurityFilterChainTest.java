package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.ClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.CorrelationIdFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.RequestActorResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.auth.AuthController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAccessDeniedHandler;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.ApiAuthenticationEntryPoint;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.SecurityContextAuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.keycloak.KeycloakJwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.contract.JwtClaimsExtractor;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.jwt.validation.AudienceValidator;
import co.edu.uco.asistenciasuco.infrastructure.config.security.SecurityConfig;
import co.edu.uco.asistenciasuco.infrastructure.audit.contract.AuditEventPublisher;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el ORDEN y CONTENIDO de los matchers de {@link SecurityConfig} — no la lógica de
 * ningún UseCase real. Por eso usa un controller sonda ({@link RbacProbeController}) mapeado
 * a las rutas reales, en lugar de los controllers de producción (que requerirían mockear
 * muchos InputPorts sin aportar nada a esta verificación).
 *
 * <p>Política vigente verificada aquí (inspeccionada en {@code SecurityConfig} antes de
 * escribir las expectativas, no inventada):</p>
 * <ul>
 *   <li>{@code /api/v1/docentes/**} (directorio general, {@code DocenteController}): solo
 *       {@code COORDINADOR}/{@code ADMINISTRADOR} — ver duda documentada en
 *       docs/security/runtime-security-provider-architecture.md (no hay contrato funcional
 *       explícito; DOCENTE NO tiene acceso bajo la política actual).</li>
 *   <li>{@code POST /api/v1/grupos} (command de coordinación; el DOCENTE solo crea a su nombre, lo exige la DB): {@code COORDINADOR}/{@code ADMINISTRADOR}/{@code DOCENTE}.</li>
 *   <li>{@code POST /api/v1/asistencias/lote} (registro docente por lote): {@code DOCENTE}.</li>
 *   <li>{@code /api/v1/estudiante/**} (portal del propio estudiante): {@code ESTUDIANTE}.</li>
 * </ul>
 */
@WebMvcTest(controllers = {RbacSecurityFilterChainTest.RbacProbeController.class, AuthController.class})
@Import({
        SecurityConfig.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        SecurityErrorResponseWriter.class,
        CorrelationIdFilter.class,
        ClientIpResolver.class,
        RequestActorResolver.class,
        RbacSecurityFilterChainTest.RbacProbeController.class,
        AuthController.class,
        SecurityContextAuthenticatedUserResolver.class,
        RbacSecurityFilterChainTest.RbacTestSupportConfig.class
})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
class RbacSecurityFilterChainTest {

    private static final String ISSUER = "http://127.0.0.1:8081/realms/asistencias-uco";
    private static final String AUDIENCE = "asistencias-api";
    private static final String API_CLIENT_ID = "asistencias-api";
    private static final String USER_ID_CLAIM = "idUsuario";
    private static final String USER_ID = "22222222-2222-2222-2222-222222222222";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void swaggerUiAndCanonicalOpenApiArePublicButBusinessApiRemainsProtected() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/openapi/openapi-golden-path.yaml"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/docentes"))
                .andExpect(status().isUnauthorized());
    }

    // --- /api/v1/docentes/** : directorio general (COORDINADOR/ADMINISTRADOR) ---

    @Test
    void docentes_directorio_estudiante_recibe_403() throws Exception {
        mockMvc.perform(get("/api/v1/docentes").header(HttpHeaders.AUTHORIZATION, bearer("ESTUDIANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void docentes_directorio_docente_recibe_403_bajo_politica_actual() throws Exception {
        // Política vigente: DOCENTE no administra el directorio general de docentes.
        mockMvc.perform(get("/api/v1/docentes").header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void docentes_directorio_coordinador_es_permitido() throws Exception {
        mockMvc.perform(get("/api/v1/docentes").header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isOk());
    }

    // --- /api/v1/coordinador/docentes/** : directorio y vinculacion del programa (HU062/HU091) ---

    @Test
    void coordinador_indicadores_sin_token_recibe_401() throws Exception {
        for (final String ruta : INDICADORES_COORDINADOR) {
            mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void coordinador_indicadores_solo_coordinador_y_administrador_pasan_el_filtro() throws Exception {
        for (final String ruta : INDICADORES_COORDINADOR) {
            for (final String rol : new String[]{"DOCENTE", "ESTUDIANTE"}) {
                mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, bearer(rol))).andExpect(status().isForbidden());
            }
            for (final String rol : new String[]{"COORDINADOR", "ADMINISTRADOR"}) {
                mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, bearer(rol))).andExpect(status().isOk());
            }
        }
    }

    @Test
    void decano_indicadores_sin_token_recibe_401() throws Exception {
        for (final String ruta : INDICADORES_DECANO) {
            mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void decano_indicadores_coordinador_docente_y_estudiante_reciben_403_y_administrador_pasa_el_filtro() throws Exception {
        for (final String ruta : INDICADORES_DECANO) {
            for (final String rol : new String[]{"COORDINADOR", "DOCENTE", "ESTUDIANTE"}) {
                mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, bearer(rol))).andExpect(status().isForbidden());
            }
            mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, bearer("ADMINISTRADOR"))).andExpect(status().isOk());
        }
    }

    @Test
    void admin_indicadores_sin_token_recibe_401() throws Exception {
        for (final String ruta : INDICADORES_ADMIN) {
            mockMvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void admin_indicadores_docente_estudiante_y_coordinador_reciben_403_y_administrador_pasa_el_filtro() throws Exception {
        for (final String ruta : INDICADORES_ADMIN) {
            for (final String rol : new String[]{"COORDINADOR", "DOCENTE", "ESTUDIANTE"}) {
                mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, bearer(rol))).andExpect(status().isForbidden());
            }
            mockMvc.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, bearer("ADMINISTRADOR"))).andExpect(status().isOk());
        }
    }

    private static final String[] INDICADORES_ADMIN = {
            "/api/v1/admin/indicadores",
            "/api/v1/admin/comparativa-facultades",
            "/api/v1/admin/tendencia-ausentismo"};

    private static final String[] INDICADORES_DECANO = {
            "/api/v1/decano/indicadores",
            "/api/v1/decano/comparativa-programas",
            "/api/v1/decano/aforo-grupos"};

    private static final String[] INDICADORES_COORDINADOR = {
            "/api/v1/coordinador/indicadores",
            "/api/v1/coordinador/alertas-tempranas",
            "/api/v1/coordinador/estudiantes/00000000-0000-0000-0000-000000000001/bitacora",
            "/api/v1/coordinador/estudiantes/00000000-0000-0000-0000-000000000001/ficha"};

    @Test
    void coordinador_docentes_sin_token_recibe_401() throws Exception {
        mockMvc.perform(get("/api/v1/coordinador/docentes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/coordinador/docentes/candidatos?busqueda=abc")).andExpect(status().isUnauthorized());
    }

    @Test
    void coordinador_docentes_estudiante_y_docente_reciben_403() throws Exception {
        for (final String rol : new String[]{"ESTUDIANTE", "DOCENTE"}) {
            mockMvc.perform(get("/api/v1/coordinador/docentes").header(HttpHeaders.AUTHORIZATION, bearer(rol)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/coordinador/docentes/candidatos?busqueda=abc")
                            .header(HttpHeaders.AUTHORIZATION, bearer(rol)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/coordinador/docentes").header(HttpHeaders.AUTHORIZATION, bearer(rol))
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void coordinador_docentes_put_exige_rol_coordinador() throws Exception {
        final String ruta = "/api/v1/coordinador/docentes/11111111-1111-1111-1111-111111111111";
        mockMvc.perform(put(ruta)).andExpect(status().isUnauthorized());
        for (final String rol : new String[]{"ESTUDIANTE", "DOCENTE"}) {
            mockMvc.perform(put(ruta).header(HttpHeaders.AUTHORIZATION, bearer(rol)).contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(put(ruta).header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")).contentType("application/json").content("{}"))
                .andExpect(status().isOk());
    }

    // --- HU096/HU097/HU098: el periodo es unico por institucion; solo el ADMINISTRADOR lo gestiona ---

    @Test
    void periodos_academicos_comandos_solo_para_administrador() throws Exception {
        final String coleccion = "/api/v1/admin/periodos-academicos";
        final String recurso = coleccion + "/11111111-1111-1111-1111-111111111111";
        mockMvc.perform(post(coleccion)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(coleccion).header(HttpHeaders.AUTHORIZATION, bearer("ADMINISTRADOR")).contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(put(recurso).header(HttpHeaders.AUTHORIZATION, bearer("ADMINISTRADOR")).contentType("application/json").content("{}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete(recurso).header(HttpHeaders.AUTHORIZATION, bearer("ADMINISTRADOR")))
                .andExpect(status().isOk());
        for (final String rol : new String[]{"DECANO", "COORDINADOR", "DOCENTE", "ESTUDIANTE"}) {
            mockMvc.perform(post(coleccion).header(HttpHeaders.AUTHORIZATION, bearer(rol)).contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(put(recurso).header(HttpHeaders.AUTHORIZATION, bearer(rol)).contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete(recurso).header(HttpHeaders.AUTHORIZATION, bearer(rol)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void coordinador_docentes_patch_estado_exige_rol_coordinador() throws Exception {
        final String ruta = "/api/v1/coordinador/docentes/11111111-1111-1111-1111-111111111111/estado";
        mockMvc.perform(patch(ruta)).andExpect(status().isUnauthorized());
        for (final String rol : new String[]{"ESTUDIANTE", "DOCENTE"}) {
            mockMvc.perform(patch(ruta).header(HttpHeaders.AUTHORIZATION, bearer(rol)).contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(patch(ruta).header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")).contentType("application/json").content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void tipos_vinculacion_exige_autenticacion_y_es_visible_para_cualquier_rol_autenticado() throws Exception {
        mockMvc.perform(get("/api/v1/tipos-vinculacion")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/tipos-vinculacion").header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isOk());
    }

    // --- GET /api/v1/auth/me : identidad institucional resuelta por el backend ---

    @Test
    void auth_me_sin_token_o_con_token_invalido_recibe_401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer token-no-valido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void auth_me_devuelve_solo_uuid_institucional_y_roles_efectivos_para_cada_rol() throws Exception {
        for (final String role : List.of("ADMINISTRADOR", "DECANO", "COORDINADOR", "DOCENTE", "ESTUDIANTE")) {
            mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(role)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exitoso").value(true))
                    .andExpect(jsonPath("$.datos.idUsuario").value(USER_ID))
                    .andExpect(jsonPath("$.datos.roles[0]").value(role))
                    .andExpect(jsonPath("$.datos.roles.length()").value(1))
                    .andExpect(jsonPath("$.datos.length()").value(2));
        }
    }

    @Test
    void auth_me_no_acepta_identidad_suministrada_por_el_cliente() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .param("idUsuario", "99999999-9999-9999-9999-999999999999")
                        .param("email", "otro@uco.edu.co")
                        .header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.idUsuario").value(USER_ID));
    }

    // --- GET /api/v1/grupos : listado institucional completo (NO es "mis grupos" del docente) ---

    @Test
    void listar_grupos_docente_recibe_403() throws Exception {
        // El listado completo institucional no es la fuente de "mis grupos" del docente
        // (esa es GET /api/v1/docente/horarios); abrirlo permitiria enumerar grupos ajenos.
        mockMvc.perform(get("/api/v1/grupos").header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void listar_grupos_coordinador_es_permitido() throws Exception {
        mockMvc.perform(get("/api/v1/grupos").header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isOk());
    }

    // --- GET /api/v1/grupos/{grupoId}/estudiantes : sub-recurso de un grupo (DOCENTE si accede,
    // scopeado a su propio grupo en Application) ---

    @Test
    void estudiantes_de_grupo_docente_es_permitido_por_security_filter_chain() throws Exception {
        mockMvc.perform(get("/api/v1/grupos/{grupoId}/estudiantes", "11111111-1111-1111-1111-111111111111")
                        .header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk());
    }

    // --- GET /api/v1/sesiones/grupo/{grupoId} : sesiones de un grupo, usadas por el docente
    // para elegir sobre cual tomar asistencia ---

    @Test
    void sesiones_por_grupo_docente_es_permitido_por_security_filter_chain() throws Exception {
        mockMvc.perform(get("/api/v1/sesiones/grupo/{grupoId}", "11111111-1111-1111-1111-111111111111")
                        .header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk());
    }

    @Test
    void sesiones_por_grupo_coordinador_recibe_403() throws Exception {
        mockMvc.perform(get("/api/v1/sesiones/grupo/{grupoId}", "11111111-1111-1111-1111-111111111111")
                        .header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isForbidden());
    }

    // --- PUT (legacy) y PATCH /api/v1/sesiones/{sesionId}: misma autorización DOCENTE (LB-001C.2A) ---

    @Test
    void actualizar_sesion_put_y_patch_docente_permitido() throws Exception {
        mockMvc.perform(put("/api/v1/sesiones/{id}", "11111111-1111-1111-1111-111111111111")
                        .header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/sesiones/{id}", "11111111-1111-1111-1111-111111111111")
                        .header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk());
    }

    @Test
    void actualizar_sesion_put_y_patch_coordinador_recibe_403() throws Exception {
        mockMvc.perform(put("/api/v1/sesiones/{id}", "11111111-1111-1111-1111-111111111111")
                        .header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/sesiones/{id}", "11111111-1111-1111-1111-111111111111")
                        .header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizar_sesion_put_y_patch_con_bearer_invalido_recibe_401() throws Exception {
        mockMvc.perform(put("/api/v1/sesiones/{id}", "11111111-1111-1111-1111-111111111111")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/sesiones/{id}", "11111111-1111-1111-1111-111111111111")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /api/v1/grupos : command de coordinación; el DOCENTE solo crea a su nombre (F3, lo exige la DB) ---

    @Test
    void crear_grupo_estudiante_recibe_403() throws Exception {
        mockMvc.perform(post("/api/v1/grupos").header(HttpHeaders.AUTHORIZATION, bearer("ESTUDIANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void crear_grupo_docente_es_permitido_por_security_filter_chain() throws Exception {
        mockMvc.perform(post("/api/v1/grupos").header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk());
    }

    @Test
    void actualizar_y_eliminar_grupo_siguen_cerrados_al_docente() throws Exception {
        mockMvc.perform(put("/api/v1/grupos/{id}", "11111111-1111-1111-1111-111111111111")
                        .header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/grupos/{id}", "11111111-1111-1111-1111-111111111111")
                        .header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void crear_grupo_coordinador_es_permitido_por_security_filter_chain() throws Exception {
        mockMvc.perform(post("/api/v1/grupos").header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isOk());
    }

    // --- POST /api/v1/grupos/{grupoId}/estudiantes : matricula manual (HU050). F-COO-004 (CULM-001 A): solo el DOCENTE
    // titular; la DB exige perfil DOCENTE y titularidad (SEC_002). COORDINADOR y ADMINISTRADOR reciben 403 en el filtro. ---

    @Test
    void matricular_estudiante_docente_es_permitido_por_security_filter_chain() throws Exception {
        mockMvc.perform(post("/api/v1/grupos/{grupoId}/estudiantes", "11111111-1111-1111-1111-111111111111")
                        .header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk());
    }

    @Test
    void matricular_estudiante_coordinador_recibe_403() throws Exception {
        mockMvc.perform(post("/api/v1/grupos/{grupoId}/estudiantes", "11111111-1111-1111-1111-111111111111")
                        .header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void matricular_estudiante_administrador_recibe_403() throws Exception {
        mockMvc.perform(post("/api/v1/grupos/{grupoId}/estudiantes", "11111111-1111-1111-1111-111111111111")
                        .header(HttpHeaders.AUTHORIZATION, bearer("ADMINISTRADOR")))
                .andExpect(status().isForbidden());
    }

    // --- HU176B: ventana de matricula por QR (solo DOCENTE; la titularidad la valida Application y la DB) y las dos
    // unicas rutas publicas de matricula (permitAll declarado una por una, sin comodin /public/**) ---

    @Test
    void T32_abrir_rotar_y_cerrar_la_ventana_de_matricula_es_solo_del_docente() throws Exception {
        final String grupo = "11111111-1111-1111-1111-111111111111";
        mockMvc.perform(post("/api/v1/grupos/{g}/matricula-qr", grupo).header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/grupos/{g}/matricula-qr/rotacion", grupo).header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/grupos/{g}/matricula-qr", grupo).header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk());
    }

    @Test
    void T32_la_ventana_de_matricula_rechaza_a_coordinador_administrador_y_estudiante_con_403() throws Exception {
        final String grupo = "11111111-1111-1111-1111-111111111111";
        for (final String rol : List.of("COORDINADOR", "ADMINISTRADOR", "ESTUDIANTE", "DECANO")) {
            mockMvc.perform(post("/api/v1/grupos/{g}/matricula-qr", grupo).header(HttpHeaders.AUTHORIZATION, bearer(rol)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/grupos/{g}/matricula-qr/rotacion", grupo).header(HttpHeaders.AUTHORIZATION, bearer(rol)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/v1/grupos/{g}/matricula-qr", grupo).header(HttpHeaders.AUTHORIZATION, bearer(rol)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void T32_la_ventana_de_matricula_sin_token_recibe_401() throws Exception {
        mockMvc.perform(post("/api/v1/grupos/{g}/matricula-qr", "11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void las_rutas_publicas_de_matricula_qr_no_exigen_token() throws Exception {
        mockMvc.perform(post("/api/v1/public/matricula-qr/validar")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/public/matricula-qr/registro")).andExpect(status().isOk());
    }

    @Test
    void T31_no_hay_comodin_publico_otra_ruta_public_sin_token_recibe_401() throws Exception {
        mockMvc.perform(post("/api/v1/public/otra")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/public/matricula-qr/otra")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/public/matricula-qr/validar")).andExpect(status().isUnauthorized());
    }

    @Test
    void matricular_grupo_del_estudiante_sigue_siendo_solo_del_rol_estudiante() throws Exception {
        mockMvc.perform(post("/api/v1/estudiante/matricular-grupo").header(HttpHeaders.AUTHORIZATION, bearer("ESTUDIANTE")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/estudiante/matricular-grupo").header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/estudiante/matricular-grupo")).andExpect(status().isUnauthorized());
    }

    // --- Política CSRF: /api/v1/** es stateless y Bearer-only; sin Bearer => 401, nunca 403 de CSRF ---

    @Test
    void peticion_insegura_solo_con_cookie_de_sesion_no_autentica_y_responde_401() throws Exception {
        mockMvc.perform(post("/api/v1/grupos").cookie(new Cookie("JSESSIONID", "session-de-prueba")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void metodos_inseguros_protegidos_sin_bearer_responden_401_y_no_403_de_csrf() throws Exception {
        mockMvc.perform(post("/api/v1/grupos")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/sesiones/{id}", "s-1")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/v1/sesiones/{id}", "s-1")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/grupos/{id}", "g-1")).andExpect(status().isUnauthorized());
    }

    @Test
    void metodos_inseguros_con_bearer_valido_no_requieren_token_csrf() throws Exception {
        mockMvc.perform(post("/api/v1/grupos").header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/sesiones/{id}", "s-1").header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/sesiones/{id}", "s-1").header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/grupos/{id}", "g-1").header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void metodos_inseguros_con_rol_incorrecto_responden_403_de_autorizacion() throws Exception {
        mockMvc.perform(post("/api/v1/grupos").header(HttpHeaders.AUTHORIZATION, bearer("ESTUDIANTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/sesiones/{id}", "s-1").header(HttpHeaders.AUTHORIZATION, bearer("ESTUDIANTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/sesiones/{id}", "s-1").header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/grupos/{id}", "g-1").header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void la_sesion_http_no_se_crea_al_autenticar_con_bearer() throws Exception {
        final var result = mockMvc.perform(
                        post("/api/v1/grupos").header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isOk())
                .andReturn();

        assertNull(result.getRequest().getSession(false));
    }

    @Test
    void peticion_insegura_con_bearer_y_cookie_sigue_permitida() throws Exception {
        mockMvc.perform(post("/api/v1/grupos")
                        .cookie(new Cookie("JSESSIONID", "session-de-prueba"))
                        .header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isOk());
    }

    @Test
    void cookie_y_bearer_invalido_no_permiten_acceso() throws Exception {
        mockMvc.perform(post("/api/v1/grupos")
                        .cookie(new Cookie("JSESSIONID", "session-de-prueba"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearer_vacio_o_malformado_no_permite_acceso() throws Exception {
        mockMvc.perform(post("/api/v1/grupos").header(HttpHeaders.AUTHORIZATION, "Bearer "))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/grupos").header(HttpHeaders.AUTHORIZATION, "Bearer"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/grupos").header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /api/v1/asistencias/lote : registro docente por lote ---

    @Test
    void registrar_asistencias_lote_estudiante_recibe_403() throws Exception {
        mockMvc.perform(post("/api/v1/asistencias/lote").header(HttpHeaders.AUTHORIZATION, bearer("ESTUDIANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrar_asistencias_lote_docente_es_permitido_por_security_filter_chain() throws Exception {
        mockMvc.perform(post("/api/v1/asistencias/lote").header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk());
    }

    // --- POST /api/v1/archivos/subir : subida de soportes (LB-004B.2, SEC-001/OWNERSHIP_DECISION) ---

    @Test
    void subir_archivo_sin_bearer_recibe_401() throws Exception {
        mockMvc.perform(post("/api/v1/archivos/subir"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void subir_archivo_estudiante_es_permitido_por_security_filter_chain() throws Exception {
        mockMvc.perform(post("/api/v1/archivos/subir").header(HttpHeaders.AUTHORIZATION, bearer("ESTUDIANTE")))
                .andExpect(status().isOk());
    }

    @Test
    void subir_archivo_docente_recibe_403() throws Exception {
        // OWNERSHIP_DECISION.md: DOCENTE upload = OUT_OF_SCOPE, sin capability.
        mockMvc.perform(post("/api/v1/archivos/subir").header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void subir_archivo_coordinador_recibe_403() throws Exception {
        mockMvc.perform(post("/api/v1/archivos/subir").header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isForbidden());
    }

    // --- /api/v1/estudiantes/** : directorio institucional de estudiantes:
    // abierto a DOCENTE para validar la existencia del estudiante a matricular en su grupo
    // (junto con COORDINADOR y ADMINISTRADOR). ---

    @Test
    void estudiantes_directorio_docente_es_permitido() throws Exception {
        mockMvc.perform(get("/api/v1/estudiantes").header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk());
    }

    @Test
    void estudiantes_directorio_coordinador_es_permitido() throws Exception {
        mockMvc.perform(get("/api/v1/estudiantes").header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isOk());
    }

    // --- /api/v1/estudiante/** : portal del propio estudiante ---

    @Test
    void portal_estudiante_docente_recibe_403() throws Exception {
        mockMvc.perform(get("/api/v1/estudiante/materias").header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void portal_estudiante_estudiante_es_permitido() throws Exception {
        mockMvc.perform(get("/api/v1/estudiante/materias").header(HttpHeaders.AUTHORIZATION, bearer("ESTUDIANTE")))
                .andExpect(status().isOk());
    }

    // --- GET /api/v1/docente/reclamos/{id} : detalle de solicitud de revision (HU036, SEC-01) ---

    @Test
    void detalle_reclamo_docente_es_permitido_por_security_filter_chain() throws Exception {
        mockMvc.perform(get("/api/v1/docente/reclamos/{id}", "33333333-3333-3333-3333-333333333333")
                        .header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk());
    }

    @Test
    void detalle_reclamo_docente_estudiante_recibe_403() throws Exception {
        mockMvc.perform(get("/api/v1/docente/reclamos/{id}", "33333333-3333-3333-3333-333333333333")
                        .header(HttpHeaders.AUTHORIZATION, bearer("ESTUDIANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void detalle_reclamo_docente_coordinador_recibe_403() throws Exception {
        mockMvc.perform(get("/api/v1/docente/reclamos/{id}", "33333333-3333-3333-3333-333333333333")
                        .header(HttpHeaders.AUTHORIZATION, bearer("COORDINADOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void detalle_reclamo_docente_sin_token_recibe_401() throws Exception {
        mockMvc.perform(get("/api/v1/docente/reclamos/{id}", "33333333-3333-3333-3333-333333333333"))
                .andExpect(status().isUnauthorized());
    }

    // --- GET /api/v1/realtime/stream : canal SSE, requiere autenticacion (cualquier rol) ---

    @Test
    void realtime_stream_sin_token_recibe_401() throws Exception {
        mockMvc.perform(get("/api/v1/realtime/stream"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void realtime_stream_con_bearer_invalido_recibe_401() throws Exception {
        mockMvc.perform(get("/api/v1/realtime/stream").header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void realtime_stream_cualquier_rol_autenticado_es_permitido() throws Exception {
        mockMvc.perform(get("/api/v1/realtime/stream").header(HttpHeaders.AUTHORIZATION, bearer("ESTUDIANTE")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/realtime/stream").header(HttpHeaders.AUTHORIZATION, bearer("DOCENTE")))
                .andExpect(status().isOk());
    }

    // --- POST /api/v1/realtime/emit : utilidad de desarrollo, restringida a ADMINISTRADOR ---

    @Test
    void realtime_emit_estudiante_recibe_403() throws Exception {
        mockMvc.perform(post("/api/v1/realtime/emit").header(HttpHeaders.AUTHORIZATION, bearer("ESTUDIANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void realtime_emit_administrador_es_permitido_por_security_filter_chain() throws Exception {
        mockMvc.perform(post("/api/v1/realtime/emit").header(HttpHeaders.AUTHORIZATION, bearer("ADMINISTRADOR")))
                .andExpect(status().isOk());
    }

    private static String bearer(final String institutionalRoleName) {
        return "Bearer " + institutionalRoleName;
    }

    private static Jwt jwtFor(final String institutionalRoleName) {
        final Instant now = Instant.now();
        return Jwt.withTokenValue(institutionalRoleName)
                .header("alg", "none")
                .issuer(ISSUER)
                .subject("keycloak-sub")
                .issuedAt(now.minusSeconds(60))
                .notBefore(now.minusSeconds(60))
                .expiresAt(now.plusSeconds(300))
                .audience(List.of(AUDIENCE))
                .claim(USER_ID_CLAIM, USER_ID)
                .claim("resource_access", Map.of(API_CLIENT_ID, Map.of("roles", List.of(institutionalRoleName))))
                .build();
    }

    @RestController
    static final class RbacProbeController {

        @GetMapping("/swagger-ui/index.html")
        String swaggerUi() {
            return "ok";
        }

        @GetMapping("/openapi/openapi-golden-path.yaml")
        String canonicalOpenApi() {
            return "openapi: 3.1.2";
        }

        @GetMapping("/api/v1/docentes")
        String docentes() {
            return "ok";
        }

        @GetMapping({"/api/v1/coordinador/indicadores", "/api/v1/coordinador/alertas-tempranas",
                "/api/v1/coordinador/estudiantes/{id}/bitacora", "/api/v1/coordinador/estudiantes/{id}/ficha"})
        String indicadoresCoordinador() {
            return "ok";
        }

        @GetMapping({"/api/v1/admin/indicadores", "/api/v1/admin/comparativa-facultades", "/api/v1/admin/tendencia-ausentismo"})
        String indicadoresAdmin() {
            return "ok";
        }

        @GetMapping({"/api/v1/decano/indicadores", "/api/v1/decano/comparativa-programas", "/api/v1/decano/aforo-grupos"})
        String indicadoresDecano() {
            return "ok";
        }

        @GetMapping({"/api/v1/coordinador/docentes", "/api/v1/coordinador/docentes/candidatos", "/api/v1/tipos-vinculacion"})
        String docentesPrograma() {
            return "ok";
        }

        @PostMapping("/api/v1/coordinador/docentes")
        String vincularDocente() {
            return "ok";
        }

        @org.springframework.web.bind.annotation.PatchMapping("/api/v1/coordinador/docentes/{id}/estado")
        String estadoDocente() {
            return "ok";
        }

        @org.springframework.web.bind.annotation.PutMapping("/api/v1/coordinador/docentes/{id}")
        String actualizarDocente() {
            return "ok";
        }

        @org.springframework.web.bind.annotation.RequestMapping(
                path = {"/api/v1/admin/periodos-academicos", "/api/v1/admin/periodos-academicos/{id}"},
                method = {org.springframework.web.bind.annotation.RequestMethod.POST,
                        org.springframework.web.bind.annotation.RequestMethod.PUT,
                        org.springframework.web.bind.annotation.RequestMethod.DELETE})
        String gestionarPeriodoAcademico() {
            return "ok";
        }

        @GetMapping("/api/v1/estudiantes")
        String estudiantes() {
            return "ok";
        }

        @GetMapping("/api/v1/grupos")
        String listarGrupos() {
            return "ok";
        }

        @GetMapping("/api/v1/grupos/{grupoId}/estudiantes")
        String estudiantesDeGrupo() {
            return "ok";
        }

        @GetMapping("/api/v1/sesiones/grupo/{grupoId}")
        String sesionesPorGrupo() {
            return "ok";
        }

        @PutMapping("/api/v1/sesiones/{sesionId}")
        String actualizarSesionLegacy() {
            return "ok";
        }

        @PatchMapping("/api/v1/sesiones/{sesionId}")
        String actualizarSesion() {
            return "ok";
        }

        @PostMapping("/api/v1/grupos")
        String crearGrupo() {
            return "ok";
        }

        @PostMapping("/api/v1/grupos/{grupoId}/estudiantes")
        String matricularEstudiante() {
            return "ok";
        }

        @PostMapping({"/api/v1/grupos/{grupoId}/matricula-qr", "/api/v1/grupos/{grupoId}/matricula-qr/rotacion",
                "/api/v1/public/matricula-qr/validar", "/api/v1/public/matricula-qr/registro",
                "/api/v1/estudiante/matricular-grupo"})
        String matriculaQr() {
            return "ok";
        }

        @DeleteMapping("/api/v1/grupos/{grupoId}/matricula-qr")
        String cerrarMatriculaQr() {
            return "ok";
        }

        @DeleteMapping("/api/v1/grupos/{grupoId}")
        String eliminarGrupo() {
            return "ok";
        }

        @PostMapping("/api/v1/asistencias/lote")
        String registrarAsistenciasLote() {
            return "ok";
        }

        @GetMapping("/api/v1/estudiante/materias")
        String materiasEstudiante() {
            return "ok";
        }

        @GetMapping("/api/v1/docente/reclamos/{id}")
        String detalleReclamoDocente() {
            return "ok";
        }

        @GetMapping("/api/v1/realtime/stream")
        String realtimeStream() {
            return "ok";
        }

        @PostMapping("/api/v1/realtime/emit")
        String realtimeEmit() {
            return "ok";
        }

        @PostMapping("/api/v1/archivos/subir")
        String subirArchivo() {
            return "ok";
        }
    }

    @TestConfiguration
    static class RbacTestSupportConfig {

        @Bean
        JwtClaimsExtractor jwtClaimsExtractor() {
            return new KeycloakJwtClaimsExtractor(API_CLIENT_ID, USER_ID_CLAIM);
        }

        @Bean
        JwtDecoder jwtDecoder() {
            final OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(AUDIENCE);
            return token -> {
                if (!List.of("COORDINADOR", "ADMINISTRADOR", "DOCENTE", "ESTUDIANTE", "DECANO").contains(token)) {
                    throw new BadJwtException("Invalid test token.");
                }
                final Jwt jwt = jwtFor(token);
                final OAuth2TokenValidatorResult result = audienceValidator.validate(jwt);
                if (result.hasErrors()) {
                    throw new BadJwtException("Invalid test token.");
                }
                return jwt;
            };
        }

        @Bean
        co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort resolverMensajeUsuarioInputPort() {
            return codigo -> java.util.Optional.empty();
        }

        @Bean
        AuditEventPublisher auditEventPublisher() {
            return event -> {
            };
        }
    }
}
