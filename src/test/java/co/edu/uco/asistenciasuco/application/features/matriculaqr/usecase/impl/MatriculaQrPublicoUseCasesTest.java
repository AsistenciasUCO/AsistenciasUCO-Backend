package co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.MatriculaQrCredencialesEmisor;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.MatriculaQrPresentador;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.MatriculaQrAccesoPolicy;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.exception.MatriculaQrErrorCode;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.MatricularEstudianteAutenticadoInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.RegistrarEstudiantePorTokenInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.ValidarTokenMatriculaInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.network.RedInstitucionalPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.qr.CodigoQrGeneratorPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.TokenGeneratorPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HU176B (T-10, T-16, T-18, T-27, T-28, T-29, T-30, T-42): casos de uso publicos y de rotacion. Orden de las reglas
 * (interruptor, red, forma, DB), DB antes que el proveedor de identidad, eventos sin datos personales y job resiliente.
 */
class MatriculaQrPublicoUseCasesTest {

    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USUARIO = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ESTUDIANTE = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID TIPO_DOC = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final String IP_CAMPUS = "10.1.2.3";
    private static final String TOKEN = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789_-AbCde";
    private static final Instant FIN = Instant.parse("2026-11-04T16:00:00Z");

    private ParameterCatalogPort parametros;
    private RedInstitucionalPort red;
    private MatriculaQrRepositoryPort repositorio;
    private RealtimePublisherPort realtime;
    private MatriculaQrAccesoPolicy acceso;

    @BeforeEach
    void preparar() {
        parametros = mock(ParameterCatalogPort.class);
        red = mock(RedInstitucionalPort.class);
        repositorio = mock(MatriculaQrRepositoryPort.class);
        realtime = mock(RealtimePublisherPort.class);
        when(parametros.getParameter("MATRICULA_QR", "HABILITADA")).thenReturn(Optional.of("true"));
        when(red.perteneceACampus(IP_CAMPUS)).thenReturn(true);
        acceso = new MatriculaQrAccesoPolicy(parametros, red);
    }

    // ---------------------------------------------------------------- validar

    @Test
    void T18_validar_fuera_del_campus_recibe_403_mqr_002_y_no_consulta_la_db() {
        final var useCase = new ValidarTokenMatriculaUseCaseImpl(acceso, repositorio);
        final var comando = new ValidarTokenMatriculaInputPort.Comando(TOKEN, null, "203.0.113.9");

        final ForbiddenException error = assertThrows(ForbiddenException.class, () -> useCase.execute(comando));

        assertEquals("MQR_002", error.getCode());
        verifyNoInteractions(repositorio);
    }

    @Test
    void T26_validar_con_la_funcionalidad_apagada_responde_404_antes_de_revisar_la_red() {
        when(parametros.getParameter("MATRICULA_QR", "HABILITADA")).thenReturn(Optional.of("false"));
        final var useCase = new ValidarTokenMatriculaUseCaseImpl(acceso, repositorio);
        final var comando = new ValidarTokenMatriculaInputPort.Comando(TOKEN, null, IP_CAMPUS);

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(comando));

