package co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.MatriculaQrCredencialesEmisor;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase.MatriculaQrPresentador;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.MatriculaQrAccesoPolicy;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.exception.MatriculaQrErrorCode;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.AbrirVentanaMatriculaInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.CerrarVentanaMatriculaInputPort;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.primaryports.RotarVentanaMatriculaInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.network.RedInstitucionalPort;
import co.edu.uco.asistenciasuco.application.secondaryports.qr.CodigoQrGeneratorPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.MatriculaQrRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.TokenGeneratorPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * HU176B (T-01, T-03, T-05, T-09, T-26, T-40): casos de uso de la ventana del docente. Titularidad, interruptor, QR del
 * backend, reintento por PIN repetido y token que solo vive en la URL y el QR.
 */
class MatriculaQrVentanaUseCasesTest {

    private static final UUID GRUPO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DOCENTE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID VENTANA = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant AHORA = Instant.parse("2026-11-04T15:00:00Z");
    private static final Instant FIN = Instant.parse("2026-11-04T16:00:00Z");
    private static final byte[] PNG = {1, 2, 3, 4};

    private ParameterCatalogPort parametros;
    private RedInstitucionalPort red;
    private InstitutionalScopePort scope;
    private MatriculaQrRepositoryPort repositorio;
    private TokenGeneratorPort generador;
    private CodigoQrGeneratorPort qr;
    private MatriculaQrAccesoPolicy acceso;
    private MatriculaQrPresentador presentador;
    private MatriculaQrCredencialesEmisor emisor;
    private final AtomicInteger secuencia = new AtomicInteger();

    @BeforeEach
    void preparar() {
        parametros = mock(ParameterCatalogPort.class);
        red = mock(RedInstitucionalPort.class);
        scope = mock(InstitutionalScopePort.class);
        repositorio = mock(MatriculaQrRepositoryPort.class);
        generador = mock(TokenGeneratorPort.class);
        qr = mock(CodigoQrGeneratorPort.class);
        when(parametros.getParameter("MATRICULA_QR", "HABILITADA")).thenReturn(Optional.of("true"));
        when(parametros.getRequiredParameter("MATRICULA_QR", "URL_BASE_FRONTEND")).thenReturn("https://asistencia.uco.test/");
        when(scope.canDocenteAccessGrupo(DOCENTE, GRUPO)).thenReturn(true);
        when(generador.generarToken()).thenAnswer(i -> "token-" + secuencia.incrementAndGet());
        when(generador.generarPin()).thenAnswer(i -> "00000" + secuencia.get());
        when(qr.generarPng(anyString(), eq(320))).thenReturn(PNG);
        acceso = new MatriculaQrAccesoPolicy(parametros, red);
        presentador = new MatriculaQrPresentador(parametros, qr);
        emisor = new MatriculaQrCredencialesEmisor(generador);
    }

