package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.identity.keycloak;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.ActualizarCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IAM-001 §2 (consumido por HU121/HU122): ciclo de vida de la cuenta en Keycloak. Mismo servidor falso HTTP real
 * que {@link KeycloakIdentityProviderAdapterTest}; el adapter no se conecta a ningun Keycloak real.
 */
class KeycloakIdentityProviderAdapterGestionCuentaTest {

    private static final String REALM = "test-realm";
    private static final String API_CLIENT_ID = "asistencias-api";
    private static final String USER_ID_ATTRIBUTE = "idUsuario";
    private static final UUID ID_USUARIO = UUID.fromString("93641bab-e3cd-485c-b275-47e7b731e18c");
    private static final String EXTERNAL_ID = "kc-user-1";
    private static final String CLIENT_UUID = "kc-client-uuid-1";
    private static final InstitutionalRole ROLE = InstitutionalRole.COORDINADOR;
    private static final String ROLE_NAME = ROLE.name();

    private static final String TOKEN_PATH = "/realms/" + REALM + "/protocol/openid-connect/token";
    private static final String USERS_PATH = "/admin/realms/" + REALM + "/users";
    private static final String USER_PATH = USERS_PATH + "/" + EXTERNAL_ID;
    private static final String LOGOUT_PATH = USER_PATH + "/logout";
    private static final String SEARCH_PATH = USERS_PATH + "?q=" + USER_ID_ATTRIBUTE + "%3A" + ID_USUARIO;
    private static final String CLIENTS_SEARCH_PATH = "/admin/realms/" + REALM + "/clients?clientId=" + API_CLIENT_ID;
    private static final String ROLE_PATH = "/admin/realms/" + REALM + "/clients/" + CLIENT_UUID + "/roles/" + ROLE_NAME;
    private static final String ROLE_MAPPINGS_PATH = USER_PATH + "/role-mappings/clients/" + CLIENT_UUID;

    private static final String ROLE_JSON = "{\"id\":\"role-id-1\",\"name\":\"" + ROLE_NAME + "\",\"clientRole\":true}";
    private static final String USER_JSON = "[{\"id\":\"" + EXTERNAL_ID + "\",\"username\":\"jperez\","
            + "\"email\":\"jperez@uco.edu.co\",\"attributes\":{\"" + USER_ID_ATTRIBUTE + "\":[\"" + ID_USUARIO + "\"]}}]";

    private FakeKeycloakAdminServer server;
    private KeycloakIdentityProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        server = new FakeKeycloakAdminServer();
        adapter = new KeycloakIdentityProviderAdapter(
                server.baseUrl(), REALM, "admin-client", "s3cr3t-value", API_CLIENT_ID, USER_ID_ATTRIBUTE);
        server.stubJson("POST", TOKEN_PATH, 200, "{\"access_token\":\"fake-admin-token\"}");
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private void stubClienteYRol() {
        server.stubJson("GET", CLIENTS_SEARCH_PATH, 200, "[{\"id\":\"" + CLIENT_UUID + "\",\"clientId\":\"" + API_CLIENT_ID + "\"}]");
        server.stubJson("GET", ROLE_PATH, 200, ROLE_JSON);
    }

    // ---------------------------------------------------------------- buscarCuentaPorIdUsuario

    @Test
    void buscar_devuelve_la_cuenta_cuando_hay_una_coincidencia_exacta_por_atributo() {
        server.stubJson("GET", SEARCH_PATH, 200, USER_JSON);

        final Optional<CuentaIdentidadDTO> cuenta = adapter.buscarCuentaPorIdUsuario(ID_USUARIO);

        assertTrue(cuenta.isPresent());
        assertEquals(EXTERNAL_ID, cuenta.get().idExterno());
        assertFalse(cuenta.get().newlyCreated());
        assertEquals("Bearer fake-admin-token", server.lastAuthorizationHeader("GET", SEARCH_PATH));
    }

    @Test
    void buscar_devuelve_vacio_cuando_no_hay_coincidencias() {
        server.stubJson("GET", SEARCH_PATH, 200, "[]");

        assertTrue(adapter.buscarCuentaPorIdUsuario(ID_USUARIO).isEmpty());
    }

