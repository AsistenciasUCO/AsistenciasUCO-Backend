package co.edu.uco.asistenciasuco.application.features.asistenciaqr;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.business.TooManyRequestsException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.network.RedInstitucionalPort;
import co.edu.uco.asistenciasuco.application.secondaryports.qr.CodigoQrGeneratorPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaQrRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.LimitadorIntentosPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.TokenGeneratorPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.abrirqrtoma.primaryports.AbrirQrTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.abrirqrtoma.usecase.impl.AbrirQrTomaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.cerrarqrtoma.primaryports.CerrarQrTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.cerrarqrtoma.usecase.impl.CerrarQrTomaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrAccesoPolicy;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrCredencialesEmisor;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrEventos;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.AsistenciaQrPresentador;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.EsperaReintento;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.common.QrTomaResumen;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.registrarasistenciaportoken.primaryports.RegistrarAsistenciaPorTokenInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.registrarasistenciaportoken.usecase.impl.RegistrarAsistenciaPorTokenUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotarqrtoma.primaryports.RotarQrTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotarqrtoma.usecase.impl.RotarQrTomaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.rotartokensasistenciaautomatico.usecase.impl.RotarTokensAsistenciaAutomaticoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaAcceso;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaEventos;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * CULM-001 B.2 (CONTRACT 3.2.6, 3.2.7, 3.5, 3.8, 3.11): QR/PIN rotativo de asistencia. Interruptor, red (CIDR fail-closed),
 * limites por estudiante y por token (todos los intentos), reintento AQR_006 (5) y de interbloqueo (3), rotado = false
 * sin evento, yaRegistrado = true sin evento y payloads exactos.
 */
class AsistenciaQrUseCasesTest {

    private static final UUID USUARIO = UUID.fromString("11111111-0000-0000-0000-000000000001");
    private static final UUID SESION = UUID.fromString("22222222-0000-0000-0000-000000000002");
    private static final UUID GRUPO = UUID.fromString("33333333-0000-0000-0000-000000000003");
    private static final UUID TOMA = UUID.fromString("44444444-0000-0000-0000-000000000004");
    private static final UUID TOKEN_ID = UUID.fromString("55555555-0000-0000-0000-000000000005");
    private static final UUID DOCENTE = UUID.fromString("66666666-0000-0000-0000-000000000006");
    private static final UUID ESTUDIANTE = UUID.fromString("77777777-0000-0000-0000-000000000007");
    private static final String TOKEN = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-AbCde";
    private static final Instant ROTACION = Instant.parse("2026-11-07T13:10:00Z");
    private static final Instant EXPIRACION = Instant.parse("2026-11-07T15:30:00Z");
    private static final String IP = "10.20.0.15";

    private ParameterCatalogPort parametros;
    private RedInstitucionalPort red;
    private TokenGeneratorPort generador;
    private CodigoQrGeneratorPort qr;
    private AsistenciaQrRepositoryPort repositorio;
    private TomaAsistenciaRepositoryPort tomasRepo;
    private InstitutionalScopePort scope;
    private RealtimePublisherPort realtime;
    private LimitadorIntentosPort limitador;
    private AsistenciaQrAccesoPolicy acceso;
    private AsistenciaQrCredencialesEmisor emisor;
    private AsistenciaQrPresentador presentador;
    private AsistenciaQrEventos eventos;
    private TomaAsistenciaAcceso tomaAcceso;
    private final List<Long> esperas = new ArrayList<>();

