package co.edu.uco.asistenciasuco.application.features.tomaasistencia;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CancelacionSesionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.abrirtoma.primaryports.AbrirTomaAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.abrirtoma.usecase.impl.AbrirTomaAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartoma.primaryports.CerrarTomaAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartoma.usecase.impl.CerrarTomaAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartomasvencidas.usecase.impl.CerrarTomasVencidasUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.AsistenciaTomaFila;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.FaltanteTomaFila;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.SesionTitularAcceso;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaAcceso;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaEventos;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomasSesionResumen;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarasistenciastoma.primaryports.ConsultarAsistenciasTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarasistenciastoma.usecase.impl.ConsultarAsistenciasTomaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarfaltantestoma.primaryports.ConsultarFaltantesTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarfaltantestoma.usecase.impl.ConsultarFaltantesTomaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultartomassesion.primaryports.ConsultarTomasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultartomassesion.usecase.impl.ConsultarTomasSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.registrarasistenciastoma.primaryports.RegistrarAsistenciasTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.registrarasistenciastoma.usecase.impl.RegistrarAsistenciasTomaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * CULM-001 B.2 (CONTRACT 3.2.5, 3.8, 3.11): casos de uso de las tomas de asistencia. Exito, cada rechazo (404, 403,
 * cancelada, validacion), eventos publicados solo tras el exito con su payload exacto, y un canal SSE caido que no rompe
 * la respuesta.
 */
class TomaAsistenciaUseCasesTest {

    private static final UUID USUARIO = UUID.fromString("11111111-0000-0000-0000-000000000001");
    private static final UUID SESION = UUID.fromString("22222222-0000-0000-0000-000000000002");
    private static final UUID GRUPO = UUID.fromString("33333333-0000-0000-0000-000000000003");
    private static final UUID TOMA = UUID.fromString("44444444-0000-0000-0000-000000000004");
    private static final Instant APERTURA = Instant.parse("2026-11-07T13:05:00Z");

    private InstitutionalScopePort scope;
    private SesionProcedenciaQueryPort sesiones;
    private TomaAsistenciaRepositoryPort repositorio;
    private RealtimePublisherPort realtime;
    private TomaAsistenciaAcceso acceso;
    private TomaAsistenciaEventos eventos;
    private SesionTitularAcceso sesionTitular;

    @BeforeEach
    void preparar() {
        scope = mock(InstitutionalScopePort.class);
        sesiones = mock(SesionProcedenciaQueryPort.class);
        repositorio = mock(TomaAsistenciaRepositoryPort.class);
        realtime = mock(RealtimePublisherPort.class);
        acceso = new TomaAsistenciaAcceso(repositorio, scope);
        eventos = new TomaAsistenciaEventos(realtime);
        sesionTitular = new SesionTitularAcceso(sesiones, scope);
        when(scope.canDocenteAccessGrupo(USUARIO, GRUPO)).thenReturn(true);
    }

    static TomaAsistenciaResumen toma(final String estado, final int registros) {
        return new TomaAsistenciaResumen(TOMA, SESION, GRUPO, 2, "QR", estado, "ABIERTA".equals(estado), false, APERTURA,
                null, new BigDecimal("1.000000"), registros, registros, 0, 0, true);
    }

    private SesionProcedenciaRepositoryProjection sesion(final CancelacionSesionProjection cancelacion) {
        return new SesionProcedenciaRepositoryProjection(SESION, GRUPO, "S1", 1, "SES-01", 1, "G1", "Grupo 1",
                LocalDateTime.of(2026, 11, 7, 13, 0), LocalDateTime.of(2026, 11, 7, 15, 0), "UTC_V2", cancelacion);
    }