    @Test
    void buscar_con_mas_de_una_coincidencia_es_identidad_ambigua() {
        server.stubJson("GET", SEARCH_PATH, 200, "[" + USER_JSON.substring(1, USER_JSON.length() - 1) + ","
                + USER_JSON.substring(1, USER_JSON.length() - 1).replace(EXTERNAL_ID, "kc-user-2") + "]");

        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.buscarCuentaPorIdUsuario(ID_USUARIO));
    }

    @Test
    void buscar_ignora_candidatos_cuyo_atributo_no_coincide_exactamente() {
        server.stubJson("GET", SEARCH_PATH, 200, USER_JSON.replace(ID_USUARIO.toString(), UUID.randomUUID().toString()));

        assertTrue(adapter.buscarCuentaPorIdUsuario(ID_USUARIO).isEmpty());
    }

    @Test
    void buscar_con_respuesta_no_200_falla() {
        server.stubJson("GET", SEARCH_PATH, 500, "{}");

        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.buscarCuentaPorIdUsuario(ID_USUARIO));
    }

    // ---------------------------------------------------------------- deshabilitar / habilitar

    @Test
    void deshabilitar_envia_enabled_false_y_cierra_las_sesiones() {
        server.stubNoContent("PUT", USER_PATH, 204);
        server.stubNoContent("POST", LOGOUT_PATH, 204);

        adapter.deshabilitarCuenta(EXTERNAL_ID);

        assertTrue(server.lastRequestBody("PUT", USER_PATH).replace(" ", "").contains("\"enabled\":false"));
        assertTrue(server.wasCalled("POST", LOGOUT_PATH));
        assertEquals("Bearer fake-admin-token", server.lastAuthorizationHeader("PUT", USER_PATH));
    }

    @Test
    void deshabilitar_no_falla_si_el_cierre_de_sesiones_falla_porque_la_cuenta_ya_quedo_deshabilitada() {
        server.stubNoContent("PUT", USER_PATH, 204);
        server.stubNoContent("POST", LOGOUT_PATH, 500);

        assertDoesNotThrow(() -> adapter.deshabilitarCuenta(EXTERNAL_ID));
    }

    @Test
    void deshabilitar_cuenta_inexistente_falla() {
        server.stubNoContent("PUT", USER_PATH, 404);

        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.deshabilitarCuenta(EXTERNAL_ID));
        assertFalse(server.wasCalled("POST", LOGOUT_PATH));
    }

    @Test
    void habilitar_envia_enabled_true() {
        server.stubNoContent("PUT", USER_PATH, 204);

        adapter.habilitarCuenta(EXTERNAL_ID);

        assertTrue(server.lastRequestBody("PUT", USER_PATH).replace(" ", "").contains("\"enabled\":true"));
    }

    @Test
    void habilitar_cuenta_inexistente_falla() {
        server.stubNoContent("PUT", USER_PATH, 404);

        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.habilitarCuenta(EXTERNAL_ID));
    }

    @Test
    void id_externo_invalido_se_rechaza_sin_llamar_a_keycloak() {
        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.deshabilitarCuenta(" "));
        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.habilitarCuenta("a/b"));
        assertThrows(IdentityProviderPort.IdentityProviderException.class,
                () -> adapter.actualizarDatosCuenta(null, new ActualizarCuentaIdentidadDTO("a@uco.edu.co", "Ana", "Perez")));
        assertFalse(server.wasCalled("POST", TOKEN_PATH));
    }

    // ---------------------------------------------------------------- actualizarDatosCuenta

    @Test
    void actualizar_datos_envia_correo_nombres_y_correo_verificado_sin_tocar_username_ni_password() {
        server.stubNoContent("PUT", USER_PATH, 204);

        adapter.actualizarDatosCuenta(EXTERNAL_ID, new ActualizarCuentaIdentidadDTO("ana@uco.edu.co", "Ana", "Gomez"));

        final String body = server.lastRequestBody("PUT", USER_PATH).replace(" ", "");
        assertTrue(body.contains("\"email\":\"ana@uco.edu.co\""));
        assertTrue(body.contains("\"firstName\":\"Ana\""));
        assertTrue(body.contains("\"lastName\":\"Gomez\""));
        assertTrue(body.contains("\"emailVerified\":true"));
        assertFalse(body.contains("username"));
        assertFalse(body.contains("credentials"));
    }

    @Test
    void actualizar_datos_con_correo_en_uso_en_el_idp_falla_por_conflicto() {
        server.stubNoContent("PUT", USER_PATH, 409);

        final IdentityProviderPort.IdentityProviderException exception = assertThrows(
                IdentityProviderPort.IdentityProviderException.class,
                () -> adapter.actualizarDatosCuenta(EXTERNAL_ID, new ActualizarCuentaIdentidadDTO("ana@uco.edu.co", "Ana", "Gomez")));

        assertTrue(exception.getMessage().toLowerCase().contains("conflicto"));
    }

    @Test
    void actualizar_datos_exige_el_dto() {
        assertThrows(NullPointerException.class, () -> adapter.actualizarDatosCuenta(EXTERNAL_ID, null));
    }

    @Test
    void HU159_actualizar_datos_con_documento_corregido_envia_el_username_y_lo_verifica() {
        server.stubNoContent("PUT", USER_PATH, 204);
        server.stubJson("GET", USER_PATH, 200, "{\"id\":\"" + EXTERNAL_ID + "\",\"username\":\"915900011\",\"email\":\"ana@uco.edu.co\"}");

        adapter.actualizarDatosCuenta(EXTERNAL_ID, new ActualizarCuentaIdentidadDTO("ana@uco.edu.co", "Ana", "Gomez", " 915900011 "));

        final String body = server.lastRequestBody("PUT", USER_PATH).replace(" ", "");
        assertTrue(body.contains("\"username\":\"915900011\""));
        assertFalse(body.contains("credentials"));
        assertTrue(server.wasCalled("GET", USER_PATH));
    }

    @Test
    void HU159_si_keycloak_no_aplica_el_username_la_actualizacion_falla_para_registrar_el_desfase() {
        server.stubNoContent("PUT", USER_PATH, 204);
        server.stubJson("GET", USER_PATH, 200, "{\"id\":\"" + EXTERNAL_ID + "\",\"username\":\"111\",\"email\":\"ana@uco.edu.co\"}");

        assertThrows(IdentityProviderPort.IdentityProviderException.class,
                () -> adapter.actualizarDatosCuenta(EXTERNAL_ID, new ActualizarCuentaIdentidadDTO("ana@uco.edu.co", "Ana", "Gomez", "915900011")));
    }

    @Test
    void HU159_username_en_blanco_se_trata_como_sin_cambio_de_documento() {
        server.stubNoContent("PUT", USER_PATH, 204);

        adapter.actualizarDatosCuenta(EXTERNAL_ID, new ActualizarCuentaIdentidadDTO("ana@uco.edu.co", "Ana", "Gomez", "  "));

        assertFalse(server.lastRequestBody("PUT", USER_PATH).contains("username"));
        assertFalse(server.wasCalled("GET", USER_PATH));
    }

    // ---------------------------------------------------------------- retirarRol

    @Test
    void retirar_rol_asignado_elimina_el_client_role_con_la_representacion_exacta() {
        stubClienteYRol();
        server.stubJson("GET", ROLE_MAPPINGS_PATH, 200, "[" + ROLE_JSON + "]");
        server.stubNoContent("DELETE", ROLE_MAPPINGS_PATH, 204);

        adapter.retirarRol(EXTERNAL_ID, ROLE);

        assertTrue(server.wasCalled("DELETE", ROLE_MAPPINGS_PATH));
        assertTrue(server.lastRequestBody("DELETE", ROLE_MAPPINGS_PATH).contains("\"id\":\"role-id-1\""));
    }

    @Test
    void retirar_rol_que_no_tiene_es_idempotente_y_no_llama_a_delete() {
        stubClienteYRol();
        server.stubJson("GET", ROLE_MAPPINGS_PATH, 200, "[]");

        assertDoesNotThrow(() -> adapter.retirarRol(EXTERNAL_ID, ROLE));

        assertFalse(server.wasCalled("DELETE", ROLE_MAPPINGS_PATH));
    }

    @Test
    void retirar_rol_con_respuesta_de_delete_inesperada_falla() {
        stubClienteYRol();
        server.stubJson("GET", ROLE_MAPPINGS_PATH, 200, "[" + ROLE_JSON + "]");
        server.stubNoContent("DELETE", ROLE_MAPPINGS_PATH, 500);

        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.retirarRol(EXTERNAL_ID, ROLE));
    }

    @Test
    void retirar_rol_exige_el_rol() {
        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> adapter.retirarRol(EXTERNAL_ID, null));
    }
}