    private AbrirVentanaMatriculaUseCaseImpl abrir() {
        return new AbrirVentanaMatriculaUseCaseImpl(acceso, scope, emisor, presentador, repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void T01_abrir_devuelve_pin_url_con_token_y_qr_generado_por_el_backend() {
        when(repositorio.abrirVentana(eq(GRUPO), anyString(), anyString(), eq(DOCENTE)))
                .thenReturn(new MatriculaQrRepositoryPort.VentanaAbierta(VENTANA, FIN));

        final AbrirVentanaMatriculaInputPort.Resultado resultado =
                abrir().execute(new AbrirVentanaMatriculaInputPort.Comando(GRUPO, DOCENTE));

        assertEquals(VENTANA, resultado.idVentana());
        assertEquals("000001", resultado.pin());
        // La URL base termina en "/" y no se duplica; el token solo aparece aqui y en el QR.
        assertEquals("https://asistencia.uco.test/matricula/qr?t=token-1", resultado.urlMatricula());
        assertArrayEquals(PNG, Base64.getDecoder().decode(resultado.qrPngBase64()));
        verify(qr).generarPng("https://asistencia.uco.test/matricula/qr?t=token-1", 320);
        assertEquals(AHORA, resultado.fechaRotacionUtc());
        assertEquals(FIN, resultado.fechaExpiracionUtc());
        verify(repositorio).abrirVentana(GRUPO, "token-1", "000001", DOCENTE);
    }

    @Test
    void T03_un_docente_sin_titularidad_recibe_403_y_no_toca_la_base_de_datos() {
        when(scope.canDocenteAccessGrupo(DOCENTE, GRUPO)).thenReturn(false);
        final AbrirVentanaMatriculaUseCaseImpl useCase = abrir();
        final var comando = new AbrirVentanaMatriculaInputPort.Comando(GRUPO, DOCENTE);

        assertThrows(ForbiddenException.class, () -> useCase.execute(comando));

        verify(repositorio, never()).abrirVentana(any(), any(), any(), any());
        verify(generador, never()).generarToken();
    }

    @Test
    void T26_con_la_funcionalidad_apagada_o_sin_parametro_responde_404() {
        final AbrirVentanaMatriculaUseCaseImpl useCase = abrir();
        final var comando = new AbrirVentanaMatriculaInputPort.Comando(GRUPO, DOCENTE);

        when(parametros.getParameter("MATRICULA_QR", "HABILITADA")).thenReturn(Optional.of("false"));
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(comando));

        when(parametros.getParameter("MATRICULA_QR", "HABILITADA")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(comando));

        verify(repositorio, never()).abrirVentana(any(), any(), any(), any());
    }

    @Test
    void T09_reintenta_con_otro_pin_cuando_la_db_informa_mqr_006() {
        when(repositorio.abrirVentana(eq(GRUPO), anyString(), anyString(), eq(DOCENTE)))
                .thenThrow(new ConflictException(MatriculaQrErrorCode.MQR_006))
                .thenThrow(new ConflictException(MatriculaQrErrorCode.MQR_006))
                .thenReturn(new MatriculaQrRepositoryPort.VentanaAbierta(VENTANA, FIN));

        final var resultado = abrir().execute(new AbrirVentanaMatriculaInputPort.Comando(GRUPO, DOCENTE));

        assertEquals("https://asistencia.uco.test/matricula/qr?t=token-3", resultado.urlMatricula());
        verify(repositorio, times(3)).abrirVentana(eq(GRUPO), anyString(), anyString(), eq(DOCENTE));
    }

    @Test
    void T09_al_quinto_intento_fallido_propaga_el_error_controlado() {
        when(repositorio.abrirVentana(any(), anyString(), anyString(), any()))
                .thenThrow(new ConflictException(MatriculaQrErrorCode.MQR_006));
        final AbrirVentanaMatriculaUseCaseImpl useCase = abrir();
        final var comando = new AbrirVentanaMatriculaInputPort.Comando(GRUPO, DOCENTE);

        final ConflictException error = assertThrows(ConflictException.class, () -> useCase.execute(comando));

        assertEquals("MQR_006", error.getCode());
        verify(repositorio, times(MatriculaQrCredencialesEmisor.MAX_INTENTOS)).abrirVentana(any(), anyString(), anyString(), any());
    }

    @Test
    void otros_errores_de_negocio_no_se_reintentan() {
        when(repositorio.abrirVentana(any(), anyString(), anyString(), any()))
                .thenThrow(new ConflictException(MatriculaQrErrorCode.MQR_004));
        final AbrirVentanaMatriculaUseCaseImpl useCase = abrir();
        final var comando = new AbrirVentanaMatriculaInputPort.Comando(GRUPO, DOCENTE);

        assertThrows(ConflictException.class, () -> useCase.execute(comando));

        verify(repositorio, times(1)).abrirVentana(any(), anyString(), anyString(), any());
    }

    @Test
    void T40_rotar_manual_genera_credenciales_nuevas_y_devuelve_la_rotacion_de_la_db() {
        final Instant rotacion = Instant.parse("2026-11-04T15:30:00Z");
        when(repositorio.rotarManual(eq(GRUPO), anyString(), anyString(), eq(DOCENTE)))
                .thenReturn(new MatriculaQrRepositoryPort.VentanaRotada(VENTANA, DOCENTE, rotacion, FIN));

        final var resultado = new RotarVentanaMatriculaUseCaseImpl(acceso, scope, emisor, presentador, repositorio)
                .execute(new RotarVentanaMatriculaInputPort.Comando(GRUPO, DOCENTE));

        assertEquals(rotacion, resultado.fechaRotacionUtc());
        assertEquals("https://asistencia.uco.test/matricula/qr?t=token-1", resultado.urlMatricula());
        verify(repositorio).rotarManual(GRUPO, "token-1", "000001", DOCENTE);
    }

    @Test
    void T40_rotar_manual_sin_titularidad_recibe_403() {
        when(scope.canDocenteAccessGrupo(DOCENTE, GRUPO)).thenReturn(false);
        final var useCase = new RotarVentanaMatriculaUseCaseImpl(acceso, scope, emisor, presentador, repositorio);
        final var comando = new RotarVentanaMatriculaInputPort.Comando(GRUPO, DOCENTE);

        assertThrows(ForbiddenException.class, () -> useCase.execute(comando));
        verify(repositorio, never()).rotarManual(any(), any(), any(), any());
    }

    @Test
    void T05_cerrar_valida_titularidad_y_siempre_puede_hacerse_aunque_la_funcionalidad_este_apagada() {
        when(parametros.getParameter("MATRICULA_QR", "HABILITADA")).thenReturn(Optional.of("false"));

        assertNull(new CerrarVentanaMatriculaUseCaseImpl(scope, repositorio)
                .execute(new CerrarVentanaMatriculaInputPort.Comando(GRUPO, DOCENTE)));

        verify(repositorio).cerrarVentana(GRUPO, DOCENTE);
    }

    @Test
    void T05_cerrar_sin_titularidad_recibe_403() {
        when(scope.canDocenteAccessGrupo(DOCENTE, GRUPO)).thenReturn(false);
        final var useCase = new CerrarVentanaMatriculaUseCaseImpl(scope, repositorio);
        final var comando = new CerrarVentanaMatriculaInputPort.Comando(GRUPO, DOCENTE);

        assertThrows(ForbiddenException.class, () -> useCase.execute(comando));
        verify(repositorio, never()).cerrarVentana(any(), any());
    }

    @Test
    void el_hash_del_token_no_lo_calcula_la_aplicacion_ni_el_token_se_loguea_en_el_resultado() {
        when(repositorio.abrirVentana(eq(GRUPO), anyString(), anyString(), eq(DOCENTE)))
                .thenReturn(new MatriculaQrRepositoryPort.VentanaAbierta(VENTANA, FIN));

        final var resultado = abrir().execute(new AbrirVentanaMatriculaInputPort.Comando(GRUPO, DOCENTE));

        // El resultado no expone el token como campo propio: solo vive dentro de la URL y del QR.
        assertTrue(resultado.urlMatricula().contains("token-1"));
        assertFalse(resultado.pin().contains("token"));
        assertEquals("MQR_001", MatriculaQrErrorCode.MQR_001.code());
        assertEquals("El código de matrícula no es válido o ya no está disponible. Solicítale al docente uno nuevo.",
                MatriculaQrErrorCode.MQR_001.defaultMessage());
        assertEquals(co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind.NOT_FOUND, MatriculaQrErrorCode.MQR_001.kind());
    }
}