    private List<RealtimeEvent> eventosPublicados() {
        final ArgumentCaptor<RealtimeEvent> captor = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime, org.mockito.Mockito.atLeast(0)).publish(captor.capture());
        return captor.getAllValues();
    }

    // ------------------------------------------------------------------ abrir

    @Test
    void abrir_toma_qr_devuelve_la_toma_releida_y_publica_toma_abierta() {
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(sesion(null)));
        when(repositorio.abrir(SESION, "QR", USUARIO)).thenReturn(new TomaAsistenciaRepositoryPort.TomaAbierta(TOMA, 2, "QR", APERTURA));
        when(repositorio.consultarToma(TOMA)).thenReturn(Optional.of(toma("ABIERTA", 0)));

        final TomaAsistenciaResumen resultado = new AbrirTomaAsistenciaUseCaseImpl(sesionTitular, repositorio, acceso, eventos)
                .execute(new AbrirTomaAsistenciaInputPort.Comando(SESION, " qr ", USUARIO));

        assertEquals(TOMA, resultado.idToma());
        final List<RealtimeEvent> publicados = eventosPublicados();
        assertEquals(1, publicados.size());
        assertEquals("TOMA_ABIERTA", publicados.getFirst().type());
        assertEquals(Map.of("grupo", GRUPO.toString(), "sesion", SESION.toString(), "toma", TOMA.toString(), "numero", 2,
                "modo", "QR", "fechaAperturaUtc", "2026-11-07T13:05:00Z"), publicados.getFirst().payload());
    }

    @Test
    void abrir_rechaza_modo_vacio_sesion_inexistente_ajena_y_cancelada_sin_escribir_ni_publicar() {
        final AbrirTomaAsistenciaUseCaseImpl useCase = new AbrirTomaAsistenciaUseCaseImpl(sesionTitular, repositorio, acceso, eventos);

        assertEquals("TOM_002", assertThrows(ValidationException.class,
                () -> useCase.execute(new AbrirTomaAsistenciaInputPort.Comando(SESION, " ", USUARIO))).getCode());
        assertEquals("TOM_002", assertThrows(ValidationException.class,
                () -> useCase.execute(new AbrirTomaAsistenciaInputPort.Comando(SESION, null, USUARIO))).getCode());

        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(new AbrirTomaAsistenciaInputPort.Comando(SESION, "MANUAL", USUARIO)));

        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(sesion(null)));
        final UUID otro = UUID.randomUUID();
        assertThrows(ForbiddenException.class,
                () -> useCase.execute(new AbrirTomaAsistenciaInputPort.Comando(SESION, "MANUAL", otro)));

        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(sesion(
                new CancelacionSesionProjection("DOCENTE", "Calamidad", null, LocalDateTime.of(2026, 11, 6, 10, 0)))));
        assertEquals("ERR_SESION_CANCELADA", assertThrows(ConflictException.class,
                () -> useCase.execute(new AbrirTomaAsistenciaInputPort.Comando(SESION, "MANUAL", USUARIO))).getCode());

        verify(repositorio, never()).abrir(any(), any(), any());
        verifyNoInteractions(realtime);
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void abrir_con_error_del_sp_no_publica_y_un_canal_caido_no_rompe_la_respuesta() {
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(sesion(null)));
        when(repositorio.abrir(SESION, "MANUAL", USUARIO)).thenThrow(new ConflictException("TOM_003", "Ya hay una toma abierta."));
        final AbrirTomaAsistenciaUseCaseImpl useCase = new AbrirTomaAsistenciaUseCaseImpl(sesionTitular, repositorio, acceso, eventos);

        assertEquals("TOM_003", assertThrows(ConflictException.class,
                () -> useCase.execute(new AbrirTomaAsistenciaInputPort.Comando(SESION, "MANUAL", USUARIO))).getCode());
        verifyNoInteractions(realtime);

        doReturn(new TomaAsistenciaRepositoryPort.TomaAbierta(TOMA, 2, "MANUAL", APERTURA)).when(repositorio).abrir(SESION, "MANUAL", USUARIO);
        when(repositorio.consultarToma(TOMA)).thenReturn(Optional.of(toma("ABIERTA", 0)));
        doThrow(new IllegalStateException("canal")).when(realtime).publish(any());

        assertEquals(TOMA, useCase.execute(new AbrirTomaAsistenciaInputPort.Comando(SESION, "MANUAL", USUARIO)).idToma());
    }

    // ------------------------------------------------------------------ consultar tomas de la sesion

    @Test
    void consultar_tomas_de_sesion_programada_y_cancelada() {
        final TomasSesionResumen tomas = new TomasSesionResumen(SESION, GRUPO, new BigDecimal("2.000000"), false, APERTURA,
                APERTURA.plusSeconds(7200), true, 4, List.of(toma("ABIERTA", 3)));
        when(repositorio.consultarTomasSesion(SESION, USUARIO)).thenReturn(tomas);
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(sesion(null)));
        final ConsultarTomasSesionUseCaseImpl useCase = new ConsultarTomasSesionUseCaseImpl(sesionTitular, repositorio);

        final ConsultarTomasSesionInputPort.Resultado programada = useCase.execute(new ConsultarTomasSesionInputPort.Consulta(SESION, USUARIO));
        assertEquals(EstadoSesion.PROGRAMADA, programada.estado());
        assertNull(programada.cancelacion());
        assertSame(tomas, programada.tomas());

        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(sesion(
                new CancelacionSesionProjection("FESTIVO", "Dia festivo: Navidad", "Navidad", LocalDateTime.of(2026, 11, 6, 10, 0)))));
        final ConsultarTomasSesionInputPort.Resultado cancelada = useCase.execute(new ConsultarTomasSesionInputPort.Consulta(SESION, USUARIO));
        assertEquals(EstadoSesion.CANCELADA, cancelada.estado());
        assertEquals("FESTIVO", cancelada.cancelacion().tipo());
        assertEquals(Instant.parse("2026-11-06T10:00:00Z"), cancelada.cancelacion().fechaCancelacionUtc());
        assertTrue(new TomasSesionResumen(SESION, GRUPO, null, false, null, null, false, 4, null).tomas().isEmpty());
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    // ------------------------------------------------------------------ cerrar

    @Test
    void cerrar_toma_marca_ausentes_y_publica_toma_cerrada_y_asistencias_actualizadas() {
        when(repositorio.consultarToma(TOMA)).thenReturn(Optional.of(toma("CERRADA", 31)));
        when(repositorio.cerrar(TOMA, null, USUARIO)).thenReturn(
                new TomaAsistenciaRepositoryPort.CierreToma(TOMA, SESION, GRUPO, 3, 3, false));

        final CerrarTomaAsistenciaInputPort.Resultado resultado = new CerrarTomaAsistenciaUseCaseImpl(acceso, repositorio, eventos)
                .execute(new CerrarTomaAsistenciaInputPort.Comando(TOMA, null, USUARIO));

        assertEquals(3, resultado.totalAusentesMarcados());
        assertEquals(3, resultado.totalFaltantes());
        assertFalse(resultado.yaEstabaCerrada());
        assertEquals("CERRADA", resultado.toma().estado());
        final List<RealtimeEvent> publicados = eventosPublicados();
        assertEquals(List.of("TOMA_CERRADA", "ASISTENCIAS_TOMA_ACTUALIZADAS"), publicados.stream().map(RealtimeEvent::type).toList());
        assertEquals(Map.of("grupo", GRUPO.toString(), "sesion", SESION.toString(), "toma", TOMA.toString(), "numero", 2,
                "totalAusentesMarcados", 3, "totalFaltantes", 3, "automatico", false), publicados.get(0).payload());
        assertEquals(Map.of("grupo", GRUPO.toString(), "sesion", SESION.toString(), "toma", TOMA.toString(),
                "totalRegistros", 31), publicados.get(1).payload());
    }

    @Test
    void cerrar_sin_ausentes_solo_publica_toma_cerrada_y_una_toma_ya_cerrada_no_publica() {
        when(repositorio.consultarToma(TOMA)).thenReturn(Optional.of(toma("CERRADA", 28)));
        final CerrarTomaAsistenciaUseCaseImpl useCase = new CerrarTomaAsistenciaUseCaseImpl(acceso, repositorio, eventos);

        when(repositorio.cerrar(TOMA, false, USUARIO)).thenReturn(
                new TomaAsistenciaRepositoryPort.CierreToma(TOMA, SESION, GRUPO, 0, 2, false));
        useCase.execute(new CerrarTomaAsistenciaInputPort.Comando(TOMA, false, USUARIO));
        verify(realtime, times(1)).publish(any());

        when(repositorio.cerrar(TOMA, true, USUARIO)).thenReturn(
                new TomaAsistenciaRepositoryPort.CierreToma(TOMA, SESION, GRUPO, 0, 0, true));
        assertTrue(useCase.execute(new CerrarTomaAsistenciaInputPort.Comando(TOMA, true, USUARIO)).yaEstabaCerrada());
        verify(realtime, times(1)).publish(any());
    }

    @Test
    void cerrar_rechaza_toma_inexistente_y_ajena_sin_llamar_al_sp() {
        final CerrarTomaAsistenciaUseCaseImpl useCase = new CerrarTomaAsistenciaUseCaseImpl(acceso, repositorio, eventos);
        when(repositorio.consultarToma(TOMA)).thenReturn(Optional.empty());

        assertEquals("TOM_001", assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(new CerrarTomaAsistenciaInputPort.Comando(TOMA, null, USUARIO))).getCode());

        when(repositorio.consultarToma(TOMA)).thenReturn(Optional.of(toma("ABIERTA", 0)));
        assertThrows(ForbiddenException.class,
                () -> useCase.execute(new CerrarTomaAsistenciaInputPort.Comando(TOMA, null, UUID.randomUUID())));
        verify(repositorio, never()).cerrar(any(), any(), any());
        verifyNoInteractions(realtime);
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void releer_una_toma_que_desaparece_responde_tom_001() {
        when(repositorio.consultarToma(TOMA)).thenReturn(Optional.empty());

        assertEquals("TOM_001", assertThrows(ResourceNotFoundException.class, () -> acceso.releer(TOMA)).getCode());
    }

    // ------------------------------------------------------------------ registrar lote

    @Test
    void registrar_lote_de_la_toma_llama_al_sp_con_estados_normalizados_y_publica() {
        when(repositorio.consultarToma(TOMA)).thenReturn(Optional.of(toma("ABIERTA", 0)));
        final UUID e1 = UUID.randomUUID();
        final UUID e2 = UUID.randomUUID();
        when(repositorio.registrarLote(any(), any(), any())).thenReturn(
                new TomaAsistenciaRepositoryPort.LoteRegistrado(TOMA, SESION, GRUPO, 2));

        final RegistrarAsistenciasTomaInputPort.Resultado resultado = new RegistrarAsistenciasTomaUseCaseImpl(acceso, repositorio, eventos)
                .execute(new RegistrarAsistenciasTomaInputPort.Comando(TOMA, List.of(
                        new RegistrarAsistenciasTomaInputPort.Registro(e1, " AN "),
                        new RegistrarAsistenciasTomaInputPort.Registro(e2, "SJC")), USUARIO));

        assertEquals(new RegistrarAsistenciasTomaInputPort.Resultado(TOMA, SESION, 2), resultado);
        verify(repositorio).registrarLote(TOMA, List.of(
                new TomaAsistenciaRepositoryPort.RegistroToma(e1, "AN"),
                new TomaAsistenciaRepositoryPort.RegistroToma(e2, "SJC")), USUARIO);
        final List<RealtimeEvent> publicados = eventosPublicados();
        assertEquals("ASISTENCIAS_TOMA_ACTUALIZADAS", publicados.getFirst().type());
        assertEquals(Map.of("grupo", GRUPO.toString(), "sesion", SESION.toString(), "toma", TOMA.toString(),
                "totalRegistros", 2), publicados.getFirst().payload());
    }

    @Test
    void registrar_lote_valida_forma_vacio_excedido_nulos_y_duplicados_sin_tocar_la_db() {
        final RegistrarAsistenciasTomaUseCaseImpl useCase = new RegistrarAsistenciasTomaUseCaseImpl(acceso, repositorio, eventos);
        final UUID e1 = UUID.randomUUID();
        final List<RegistrarAsistenciasTomaInputPort.Registro> excedido = new ArrayList<>();
        for (int i = 0; i < 201; i++) {
            excedido.add(new RegistrarAsistenciasTomaInputPort.Registro(UUID.randomUUID(), "AN"));
        }
        final List<List<RegistrarAsistenciasTomaInputPort.Registro>> invalidos = new ArrayList<>(List.of(
                List.of(),
                excedido,
                Collections.singletonList(null),
                List.of(new RegistrarAsistenciasTomaInputPort.Registro(null, "AN")),
                List.of(new RegistrarAsistenciasTomaInputPort.Registro(e1, null)),
                List.of(new RegistrarAsistenciasTomaInputPort.Registro(e1, " ")),
                List.of(new RegistrarAsistenciasTomaInputPort.Registro(e1, "AN"), new RegistrarAsistenciasTomaInputPort.Registro(e1, "EX"))));
        invalidos.add(null);
        for (final List<RegistrarAsistenciasTomaInputPort.Registro> registros : invalidos) {
            assertEquals("VALIDATION_ERROR", assertThrows(ValidationException.class,
                    () -> useCase.execute(new RegistrarAsistenciasTomaInputPort.Comando(TOMA, registros, USUARIO))).getCode());
        }
        verifyNoInteractions(repositorio, realtime);
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    // ------------------------------------------------------------------ consultas paginadas

    @Test
    void consultas_paginadas_aplican_valores_por_defecto_y_exigen_titularidad() {
        when(repositorio.consultarToma(TOMA)).thenReturn(Optional.of(toma("ABIERTA", 1)));
        final PaginaResultado<AsistenciaTomaFila> asistencias = new PaginaResultado<>(List.of(
                new AsistenciaTomaFila(UUID.randomUUID(), "Ana Perez", "AN", true, APERTURA)), 1, 50, 1);
        final PaginaResultado<FaltanteTomaFila> faltantes = new PaginaResultado<>(null, 2, 10, 0);
        when(repositorio.consultarAsistencias(TOMA, 1, 50, USUARIO)).thenReturn(asistencias);
        when(repositorio.consultarFaltantes(TOMA, 2, 10, USUARIO)).thenReturn(faltantes);

        assertSame(asistencias, new ConsultarAsistenciasTomaUseCaseImpl(acceso, repositorio)
                .execute(new ConsultarAsistenciasTomaInputPort.Consulta(TOMA, null, null, USUARIO)));
        final PaginaResultado<FaltanteTomaFila> resultado = new ConsultarFaltantesTomaUseCaseImpl(acceso, repositorio)
                .execute(new ConsultarFaltantesTomaInputPort.Consulta(TOMA, 2, 10, USUARIO));
        assertTrue(resultado.elementos().isEmpty());

        final UUID ajeno = UUID.randomUUID();
        assertThrows(ForbiddenException.class, () -> new ConsultarFaltantesTomaUseCaseImpl(acceso, repositorio)
                .execute(new ConsultarFaltantesTomaInputPort.Consulta(TOMA, 1, 50, ajeno)));
        assertThrows(NullPointerException.class, () -> new ConsultarAsistenciasTomaUseCaseImpl(acceso, repositorio).execute(null));
        assertThrows(NullPointerException.class, () -> new ConsultarFaltantesTomaUseCaseImpl(acceso, repositorio).execute(null));
    }

    // ------------------------------------------------------------------ cierre automatico

    @Test
    void cerrar_tomas_vencidas_publica_toma_cerrada_automatica_por_cada_toma() {
        final UUID otra = UUID.randomUUID();
        when(repositorio.cerrarVencidas()).thenReturn(List.of(
                new TomaAsistenciaRepositoryPort.TomaCerradaAutomaticamente(TOMA, SESION, GRUPO, 4),
                new TomaAsistenciaRepositoryPort.TomaCerradaAutomaticamente(otra, SESION, GRUPO, 0)));
        when(repositorio.consultarToma(TOMA)).thenReturn(Optional.of(toma("CERRADA", 30)));
        when(repositorio.consultarToma(otra)).thenThrow(new IllegalStateException("lectura caida"));

        assertEquals(2, new CerrarTomasVencidasUseCaseImpl(repositorio, eventos).execute());

        final List<RealtimeEvent> publicados = eventosPublicados();
        assertEquals(2, publicados.size());
        assertEquals(Map.of("grupo", GRUPO.toString(), "sesion", SESION.toString(), "toma", TOMA.toString(), "numero", 2,
                "totalAusentesMarcados", 4, "automatico", true), publicados.get(0).payload());
        assertFalse(publicados.get(1).payload().containsKey("numero"));
        assertEquals("TOMA_CERRADA", publicados.get(1).type());
    }

    @Test
    void sin_tomas_vencidas_no_publica() {
        when(repositorio.cerrarVencidas()).thenReturn(List.of());

        assertEquals(0, new CerrarTomasVencidasUseCaseImpl(repositorio, eventos).execute());
        verifyNoInteractions(realtime);
    }

    @Test
    void los_componentes_exigen_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new TomaAsistenciaAcceso(null, scope));
        assertThrows(NullPointerException.class, () -> new TomaAsistenciaAcceso(repositorio, null));
        assertThrows(NullPointerException.class, () -> new SesionTitularAcceso(null, scope));
        assertThrows(NullPointerException.class, () -> new SesionTitularAcceso(sesiones, null));
        assertThrows(NullPointerException.class, () -> new TomaAsistenciaEventos(null));
        assertThrows(NullPointerException.class, () -> new AbrirTomaAsistenciaUseCaseImpl(null, repositorio, acceso, eventos));
        assertThrows(NullPointerException.class, () -> new AbrirTomaAsistenciaUseCaseImpl(sesionTitular, null, acceso, eventos));
        assertThrows(NullPointerException.class, () -> new AbrirTomaAsistenciaUseCaseImpl(sesionTitular, repositorio, null, eventos));
        assertThrows(NullPointerException.class, () -> new AbrirTomaAsistenciaUseCaseImpl(sesionTitular, repositorio, acceso, null));
        assertThrows(NullPointerException.class, () -> new ConsultarTomasSesionUseCaseImpl(null, repositorio));
        assertThrows(NullPointerException.class, () -> new ConsultarTomasSesionUseCaseImpl(sesionTitular, null));
        assertThrows(NullPointerException.class, () -> new CerrarTomaAsistenciaUseCaseImpl(null, repositorio, eventos));
        assertThrows(NullPointerException.class, () -> new CerrarTomaAsistenciaUseCaseImpl(acceso, null, eventos));
        assertThrows(NullPointerException.class, () -> new CerrarTomaAsistenciaUseCaseImpl(acceso, repositorio, null));
        assertThrows(NullPointerException.class, () -> new RegistrarAsistenciasTomaUseCaseImpl(null, repositorio, eventos));
        assertThrows(NullPointerException.class, () -> new RegistrarAsistenciasTomaUseCaseImpl(acceso, null, eventos));
        assertThrows(NullPointerException.class, () -> new RegistrarAsistenciasTomaUseCaseImpl(acceso, repositorio, null));
        assertThrows(NullPointerException.class, () -> new ConsultarAsistenciasTomaUseCaseImpl(null, repositorio));
        assertThrows(NullPointerException.class, () -> new ConsultarAsistenciasTomaUseCaseImpl(acceso, null));
        assertThrows(NullPointerException.class, () -> new ConsultarFaltantesTomaUseCaseImpl(null, repositorio));
        assertThrows(NullPointerException.class, () -> new ConsultarFaltantesTomaUseCaseImpl(acceso, null));
        assertThrows(NullPointerException.class, () -> new CerrarTomasVencidasUseCaseImpl(null, eventos));
        assertThrows(NullPointerException.class, () -> new CerrarTomasVencidasUseCaseImpl(repositorio, null));
    }
}