    @BeforeEach
    void preparar() {
        parametros = mock(ParameterCatalogPort.class);
        red = mock(RedInstitucionalPort.class);
        generador = mock(TokenGeneratorPort.class);
        qr = mock(CodigoQrGeneratorPort.class);
        repositorio = mock(AsistenciaQrRepositoryPort.class);
        tomasRepo = mock(TomaAsistenciaRepositoryPort.class);
        scope = mock(InstitutionalScopePort.class);
        realtime = mock(RealtimePublisherPort.class);
        limitador = mock(LimitadorIntentosPort.class);
        when(parametros.getParameter("ASISTENCIA_QR", "HABILITADA")).thenReturn(Optional.of("true"));
        when(parametros.getParameter("ASISTENCIA_QR", "URL_BASE_FRONTEND")).thenReturn(Optional.of("https://asistencia.uco.test/"));
        when(parametros.getParameter("ASISTENCIA_QR", "ROTACION_SEGUNDOS")).thenReturn(Optional.of("60"));
        when(red.perteneceACampus(IP)).thenReturn(true);
        when(generador.generarToken()).thenReturn(TOKEN);
        when(generador.generarPin()).thenReturn("482913");
        when(qr.generarPng(anyString(), anyInt())).thenReturn(new byte[]{1, 2, 3});
        when(scope.canDocenteAccessGrupo(USUARIO, GRUPO)).thenReturn(true);
        when(tomasRepo.consultarToma(TOMA)).thenReturn(Optional.of(toma()));
        when(limitador.permitir(anyString(), anyInt())).thenReturn(true);
        acceso = new AsistenciaQrAccesoPolicy(parametros, red);
        emisor = new AsistenciaQrCredencialesEmisor(generador);
        presentador = new AsistenciaQrPresentador(parametros, qr);
        eventos = new AsistenciaQrEventos(new TomaAsistenciaEventos(realtime));
        tomaAcceso = new TomaAsistenciaAcceso(tomasRepo, scope);
    }

    private static TomaAsistenciaResumen toma() {
        return new TomaAsistenciaResumen(TOMA, SESION, GRUPO, 1, "QR", "ABIERTA", true, false, ROTACION, null,
                new BigDecimal("2.000000"), 30, 29, 1, 0, true);
    }