        verifyNoInteractions(red);
        verifyNoInteractions(repositorio);
    }

    @Test
    void T30_ninguno_o_ambos_token_y_pin_se_rechazan_con_mqr_007_sin_consultar_la_db() {
        final var useCase = new ValidarTokenMatriculaUseCaseImpl(acceso, repositorio);
        final var comandoNinguno = new ValidarTokenMatriculaInputPort.Comando(null, " ", IP_CAMPUS);
        final var comandoAmbos = new ValidarTokenMatriculaInputPort.Comando(TOKEN, "123456", IP_CAMPUS);

        final ValidationException ninguno = assertThrows(ValidationException.class, () -> useCase.execute(comandoNinguno));
        final ValidationException ambos = assertThrows(ValidationException.class, () -> useCase.execute(comandoAmbos));

        assertEquals("MQR_007", ninguno.getCode());
        assertEquals("MQR_007", ambos.getCode());
        verifyNoInteractions(repositorio);
    }

    @Test
    void validar_devuelve_solo_los_datos_minimos_del_curso() {
        when(repositorio.validar(new MatriculaQrRepositoryPort.CredencialMatricula(TOKEN, null)))
                .thenReturn(new MatriculaQrRepositoryPort.CursoMatricula(GRUPO, "Calculo", "G1", "Maria Perez", FIN, 7));

        final var resultado = new ValidarTokenMatriculaUseCaseImpl(acceso, repositorio)
                .execute(new ValidarTokenMatriculaInputPort.Comando(TOKEN, null, IP_CAMPUS));

        assertEquals(new ValidarTokenMatriculaInputPort.Resultado(GRUPO, "Calculo", "G1", "Maria Perez", FIN, 7), resultado);
    }

    @Test
    void validar_propaga_el_error_generico_mqr_001_de_la_db() {
        when(repositorio.validar(any())).thenThrow(new ResourceNotFoundException(MatriculaQrErrorCode.MQR_001));
        final var useCase = new ValidarTokenMatriculaUseCaseImpl(acceso, repositorio);
        final var comando = new ValidarTokenMatriculaInputPort.Comando(null, "123456", IP_CAMPUS);

        final ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class, () -> useCase.execute(comando));

        assertEquals("MQR_001", error.getCode());
    }

    // ---------------------------------------------------------------- matricula autenticada

    @Test
    void T16_el_estudiante_autenticado_se_matricula_y_se_publica_el_evento_sin_datos_personales() {
        when(repositorio.matricularAutenticado(new MatriculaQrRepositoryPort.CredencialMatricula(null, "123456"), USUARIO))
                .thenReturn(new MatriculaQrRepositoryPort.MatriculaRealizada(ESTUDIANTE, USUARIO, GRUPO));

        final var resultado = new MatricularEstudianteAutenticadoUseCaseImpl(acceso, repositorio, realtime)
                .execute(new MatricularEstudianteAutenticadoInputPort.Comando(null, "123456", IP_CAMPUS, USUARIO));

        assertEquals(GRUPO, resultado.idGrupo());
        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals("ESTUDIANTE_MATRICULADO", evento.getValue().type());
        assertEquals(java.util.Map.of("grupo", GRUPO.toString(), "origen", "QR"), evento.getValue().payload());
    }

    @Test
    void T18_matricula_autenticada_fuera_del_campus_recibe_403_y_no_escribe() {
        final var useCase = new MatricularEstudianteAutenticadoUseCaseImpl(acceso, repositorio, realtime);
        final var comando = new MatricularEstudianteAutenticadoInputPort.Comando(TOKEN, null, "8.8.8.8", USUARIO);

        assertThrows(ForbiddenException.class, () -> useCase.execute(comando));

        verifyNoInteractions(repositorio);
        verifyNoInteractions(realtime);
    }

    @Test
    void T12_un_conflicto_de_la_db_no_publica_ningun_evento() {
        when(repositorio.matricularAutenticado(any(), eq(USUARIO))).thenThrow(new ConflictException(MatriculaQrErrorCode.MQR_004));
        final var useCase = new MatricularEstudianteAutenticadoUseCaseImpl(acceso, repositorio, realtime);
        final var comando = new MatricularEstudianteAutenticadoInputPort.Comando(TOKEN, null, IP_CAMPUS, USUARIO);

        assertThrows(ConflictException.class, () -> useCase.execute(comando));

        verifyNoInteractions(realtime);
    }

    // ---------------------------------------------------------------- registro publico

    private PasswordEncoderPort encoder;
    private IdentityProviderPort identidad;
    private NotificacionPort notificaciones;
    private CorreoInstitucionalPolicy correoPolicy;

    private RegistrarEstudiantePorTokenUseCaseImpl registro() {
        encoder = mock(PasswordEncoderPort.class);
        identidad = mock(IdentityProviderPort.class);
        notificaciones = mock(NotificacionPort.class);
        correoPolicy = mock(CorreoInstitucionalPolicy.class);
        when(encoder.encode("Clave-segura-1")).thenReturn("HASH");
        when(repositorio.registrarPublico(any()))
                .thenReturn(new MatriculaQrRepositoryPort.MatriculaRealizada(ESTUDIANTE, USUARIO, GRUPO));
        return new RegistrarEstudiantePorTokenUseCaseImpl(
                acceso, correoPolicy, encoder, repositorio, identidad, notificaciones, realtime);
    }

    private static RegistrarEstudiantePorTokenInputPort.Comando comandoRegistro(final String ip) {
        return new RegistrarEstudiantePorTokenInputPort.Comando(
                TOKEN, null, ip, TIPO_DOC, 1017555444, "Ana", null, "Gomez", null, "ana@uco.edu.co", "Clave-segura-1");
    }

    @Test
    void T10_el_registro_publico_guarda_primero_en_la_db_y_luego_crea_la_cuenta_con_verificacion_de_correo() {
        final var useCase = registro();

        final var resultado = useCase.execute(comandoRegistro(IP_CAMPUS));

        assertEquals(new RegistrarEstudiantePorTokenInputPort.Resultado(GRUPO, "1017555444"), resultado);
        final InOrder orden = inOrder(correoPolicy, encoder, repositorio, identidad, realtime);
        orden.verify(correoPolicy).exigir("ana@uco.edu.co");
        orden.verify(encoder).encode("Clave-segura-1");
        orden.verify(repositorio).registrarPublico(any());
        orden.verify(identidad).crearCuenta(any());
        orden.verify(realtime).publish(any());

        final ArgumentCaptor<CrearCuentaIdentidadDTO> cuenta = ArgumentCaptor.forClass(CrearCuentaIdentidadDTO.class);
        verify(identidad).crearCuenta(cuenta.capture());
        assertTrue(cuenta.getValue().requiereVerificacionCorreo());
        assertEquals("1017555444", cuenta.getValue().username());
        assertEquals("Clave-segura-1", cuenta.getValue().passwordInicial());
        assertEquals(USUARIO, cuenta.getValue().idUsuario());
    }

    @Test
    void T10_la_db_recibe_la_contrasena_codificada_nunca_en_claro_y_vacios_en_los_segundos_nombres() {
        final var useCase = registro();

        useCase.execute(comandoRegistro(IP_CAMPUS));

        final ArgumentCaptor<MatriculaQrRepositoryPort.RegistroPublico> datos = ArgumentCaptor.forClass(MatriculaQrRepositoryPort.RegistroPublico.class);
        verify(repositorio).registrarPublico(datos.capture());
        assertEquals("HASH", datos.getValue().passwordCodificada());
        assertEquals("", datos.getValue().segundoNombre());
        assertEquals("", datos.getValue().segundoApellido());
        assertEquals(TOKEN, datos.getValue().credencial().token());
    }

    @Test
    void T27_el_evento_del_registro_lleva_solo_grupo_y_origen() {
        registro().execute(comandoRegistro(IP_CAMPUS));

        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals("ESTUDIANTE_MATRICULADO", evento.getValue().type());
        assertEquals(java.util.Map.of("grupo", GRUPO.toString(), "origen", "QR"), evento.getValue().payload());
    }

    @Test
    void T18_registro_fuera_del_campus_recibe_403_y_no_toca_db_ni_identidad() {
        final var useCase = registro();
        final var comando = comandoRegistro("203.0.113.9");

        assertThrows(ForbiddenException.class, () -> useCase.execute(comando));

        verifyNoInteractions(repositorio);
        verifyNoInteractions(identidad);
    }

    @Test
    void T29_si_el_proveedor_de_identidad_falla_la_matricula_ya_esta_guardada_y_el_error_es_controlado() {
        final var useCase = registro();
        doThrow(new IdentityProviderPort.IdentityProviderException("keycloak caido")).when(identidad).crearCuenta(any());
        final var comando = comandoRegistro(IP_CAMPUS);

        assertThrows(InternalApplicationException.class, () -> useCase.execute(comando));

        verify(repositorio).registrarPublico(any());
        verify(identidad, never()).eliminarCuenta(anyString());
    }

    @Test
    void T11_persona_ya_registrada_mqr_005_no_crea_cuenta_ni_publica_evento() {
        final var useCase = registro();
        when(repositorio.registrarPublico(any())).thenThrow(new ConflictException(MatriculaQrErrorCode.MQR_005));
        final var comando = comandoRegistro(IP_CAMPUS);

        final ConflictException error = assertThrows(ConflictException.class, () -> useCase.execute(comando));

        assertEquals("MQR_005", error.getCode());
        verifyNoInteractions(identidad);
        verifyNoInteractions(realtime);
    }

    @Test
    void ident001_un_correo_no_institucional_se_rechaza_antes_de_la_db() {
        final var useCase = registro();
        doThrow(new ValidationException("VCE_001", "El correo debe ser institucional")).when(correoPolicy).exigir(anyString());
        final var comando = comandoRegistro(IP_CAMPUS);

        assertThrows(ValidationException.class, () -> useCase.execute(comando));

        verifyNoInteractions(repositorio);
        verifyNoInteractions(identidad);
    }

    @Test
    void T28_un_fallo_de_la_cola_de_avisos_no_revierte_el_registro() {
        final var useCase = registro();
        doThrow(new RuntimeException("cola caida")).when(notificaciones).notificar(any());

        final var resultado = useCase.execute(comandoRegistro(IP_CAMPUS));

        assertEquals(GRUPO, resultado.idGrupo());
        verify(realtime).publish(any());
    }

    @Test
    void faltan_datos_obligatorios_del_registro_es_validacion() {
        final var useCase = registro();
        final var sinDocumento = new RegistrarEstudiantePorTokenInputPort.Comando(
                TOKEN, null, IP_CAMPUS, TIPO_DOC, null, "Ana", null, "Gomez", null, "ana@uco.edu.co", "Clave-segura-1");
        final var ambasCredenciales = new RegistrarEstudiantePorTokenInputPort.Comando(
                TOKEN, "123456", IP_CAMPUS, TIPO_DOC, 1017555444, "Ana", null, "Gomez", null, "ana@uco.edu.co", "Clave-segura-1");
        final var sinTipo = new RegistrarEstudiantePorTokenInputPort.Comando(
                TOKEN, null, IP_CAMPUS, null, 1017555444, "Ana", null, "Gomez", null, "ana@uco.edu.co", "Clave-segura-1");
        final var sinNombre = new RegistrarEstudiantePorTokenInputPort.Comando(
                TOKEN, null, IP_CAMPUS, TIPO_DOC, 1017555444, " ", "Segundo", "Gomez", "SegundoAp", "ana@uco.edu.co", "Clave-segura-1");
        final var sinApellido = new RegistrarEstudiantePorTokenInputPort.Comando(
                TOKEN, null, IP_CAMPUS, TIPO_DOC, 1017555444, "Ana", "Segundo", null, "SegundoAp", "ana@uco.edu.co", "Clave-segura-1");
        final var sinCorreo = new RegistrarEstudiantePorTokenInputPort.Comando(
                TOKEN, null, IP_CAMPUS, TIPO_DOC, 1017555444, "Ana", null, "Gomez", null, "", "Clave-segura-1");
        final var sinPassword = new RegistrarEstudiantePorTokenInputPort.Comando(
                TOKEN, null, IP_CAMPUS, TIPO_DOC, 1017555444, "Ana", null, "Gomez", null, "ana@uco.edu.co", null);

        assertThrows(ValidationException.class, () -> useCase.execute(sinDocumento));
        assertThrows(ValidationException.class, () -> useCase.execute(ambasCredenciales));
        assertThrows(ValidationException.class, () -> useCase.execute(sinTipo));
        assertThrows(ValidationException.class, () -> useCase.execute(sinNombre));
        assertThrows(ValidationException.class, () -> useCase.execute(sinApellido));
        assertThrows(ValidationException.class, () -> useCase.execute(sinCorreo));
        assertThrows(ValidationException.class, () -> useCase.execute(sinPassword));
        verifyNoInteractions(repositorio);
    }

    @Test
    void si_la_db_no_devuelve_el_usuario_creado_falla_de_forma_controlada() {
        final var useCase = registro();
        when(repositorio.registrarPublico(any()))
                .thenReturn(new MatriculaQrRepositoryPort.MatriculaRealizada(ESTUDIANTE, null, GRUPO));
        final var comando = comandoRegistro(IP_CAMPUS);

        assertThrows(InternalApplicationException.class, () -> useCase.execute(comando));
        verifyNoInteractions(identidad);
    }

    // ---------------------------------------------------------------- rotacion automatica (job)

    private TokenGeneratorPort generador;
    private CodigoQrGeneratorPort qr;

    private RotarTokensMatriculaAutomaticoUseCaseImpl job() {
        generador = mock(TokenGeneratorPort.class);
        qr = mock(CodigoQrGeneratorPort.class);
        final int[] n = {0};
        when(generador.generarToken()).thenAnswer(i -> "tok-" + (++n[0]));
        when(generador.generarPin()).thenAnswer(i -> String.format("%06d", n[0]));
        when(qr.generarPng(anyString(), eq(320))).thenReturn(new byte[]{9});
        when(parametros.getRequiredParameter("MATRICULA_QR", "URL_BASE_FRONTEND")).thenReturn("https://asistencia.uco.test");
        return new RotarTokensMatriculaAutomaticoUseCaseImpl(
                acceso, new MatriculaQrCredencialesEmisor(generador), new MatriculaQrPresentador(parametros, qr), repositorio, realtime);
    }

    @Test
    void T42_rota_cada_ventana_y_publica_el_qr_nuevo_con_la_clave_grupo() {
        final var useCase = job();
        final UUID otroGrupo = UUID.fromString("77777777-7777-7777-7777-777777777777");
        when(repositorio.consultarVentanasPorRotar()).thenReturn(List.of(
                new MatriculaQrRepositoryPort.VentanaPorRotar(GRUPO, USUARIO),
                new MatriculaQrRepositoryPort.VentanaPorRotar(otroGrupo, USUARIO)));
        when(repositorio.rotarAutomatico(any(), anyString(), anyString())).thenAnswer(i ->
                new MatriculaQrRepositoryPort.VentanaRotada(UUID.randomUUID(), USUARIO, Instant.parse("2026-11-04T15:00:00Z"), FIN));

        assertEquals(2, useCase.execute());

        final ArgumentCaptor<RealtimeEvent> eventos = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime, times(2)).publish(eventos.capture());
        final RealtimeEvent primero = eventos.getAllValues().get(0);
        assertEquals("MATRICULA_QR_ROTADO", primero.type());
        assertEquals(GRUPO.toString(), primero.payload().get("grupo"));
        assertEquals("https://asistencia.uco.test/matricula/qr?t=tok-1", primero.payload().get("urlMatricula"));
        assertEquals("000001", primero.payload().get("pin"));
        assertFalse(primero.payload().containsKey("tokenHash"));
    }

    @Test
    void T42_un_fallo_en_una_ventana_no_detiene_las_demas() {
        final var useCase = job();
        final UUID otroGrupo = UUID.fromString("77777777-7777-7777-7777-777777777777");
        when(repositorio.consultarVentanasPorRotar()).thenReturn(List.of(
                new MatriculaQrRepositoryPort.VentanaPorRotar(GRUPO, USUARIO),
                new MatriculaQrRepositoryPort.VentanaPorRotar(otroGrupo, USUARIO)));
        when(repositorio.rotarAutomatico(eq(GRUPO), anyString(), anyString())).thenThrow(new IllegalStateException("db caida"));
        when(repositorio.rotarAutomatico(eq(otroGrupo), anyString(), anyString())).thenReturn(
                new MatriculaQrRepositoryPort.VentanaRotada(UUID.randomUUID(), USUARIO, Instant.parse("2026-11-04T15:00:00Z"), FIN));

        assertEquals(1, useCase.execute());

        verify(realtime, times(1)).publish(any());
    }

    @Test
    void una_rotacion_que_no_aplico_porque_otra_instancia_ya_roto_no_publica_un_qr_invalido() {
        final var useCase = job();
        when(repositorio.consultarVentanasPorRotar()).thenReturn(List.of(new MatriculaQrRepositoryPort.VentanaPorRotar(GRUPO, USUARIO)));
        when(repositorio.rotarAutomatico(any(), anyString(), anyString())).thenReturn(null);

        assertEquals(0, useCase.execute());

        verifyNoInteractions(realtime);
    }

    @Test
    void con_la_funcionalidad_apagada_el_job_no_consulta_ni_rota() {
        final var useCase = job();
        when(parametros.getParameter("MATRICULA_QR", "HABILITADA")).thenReturn(Optional.of("false"));

        assertEquals(0, useCase.execute());

        verify(repositorio, never()).consultarVentanasPorRotar();
    }
}
