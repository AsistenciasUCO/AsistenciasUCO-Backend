package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr;

import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.AbrirVentanaMatriculaInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.CerrarVentanaMatriculaInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.MatricularEstudianteAutenticadoInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.RegistrarEstudiantePorTokenInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.RotarVentanaMatriculaInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.ValidarTokenMatriculaInputPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.MatriculaQrRateLimitFilter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.NetworkClientIpResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter.SlidingWindowRateLimiter;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU176B: contrato HTTP de los tres controllers y del filtro de limite de intentos. El token solo sale en la respuesta de
 * apertura/rotacion; los cuerpos publicos se validan en forma antes de llegar al caso de uso.
 */
class MatriculaQrControllersTest {

    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ACTOR = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID VENTANA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String TOKEN = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-AbCde";

    private AbrirVentanaMatriculaInputPort abrir;
    private RotarVentanaMatriculaInputPort rotar;
    private CerrarVentanaMatriculaInputPort cerrar;
    private ValidarTokenMatriculaInputPort validar;
    private RegistrarEstudiantePorTokenInputPort registrar;
    private MatricularEstudianteAutenticadoInputPort matricular;
    private co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.ConsultarTiposIdentificacionInputPort tipos;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        abrir = mock(AbrirVentanaMatriculaInputPort.class);
        rotar = mock(RotarVentanaMatriculaInputPort.class);
        cerrar = mock(CerrarVentanaMatriculaInputPort.class);
        validar = mock(ValidarTokenMatriculaInputPort.class);
        registrar = mock(RegistrarEstudiantePorTokenInputPort.class);
        matricular = mock(MatricularEstudianteAutenticadoInputPort.class);
        tipos = mock(co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.ConsultarTiposIdentificacionInputPort.class);
        when(tipos.execute()).thenReturn(java.util.List.of(
                new co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO(
                        UUID.fromString("99999999-9999-9999-9999-999999999999"), "CC", "Cedula de Ciudadania")));
        final AuthenticatedUserResolver usuario = () -> ACTOR;
        final NetworkClientIpResolver ip = new NetworkClientIpResolver(Optional::empty);
        mvc = MockMvcBuilders.standaloneSetup(
                new GrupoMatriculaQrController(abrir, rotar, cerrar, usuario),
                new MatriculaQrPublicaController(validar, registrar, tipos, ip),
                new EstudianteMatriculaQrController(matricular, ip, usuario)
        ).build();
    }

    private static AbrirVentanaMatriculaInputPort.Resultado ventana() {
        return new AbrirVentanaMatriculaInputPort.Resultado(
                VENTANA, "482913", "https://x.test/matricula/qr?t=" + TOKEN, "iVBORw0",
                Instant.parse("2026-11-04T15:00:00Z"), Instant.parse("2026-11-04T16:00:00Z"));
    }

    @Test
    void T01_abrir_responde_201_con_pin_url_qr_y_fechas() throws Exception {
        when(abrir.execute(new AbrirVentanaMatriculaInputPort.Comando(GRUPO, ACTOR))).thenReturn(ventana());

        mvc.perform(post("/api/v1/grupos/{g}/matricula-qr", GRUPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.exitoso").value(true))
                .andExpect(jsonPath("$.datos.idVentana").value(VENTANA.toString()))
                .andExpect(jsonPath("$.datos.pin").value("482913"))
                .andExpect(jsonPath("$.datos.qrPngBase64").value("iVBORw0"))
                .andExpect(jsonPath("$.datos.urlMatricula").value("https://x.test/matricula/qr?t=" + TOKEN));
    }

    @Test
    void T40_rotacion_manual_responde_200_con_el_mismo_cuerpo_que_abrir() throws Exception {
        when(rotar.execute(new RotarVentanaMatriculaInputPort.Comando(GRUPO, ACTOR))).thenReturn(ventana());

        mvc.perform(post("/api/v1/grupos/{g}/matricula-qr/rotacion", GRUPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.pin").value("482913"));
    }

    @Test
    void T05_cerrar_responde_200() throws Exception {
        mvc.perform(delete("/api/v1/grupos/{g}/matricula-qr", GRUPO)).andExpect(status().isOk());

        verify(cerrar).execute(new CerrarVentanaMatriculaInputPort.Comando(GRUPO, ACTOR));
    }

    @Test
    void validar_publico_responde_200_con_los_datos_minimos_del_curso() throws Exception {
        when(validar.execute(new ValidarTokenMatriculaInputPort.Comando(TOKEN, null, "127.0.0.1")))
                .thenReturn(new ValidarTokenMatriculaInputPort.Resultado(GRUPO, "Calculo", "G1", "Maria Perez",
                        Instant.parse("2026-11-04T16:00:00Z"), 7));

        mvc.perform(post("/api/v1/public/matricula-qr/validar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + TOKEN + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.datos.nombreAsignatura").value("Calculo"))
                .andExpect(jsonPath("$.datos.cupoDisponible").value(7))
                .andExpect(jsonPath("$.datos.tiposIdentificacion[0].tipoIdentificacion").value("CC"));
    }

    @Test
    void T30_validar_con_formato_de_token_o_pin_invalido_responde_400_sin_llegar_al_caso_de_uso() {
        exigirRechazoDeValidacion("/api/v1/public/matricula-qr/validar", "{\"token\":\"corto\"}");
        exigirRechazoDeValidacion("/api/v1/public/matricula-qr/validar", "{\"pin\":\"12ab56\"}");

        verify(validar, never()).execute(any());
    }

    @Test
    void T10_registro_publico_responde_201_con_el_usuario_y_sin_token_de_sesion() throws Exception {
        when(registrar.execute(any())).thenReturn(new RegistrarEstudiantePorTokenInputPort.Resultado(GRUPO, "1017555444"));

        mvc.perform(post("/api/v1/public/matricula-qr/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("Clave-segura-1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.usuario").value("1017555444"))
                .andExpect(jsonPath("$.datos.token").doesNotExist())
                .andExpect(jsonPath("$.datos.accessToken").doesNotExist());
    }

    @Test
    void T30_registro_con_contrasena_corta_o_documento_fuera_de_rango_responde_400() {
        exigirRechazoDeValidacion("/api/v1/public/matricula-qr/registro", registroJson("corta"));
        exigirRechazoDeValidacion("/api/v1/public/matricula-qr/registro", registroJson("Clave-segura-1").replace("1017555444", "99999999999"));

        verify(registrar, never()).execute(any());
    }

    @Test
    void validador_cubre_todos_los_campos_y_limites_del_registro_y_credencial() {
        final var validator = new co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr.validation.MatriculaQrRequestValidator();
        org.junit.jupiter.api.Assertions.assertFalse(validator.validarCredencial(null).isValid());
        org.junit.jupiter.api.Assertions.assertFalse(validator.validarRegistro(null).isValid());

        final String largo51 = "a".repeat(51);
        final String correoLargo = "a".repeat(95) + "@uco.edu.co";
        final String passLarga = "a".repeat(256);
        final var invalido = new co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr.request.MatriculaQrRequests.Registro(
                TOKEN, null, null, 50L, largo51, largo51, "", null, correoLargo, passLarga);
        org.junit.jupiter.api.Assertions.assertFalse(validator.validarRegistro(invalido).isValid());

        final var vacios = new co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr.request.MatriculaQrRequests.Registro(
                null, "123456", null, null, null, "Segundo", "Apellido", "SegundoAp", "sin-arroba", "");
        org.junit.jupiter.api.Assertions.assertFalse(validator.validarRegistro(vacios).isValid());

        final var sinCorreo = new co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.matriculaqr.request.MatriculaQrRequests.Registro(
                TOKEN, null, UUID.randomUUID(), 1017555444L, "Ana", null, "Gomez", null, " ", "Clave-segura-1");
        org.junit.jupiter.api.Assertions.assertFalse(validator.validarRegistro(sinCorreo).isValid());
    }

    @Test
    void T16_matricular_grupo_autenticado_responde_201_con_el_grupo() throws Exception {
        when(matricular.execute(new MatricularEstudianteAutenticadoInputPort.Comando(null, "482913", "127.0.0.1", ACTOR)))
                .thenReturn(new MatricularEstudianteAutenticadoInputPort.Resultado(GRUPO));

        mvc.perform(post("/api/v1/estudiante/matricular-grupo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pin\":\"482913\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.datos.idGrupo").value(GRUPO.toString()));
    }

    /**
     * Sin el GlobalExceptionHandler (que la aplicacion traduce a 400) la validacion de forma sale como
     * RequestValidationException; lo que se verifica aqui es que el rechazo ocurre ANTES del caso de uso.
     */
    private void exigirRechazoDeValidacion(final String ruta, final String cuerpo) {
        final Exception error = org.junit.jupiter.api.Assertions.assertThrows(Exception.class,
                () -> mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo)));
        org.junit.jupiter.api.Assertions.assertInstanceOf(
                co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.error.RequestValidationException.class,
                error.getCause());
    }

    private static String registroJson(final String password) {
        return "{\"token\":\"" + TOKEN + "\",\"idTipoIdentificacion\":\"" + UUID.randomUUID()
                + "\",\"numeroIdentificacion\":1017555444,\"primerNombre\":\"Ana\",\"primerApellido\":\"Gomez\","
                + "\"correo\":\"ana@uco.edu.co\",\"password\":\"" + password + "\"}";
    }

    // ------------------------------------------------------------------ filtro de limite de intentos

    private static final class RelojFijo extends Clock {
        @Override
        public java.time.ZoneId getZone() {
            return java.time.ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.parse("2026-11-04T15:00:00Z");
        }
    }

    @Test
    void T23_el_filtro_responde_429_al_superar_el_limite_por_ip_y_no_llega_a_la_cadena() throws Exception {
        final SecurityErrorResponseWriter writer = mock(SecurityErrorResponseWriter.class);
        final MatriculaQrRateLimitFilter filtro = new MatriculaQrRateLimitFilter(
                new SlidingWindowRateLimiter(Duration.ofSeconds(60), 100, new RelojFijo()),
                () -> 2, new NetworkClientIpResolver(Optional::empty), writer);

        for (int i = 0; i < 2; i++) {
            final MockFilterChain cadena = new MockFilterChain();
            filtro.doFilter(peticion("/api/v1/public/matricula-qr/validar", "POST"), new MockHttpServletResponse(), cadena);
            assertEquals(true, cadena.getRequest() != null, "el intento " + (i + 1) + " debia pasar");
        }
        final MockFilterChain bloqueada = new MockFilterChain();
        final MockHttpServletRequest tercera = peticion("/api/v1/public/matricula-qr/registro", "POST");
        final MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filtro.doFilter(tercera, respuesta, bloqueada);

        assertNull(bloqueada.getRequest(), "el tercer intento no debia llegar a la cadena");
        verify(writer).write(tercera, respuesta, HttpStatus.TOO_MANY_REQUESTS, SecurityErrorCode.TOO_MANY_REQUESTS);
    }

    @Test
    void el_filtro_ignora_otras_rutas_y_otros_metodos() throws Exception {
        final SecurityErrorResponseWriter writer = mock(SecurityErrorResponseWriter.class);
        final MatriculaQrRateLimitFilter filtro = new MatriculaQrRateLimitFilter(
                new SlidingWindowRateLimiter(Duration.ofSeconds(60), 100, new RelojFijo()),
                () -> 1, new NetworkClientIpResolver(Optional::empty), writer);

        for (int i = 0; i < 5; i++) {
            final MockFilterChain cadena = new MockFilterChain();
            filtro.doFilter(peticion("/api/v1/grupos", "POST"), new MockHttpServletResponse(), cadena);
            assertEquals(true, cadena.getRequest() != null);
            final MockFilterChain cadenaGet = new MockFilterChain();
            filtro.doFilter(peticion("/api/v1/public/matricula-qr/validar", "GET"), new MockHttpServletResponse(), cadenaGet);
            assertEquals(true, cadenaGet.getRequest() != null);
        }
        verify(writer, never()).write(any(), any(), any(), any());
    }

    @Test
    void un_limite_configurado_en_cero_o_negativo_se_trata_como_uno() throws Exception {
        final SecurityErrorResponseWriter writer = mock(SecurityErrorResponseWriter.class);
        final MatriculaQrRateLimitFilter filtro = new MatriculaQrRateLimitFilter(
                new SlidingWindowRateLimiter(Duration.ofSeconds(60), 100, new RelojFijo()),
                () -> 0, new NetworkClientIpResolver(Optional::empty), writer);

        final MockFilterChain primera = new MockFilterChain();
        filtro.doFilter(peticion("/api/v1/public/matricula-qr/validar", "POST"), new MockHttpServletResponse(), primera);
        assertEquals(true, primera.getRequest() != null);
        final MockFilterChain segunda = new MockFilterChain();
        filtro.doFilter(peticion("/api/v1/public/matricula-qr/validar", "POST"), new MockHttpServletResponse(), segunda);
        assertNull(segunda.getRequest());
    }

    @Test
    void wiring_crea_todos_los_beans_y_resuelve_el_limite_por_ip_con_respaldo() throws Exception {
        final var config = new co.edu.uco.asistenciasuco.infrastructure.config.wiring.MatriculaQrWiringConfiguration();
        final var catalogo = mock(co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort.class);
        @SuppressWarnings("unchecked")
        final org.springframework.beans.factory.ObjectProvider<co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort> provider =
                mock(org.springframework.beans.factory.ObjectProvider.class);
        when(provider.getObject()).thenReturn(catalogo);

        final var tokenGen = config.tokenGeneratorPort();
        final var qrGen = config.codigoQrGeneratorPort();
        final var redPort = config.redInstitucionalPort(catalogo);
        final var ipResolver = config.networkClientIpResolver(provider);
        final var acceso = config.matriculaQrAccesoPolicy(catalogo, redPort);
        final var emisor = config.matriculaQrCredencialesEmisor(tokenGen);
        final var presentador = config.matriculaQrPresentador(catalogo, qrGen);
        final var scope = mock(co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort.class);
        final var repo = mock(co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort.class);
        final var correoPolicy = mock(co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy.class);
        final var encoder = mock(co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort.class);
        final var idp = mock(co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort.class);
        final var notif = mock(co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort.class);
        final var realtime = mock(co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort.class);
        final var writer = mock(SecurityErrorResponseWriter.class);

        assertNotNull(config.abrirVentanaMatriculaInputPort(acceso, scope, emisor, presentador, repo));
        assertNotNull(config.rotarVentanaMatriculaInputPort(acceso, scope, emisor, presentador, repo));
        assertNotNull(config.cerrarVentanaMatriculaInputPort(scope, repo));
        assertNotNull(config.validarTokenMatriculaInputPort(acceso, repo));
        assertNotNull(config.registrarEstudiantePorTokenInputPort(acceso, correoPolicy, encoder, repo, idp, notif, realtime));
        assertNotNull(config.matricularEstudianteAutenticadoInputPort(acceso, repo, realtime));
        final var rotarAuto = config.rotarTokensMatriculaAutomaticoInputPort(acceso, emisor, presentador, repo, realtime);
        assertNotNull(config.rotacionTokenMatriculaScheduler(rotarAuto, Duration.ofSeconds(5)));

        final var reg = config.matriculaQrRateLimitFilter(provider, ipResolver, writer, 100);
        assertNotNull(reg.getFilter());
        when(catalogo.getParameter("MATRICULA_QR", "LIMITE_IP_POR_MINUTO")).thenReturn(Optional.of("1"));
        reg.getFilter().doFilter(peticion("/api/v1/public/matricula-qr/validar", "POST"), new MockHttpServletResponse(), new MockFilterChain());
        when(catalogo.getParameter("MATRICULA_QR", "LIMITE_IP_POR_MINUTO")).thenReturn(Optional.of("invalido"));
        reg.getFilter().doFilter(peticion("/api/v1/public/matricula-qr/validar", "POST"), new MockHttpServletResponse(), new MockFilterChain());
    }

    private static MockHttpServletRequest peticion(final String uri, final String metodo) {
        final MockHttpServletRequest request = new MockHttpServletRequest(metodo, uri);
        request.setRemoteAddr("10.1.2.3");
        return request;
    }
}