    private List<RealtimeEvent> publicados() {
        final ArgumentCaptor<RealtimeEvent> captor = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime, atLeast(0)).publish(captor.capture());
        return captor.getAllValues();
    }

    private static Map<String, Object> payloadRotado() {
        return Map.of("grupo", GRUPO.toString(), "sesion", SESION.toString(), "toma", TOMA.toString(), "pin", "482913",
                "urlAsistencia", "https://asistencia.uco.test/app/asistencia/qr?t=" + TOKEN, "qrPngBase64", "AQID",
                "fechaRotacionUtc", ROTACION.toString(), "fechaExpiracionUtc", EXPIRACION.toString(), "rotacionSegundos", 60);
    }

    // ------------------------------------------------------------------ abrir / rotar / cerrar

    @Test
    void abrir_qr_devuelve_pin_url_qr_y_fechas_y_publica_el_qr_al_titular() {
        when(repositorio.abrir(TOMA, TOKEN, "482913", USUARIO)).thenReturn(new AsistenciaQrRepositoryPort.QrEmitido(
                TOKEN_ID, TOMA, SESION, GRUPO, ROTACION, EXPIRACION, 60));

        final QrTomaResumen resultado = new AbrirQrTomaUseCaseImpl(acceso, tomaAcceso, emisor, presentador, repositorio, eventos)
                .execute(new AbrirQrTomaInputPort.Comando(TOMA, USUARIO));

        assertEquals(new QrTomaResumen(TOMA, SESION, GRUPO, "482913", "https://asistencia.uco.test/app/asistencia/qr?t=" + TOKEN,
                "AQID", ROTACION, EXPIRACION, 60), resultado);
        verify(qr).generarPng("https://asistencia.uco.test/app/asistencia/qr?t=" + TOKEN, 320);
        assertEquals("ASISTENCIA_QR_ROTADO", publicados().getFirst().type());
        assertEquals(payloadRotado(), publicados().getFirst().payload());
    }

    @Test
    void abrir_reintenta_ante_aqr_006_hasta_5_veces_y_propaga_otros_errores() {
        final AtomicInteger llamadas = new AtomicInteger();
        when(repositorio.abrir(any(), any(), any(), any())).thenAnswer(invocacion -> {
            if (llamadas.incrementAndGet() < 3) {
                throw new ConflictException("AQR_006", "PIN repetido");
            }
            return new AsistenciaQrRepositoryPort.QrEmitido(TOKEN_ID, TOMA, SESION, GRUPO, ROTACION, EXPIRACION, 60);
        });
        final AbrirQrTomaUseCaseImpl useCase = new AbrirQrTomaUseCaseImpl(acceso, tomaAcceso, emisor, presentador, repositorio, eventos);

        assertEquals(TOMA, useCase.execute(new AbrirQrTomaInputPort.Comando(TOMA, USUARIO)).idToma());
        assertEquals(3, llamadas.get());

        doThrow(new ConflictException("AQR_006", "PIN repetido")).when(repositorio).abrir(any(), any(), any(), any());
        assertEquals("AQR_006", assertThrows(ConflictException.class,
                () -> useCase.execute(new AbrirQrTomaInputPort.Comando(TOMA, USUARIO))).getCode());
        verify(generador, times(3 + AsistenciaQrCredencialesEmisor.MAX_INTENTOS)).generarPin();

        doThrow(new ConflictException("AQR_003", "Toma manual")).when(repositorio).abrir(any(), any(), any(), any());
        assertEquals("AQR_003", assertThrows(ConflictException.class,
                () -> useCase.execute(new AbrirQrTomaInputPort.Comando(TOMA, USUARIO))).getCode());
        verify(realtime, times(1)).publish(any());
    }

    @Test
    void abrir_con_interruptor_apagado_o_parametro_ausente_o_catalogo_caido_es_aqr_010_y_ajeno_es_403() {
        final AbrirQrTomaUseCaseImpl useCase = new AbrirQrTomaUseCaseImpl(acceso, tomaAcceso, emisor, presentador, repositorio, eventos);
        when(parametros.getParameter("ASISTENCIA_QR", "HABILITADA")).thenReturn(Optional.of("false"));
        assertEquals("AQR_010", assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(new AbrirQrTomaInputPort.Comando(TOMA, USUARIO))).getCode());
        when(parametros.getParameter("ASISTENCIA_QR", "HABILITADA")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(new AbrirQrTomaInputPort.Comando(TOMA, USUARIO)));
        when(parametros.getParameter("ASISTENCIA_QR", "HABILITADA")).thenThrow(new ParameterCatalogPort.ParameterCatalogException("caido"));
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(new AbrirQrTomaInputPort.Comando(TOMA, USUARIO)));

        doReturn(Optional.of(" TRUE ")).when(parametros).getParameter("ASISTENCIA_QR", "HABILITADA");
        assertThrows(ForbiddenException.class, () -> useCase.execute(new AbrirQrTomaInputPort.Comando(TOMA, UUID.randomUUID())));
        verifyNoInteractions(repositorio, realtime);
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void rotar_manual_usa_la_sesion_de_la_toma_y_la_rotacion_del_parametro() {
        when(parametros.getParameter("ASISTENCIA_QR", "ROTACION_SEGUNDOS")).thenReturn(Optional.of("45"));
        when(repositorio.rotarManual(TOMA, TOKEN, "482913", USUARIO)).thenReturn(new AsistenciaQrRepositoryPort.QrRotado(
                TOKEN_ID, TOMA, GRUPO, DOCENTE, ROTACION, EXPIRACION));

        final QrTomaResumen resultado = new RotarQrTomaUseCaseImpl(acceso, tomaAcceso, emisor, presentador, repositorio, eventos)
                .execute(new RotarQrTomaInputPort.Comando(TOMA, USUARIO));

        assertEquals(SESION, resultado.idSesion());
        assertEquals(45, resultado.rotacionSegundos());
        assertEquals("ASISTENCIA_QR_ROTADO", publicados().getFirst().type());
        assertThrows(NullPointerException.class, () -> new RotarQrTomaUseCaseImpl(acceso, tomaAcceso, emisor, presentador, repositorio, eventos)
                .execute(null));
    }

    @Test
    void parametros_numericos_invalidos_o_no_positivos_usan_el_valor_por_defecto_y_la_url_base_tambien() {
        when(parametros.getParameter("ASISTENCIA_QR", "ROTACION_SEGUNDOS")).thenReturn(Optional.of("abc"));
        assertEquals(60, acceso.rotacionSegundos());
        when(parametros.getParameter("ASISTENCIA_QR", "ROTACION_SEGUNDOS")).thenReturn(Optional.of("0"));
        assertEquals(60, acceso.rotacionSegundos());
        when(parametros.getParameter("ASISTENCIA_QR", "LIMITE_ESTUDIANTE_POR_MINUTO")).thenReturn(Optional.empty());
        assertEquals(5, acceso.limiteEstudiantePorMinuto());
        when(parametros.getParameter("ASISTENCIA_QR", "LIMITE_TOKEN_POR_MINUTO")).thenReturn(Optional.of("300"));
        assertEquals(300, acceso.limiteTokenPorMinuto());

        when(parametros.getParameter("ASISTENCIA_QR", "URL_BASE_FRONTEND")).thenReturn(Optional.of("  "));
        assertEquals("http://localhost:4200/app/asistencia/qr?t=x", presentador.presentar("x").urlAsistencia());
        when(parametros.getParameter("ASISTENCIA_QR", "URL_BASE_FRONTEND")).thenThrow(new ParameterCatalogPort.ParameterCatalogException("caido"));
        assertEquals("http://localhost:4200/app/asistencia/qr?t=y", presentador.presentar("y").urlAsistencia());
        doReturn(Optional.of("https://a.test")).when(parametros).getParameter("ASISTENCIA_QR", "URL_BASE_FRONTEND");
        assertEquals("https://a.test/app/asistencia/qr?t=z", presentador.presentar("z").urlAsistencia());
    }

    @Test
    void cerrar_qr_devuelve_el_mensaje_del_sp_y_exige_titularidad() {
        when(repositorio.cerrar(TOMA, USUARIO)).thenReturn("Se dejó de proyectar el código QR.");
        final CerrarQrTomaUseCaseImpl useCase = new CerrarQrTomaUseCaseImpl(tomaAcceso, repositorio);

        assertEquals("Se dejó de proyectar el código QR.", useCase.execute(new CerrarQrTomaInputPort.Comando(TOMA, USUARIO)));
        assertThrows(ForbiddenException.class, () -> useCase.execute(new CerrarQrTomaInputPort.Comando(TOMA, UUID.randomUUID())));
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        verify(repositorio, times(1)).cerrar(any(), any());
    }

    // ------------------------------------------------------------------ rotacion automatica

    @Test
    void rotacion_automatica_publica_solo_las_rotadas_y_un_fallo_no_detiene_las_demas() {
        final UUID otraToma = UUID.randomUUID();
        final UUID fallida = UUID.randomUUID();
        when(repositorio.consultarTomasPorRotar()).thenReturn(List.of(
                new AsistenciaQrRepositoryPort.TomaPorRotar(TOMA, GRUPO, DOCENTE),
                new AsistenciaQrRepositoryPort.TomaPorRotar(otraToma, GRUPO, DOCENTE),
                new AsistenciaQrRepositoryPort.TomaPorRotar(fallida, GRUPO, DOCENTE)));
        when(repositorio.rotarAutomatico(TOMA, TOKEN, "482913")).thenReturn(Optional.of(
                new AsistenciaQrRepositoryPort.QrRotado(TOKEN_ID, TOMA, GRUPO, DOCENTE, ROTACION, EXPIRACION)));
        when(repositorio.rotarAutomatico(otraToma, TOKEN, "482913")).thenReturn(Optional.empty());
        when(repositorio.rotarAutomatico(fallida, TOKEN, "482913")).thenThrow(new IllegalStateException("db"));

        final int rotadas = new RotarTokensAsistenciaAutomaticoUseCaseImpl(acceso, emisor, presentador, repositorio, tomasRepo, eventos).execute();

        assertEquals(1, rotadas);
        assertEquals(1, publicados().size());
        assertEquals(payloadRotado(), publicados().getFirst().payload());
    }

    @Test
    void rotacion_automatica_con_interruptor_apagado_no_consulta() {
        when(parametros.getParameter("ASISTENCIA_QR", "HABILITADA")).thenReturn(Optional.of("false"));

        assertEquals(0, new RotarTokensAsistenciaAutomaticoUseCaseImpl(acceso, emisor, presentador, repositorio, tomasRepo, eventos).execute());
        verifyNoInteractions(repositorio, realtime);
    }

    // ------------------------------------------------------------------ registro del estudiante

    private RegistrarAsistenciaPorTokenUseCaseImpl registrar() {
        return new RegistrarAsistenciaPorTokenUseCaseImpl(acceso, limitador, repositorio, tomasRepo, scope, eventos, esperas::add);
    }

    private static AsistenciaQrRepositoryPort.AsistenciaPorToken registro(final boolean yaRegistrado) {
        return new AsistenciaQrRepositoryPort.AsistenciaPorToken(TOMA, SESION, GRUPO, "Arquitectura", "Grupo 1", 12, 2,
                yaRegistrado, ROTACION, yaRegistrado ? "Tu asistencia a esta toma ya estaba registrada." : "¡Listo! Tu asistencia quedó registrada.");
    }

    @Test
    void registrar_por_token_nuevo_publica_registrada_con_estudiante_y_presentes() {
        when(repositorio.registrarPorToken(TOKEN, null, USUARIO)).thenReturn(registro(false));
        when(scope.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(ESTUDIANTE));

        final RegistrarAsistenciaPorTokenInputPort.Resultado resultado = registrar()
                .execute(new RegistrarAsistenciaPorTokenInputPort.Comando(" " + TOKEN + " ", null, IP, USUARIO));

        assertEquals(new RegistrarAsistenciaPorTokenInputPort.Resultado(TOMA, SESION, "Arquitectura", "Grupo 1", 12, 2, false,
                ROTACION, "¡Listo! Tu asistencia quedó registrada."), resultado);
        assertEquals("ASISTENCIA_QR_REGISTRADA", publicados().getFirst().type());
        assertEquals(Map.of("grupo", GRUPO.toString(), "sesion", SESION.toString(), "toma", TOMA.toString(),
                "estudiante", ESTUDIANTE.toString(), "totalPresentes", 29), publicados().getFirst().payload());
        verify(limitador).permitir("asistencia-qr:estudiante:" + USUARIO, 5);
        verify(limitador).permitir(eq("asistencia-qr:token:" + java.util.HexFormat.of().formatHex(
                co.edu.uco.asistenciasuco.crosscutting.util.TokenHashHelper.sha256(TOKEN))), eq(120));
    }

    @Test
    void registrar_por_pin_ya_registrado_no_publica_y_lecturas_auxiliares_caidas_no_rompen_el_registro() {
        when(repositorio.registrarPorToken(null, "482913", USUARIO)).thenReturn(registro(true));

        assertTrue(registrar().execute(new RegistrarAsistenciaPorTokenInputPort.Comando("", "482913", IP, USUARIO)).yaRegistrado());
        verifyNoInteractions(realtime);
        verify(limitador).permitir("asistencia-qr:pin:482913", 120);

        when(repositorio.registrarPorToken(null, "482913", USUARIO)).thenReturn(registro(false));
        when(scope.findEstudianteIdByUsuario(USUARIO)).thenThrow(new IllegalStateException("scope caido"));
        assertFalse(registrar().execute(new RegistrarAsistenciaPorTokenInputPort.Comando(null, "482913", IP, USUARIO)).yaRegistrado());

        doReturn(Optional.empty()).when(scope).findEstudianteIdByUsuario(USUARIO);
        when(tomasRepo.consultarToma(TOMA)).thenReturn(Optional.empty());
        registrar().execute(new RegistrarAsistenciaPorTokenInputPort.Comando(null, "482913", IP, USUARIO));
        assertEquals(Map.of("grupo", GRUPO.toString(), "sesion", SESION.toString(), "toma", TOMA.toString()),
                publicados().getLast().payload());
    }

    @Test
    void registrar_rechaza_en_orden_interruptor_red_limite_estudiante_credencial_y_limite_token() {
        final RegistrarAsistenciaPorTokenUseCaseImpl useCase = registrar();

        when(parametros.getParameter("ASISTENCIA_QR", "HABILITADA")).thenReturn(Optional.of("false"));
        assertEquals("AQR_010", assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(new RegistrarAsistenciaPorTokenInputPort.Comando(TOKEN, null, IP, USUARIO))).getCode());
        verifyNoInteractions(limitador);

        when(parametros.getParameter("ASISTENCIA_QR", "HABILITADA")).thenReturn(Optional.of("true"));
        assertEquals("AQR_002", assertThrows(ForbiddenException.class,
                () -> useCase.execute(new RegistrarAsistenciaPorTokenInputPort.Comando(TOKEN, null, "8.8.8.8", USUARIO))).getCode());
        assertEquals("AQR_002", assertThrows(ForbiddenException.class,
                () -> useCase.execute(new RegistrarAsistenciaPorTokenInputPort.Comando(TOKEN, null, null, USUARIO))).getCode());
        verifyNoInteractions(limitador);

        when(limitador.permitir("asistencia-qr:estudiante:" + USUARIO, 5)).thenReturn(false);
        assertEquals("AQR_011", assertThrows(TooManyRequestsException.class,
                () -> useCase.execute(new RegistrarAsistenciaPorTokenInputPort.Comando(TOKEN, null, IP, USUARIO))).getCode());

        when(limitador.permitir("asistencia-qr:estudiante:" + USUARIO, 5)).thenReturn(true);
        assertEquals("AQR_007", assertThrows(ValidationException.class,
                () -> useCase.execute(new RegistrarAsistenciaPorTokenInputPort.Comando(TOKEN, "482913", IP, USUARIO))).getCode());
        assertEquals("AQR_007", assertThrows(ValidationException.class,
                () -> useCase.execute(new RegistrarAsistenciaPorTokenInputPort.Comando(" ", null, IP, USUARIO))).getCode());

        when(limitador.permitir("asistencia-qr:pin:482913", 120)).thenReturn(false);
        assertEquals("AQR_011", assertThrows(TooManyRequestsException.class,
                () -> useCase.execute(new RegistrarAsistenciaPorTokenInputPort.Comando(null, "482913", IP, USUARIO))).getCode());
        verify(repositorio, never()).registrarPorToken(any(), any(), any());
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void registrar_reintenta_3_veces_ante_interbloqueo_con_espera_de_20_a_80_ms_y_luego_propaga() {
        final AtomicInteger llamadas = new AtomicInteger();
        when(repositorio.registrarPorToken(TOKEN, null, USUARIO)).thenAnswer(invocacion -> {
            if (llamadas.incrementAndGet() <= 3) {
                throw new AsistenciaQrRepositoryPort.ConflictoConcurrenciaException("1205");
            }
            return registro(true);
        });

        assertTrue(registrar().execute(new RegistrarAsistenciaPorTokenInputPort.Comando(TOKEN, null, IP, USUARIO)).yaRegistrado());
        assertEquals(4, llamadas.get());
        assertEquals(3, esperas.size());
        assertTrue(esperas.stream().allMatch(ms -> ms >= 20 && ms <= 80));

        doThrow(new AsistenciaQrRepositoryPort.ConflictoConcurrenciaException("1205")).when(repositorio).registrarPorToken(TOKEN, null, USUARIO);
        assertThrows(AsistenciaQrRepositoryPort.ConflictoConcurrenciaException.class,
                () -> registrar().execute(new RegistrarAsistenciaPorTokenInputPort.Comando(TOKEN, null, IP, USUARIO)));
        assertEquals(6, esperas.size());
    }

    @Test
    void un_error_del_sp_se_propaga_sin_evento() {
        when(repositorio.registrarPorToken(TOKEN, null, USUARIO)).thenThrow(new ResourceNotFoundException("AQR_001", "Codigo invalido"));

        assertEquals("AQR_001", assertThrows(ResourceNotFoundException.class,
                () -> registrar().execute(new RegistrarAsistenciaPorTokenInputPort.Comando(TOKEN, null, IP, USUARIO))).getCode());
        verifyNoInteractions(realtime);
    }

    @Test
    void la_espera_del_hilo_duerme_y_respeta_la_interrupcion() {
        final EsperaReintento espera = EsperaReintento.delHilo();
        final long inicio = System.nanoTime();
        espera.esperar(5);
        assertTrue(System.nanoTime() - inicio >= 4_000_000L);

        Thread.currentThread().interrupt();
        espera.esperar(50);
        assertTrue(Thread.interrupted());
    }

    @Test
    void la_publicacion_es_de_mejor_esfuerzo() {
        doThrow(new IllegalStateException("canal")).when(realtime).publish(any());
        when(repositorio.abrir(TOMA, TOKEN, "482913", USUARIO)).thenReturn(new AsistenciaQrRepositoryPort.QrEmitido(
                TOKEN_ID, TOMA, SESION, GRUPO, ROTACION, EXPIRACION, 60));

        assertSame(TOMA, new AbrirQrTomaUseCaseImpl(acceso, tomaAcceso, emisor, presentador, repositorio, eventos)
                .execute(new AbrirQrTomaInputPort.Comando(TOMA, USUARIO)).idToma());
    }

    @Test
    void los_componentes_exigen_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new AsistenciaQrAccesoPolicy(null, red));
        assertThrows(NullPointerException.class, () -> new AsistenciaQrAccesoPolicy(parametros, null));
        assertThrows(NullPointerException.class, () -> new AsistenciaQrCredencialesEmisor(null));
        assertThrows(NullPointerException.class, () -> new AsistenciaQrPresentador(null, qr));
        assertThrows(NullPointerException.class, () -> new AsistenciaQrPresentador(parametros, null));
        assertThrows(NullPointerException.class, () -> new AsistenciaQrEventos(null));
        assertThrows(NullPointerException.class, () -> new AbrirQrTomaUseCaseImpl(null, tomaAcceso, emisor, presentador, repositorio, eventos));
        assertThrows(NullPointerException.class, () -> new AbrirQrTomaUseCaseImpl(acceso, null, emisor, presentador, repositorio, eventos));
        assertThrows(NullPointerException.class, () -> new AbrirQrTomaUseCaseImpl(acceso, tomaAcceso, null, presentador, repositorio, eventos));
        assertThrows(NullPointerException.class, () -> new AbrirQrTomaUseCaseImpl(acceso, tomaAcceso, emisor, null, repositorio, eventos));
        assertThrows(NullPointerException.class, () -> new AbrirQrTomaUseCaseImpl(acceso, tomaAcceso, emisor, presentador, null, eventos));
        assertThrows(NullPointerException.class, () -> new AbrirQrTomaUseCaseImpl(acceso, tomaAcceso, emisor, presentador, repositorio, null));
        assertThrows(NullPointerException.class, () -> new RotarQrTomaUseCaseImpl(null, tomaAcceso, emisor, presentador, repositorio, eventos));
        assertThrows(NullPointerException.class, () -> new RotarQrTomaUseCaseImpl(acceso, null, emisor, presentador, repositorio, eventos));
        assertThrows(NullPointerException.class, () -> new RotarQrTomaUseCaseImpl(acceso, tomaAcceso, null, presentador, repositorio, eventos));
        assertThrows(NullPointerException.class, () -> new RotarQrTomaUseCaseImpl(acceso, tomaAcceso, emisor, null, repositorio, eventos));
        assertThrows(NullPointerException.class, () -> new RotarQrTomaUseCaseImpl(acceso, tomaAcceso, emisor, presentador, null, eventos));
        assertThrows(NullPointerException.class, () -> new RotarQrTomaUseCaseImpl(acceso, tomaAcceso, emisor, presentador, repositorio, null));
        assertThrows(NullPointerException.class, () -> new CerrarQrTomaUseCaseImpl(null, repositorio));
        assertThrows(NullPointerException.class, () -> new CerrarQrTomaUseCaseImpl(tomaAcceso, null));
        assertThrows(NullPointerException.class, () -> new RotarTokensAsistenciaAutomaticoUseCaseImpl(null, emisor, presentador, repositorio, tomasRepo, eventos));
        assertThrows(NullPointerException.class, () -> new RotarTokensAsistenciaAutomaticoUseCaseImpl(acceso, null, presentador, repositorio, tomasRepo, eventos));
        assertThrows(NullPointerException.class, () -> new RotarTokensAsistenciaAutomaticoUseCaseImpl(acceso, emisor, null, repositorio, tomasRepo, eventos));
        assertThrows(NullPointerException.class, () -> new RotarTokensAsistenciaAutomaticoUseCaseImpl(acceso, emisor, presentador, null, tomasRepo, eventos));
        assertThrows(NullPointerException.class, () -> new RotarTokensAsistenciaAutomaticoUseCaseImpl(acceso, emisor, presentador, repositorio, null, eventos));
        assertThrows(NullPointerException.class, () -> new RotarTokensAsistenciaAutomaticoUseCaseImpl(acceso, emisor, presentador, repositorio, tomasRepo, null));
        final EsperaReintento espera = esperas::add;
        assertThrows(NullPointerException.class, () -> new RegistrarAsistenciaPorTokenUseCaseImpl(null, limitador, repositorio, tomasRepo, scope, eventos, espera));
        assertThrows(NullPointerException.class, () -> new RegistrarAsistenciaPorTokenUseCaseImpl(acceso, null, repositorio, tomasRepo, scope, eventos, espera));
        assertThrows(NullPointerException.class, () -> new RegistrarAsistenciaPorTokenUseCaseImpl(acceso, limitador, null, tomasRepo, scope, eventos, espera));
        assertThrows(NullPointerException.class, () -> new RegistrarAsistenciaPorTokenUseCaseImpl(acceso, limitador, repositorio, null, scope, eventos, espera));
        assertThrows(NullPointerException.class, () -> new RegistrarAsistenciaPorTokenUseCaseImpl(acceso, limitador, repositorio, tomasRepo, null, eventos, espera));
        assertThrows(NullPointerException.class, () -> new RegistrarAsistenciaPorTokenUseCaseImpl(acceso, limitador, repositorio, tomasRepo, scope, null, espera));
        assertThrows(NullPointerException.class, () -> new RegistrarAsistenciaPorTokenUseCaseImpl(acceso, limitador, repositorio, tomasRepo, scope, eventos, null));
    }
}
