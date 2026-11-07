package co.edu.uco.asistenciasuco.application.features.sesioncancelacion.cancelarsesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesioncancelacion.cancelarsesion.primaryports.CancelarSesionInputPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort.DestinatarioSesionCancelada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort.SesionCancelada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * CULM-001 B.1 (HU047, CONTRACT 3.2.1, 3.7, 3.8): cancelacion por el docente titular. Exito con aviso por estudiante y
 * evento SSE sin motivo; titularidad y sesion inexistente antes del SP; errores de la DB sin aviso ni evento; avisos y
 * evento de mejor esfuerzo.
 */
class CancelarSesionUseCaseImplTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DOCENTE = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final LocalDateTime INICIO_UTC = LocalDateTime.of(2026, 11, 10, 13, 0);
    private static final LocalDateTime FIN_UTC = LocalDateTime.of(2026, 11, 10, 15, 0);
    private static final LocalDateTime CANCELADA_UTC = LocalDateTime.of(2026, 11, 7, 14, 0);
    private static final String MOTIVO = "Calamidad domestica del docente";

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final SesionProcedenciaQueryPort sesiones = mock(SesionProcedenciaQueryPort.class);
    private final SesionCancelacionRepositoryPort repositorio = mock(SesionCancelacionRepositoryPort.class);
    private final NotificacionPort notificaciones = mock(NotificacionPort.class);
    private final RealtimePublisherPort realtime = mock(RealtimePublisherPort.class);
    private final CancelarSesionUseCaseImpl useCase =
            new CancelarSesionUseCaseImpl(scope, sesiones, repositorio, notificaciones, realtime);

    @Test
    void cancela_avisa_a_cada_estudiante_y_publica_el_evento_del_grupo() {
        prepararSesion("UTC_V2");
        when(repositorio.cancelar(SESION, MOTIVO, DOCENTE)).thenReturn(new SesionCancelada(SESION, GRUPO, CANCELADA_UTC, 2));
        when(repositorio.consultarDestinatarios(SESION, DOCENTE)).thenReturn(List.of(
                destinatario("Ana Perez", "ana@uco.edu.co", MOTIVO), destinatario("Luis Gomez", "luis@uco.edu.co", null)));

        final CancelarSesionInputPort.Resultado resultado =
                useCase.execute(new CancelarSesionInputPort.Comando(SESION, "  " + MOTIVO + "  ", DOCENTE));

        assertEquals(new CancelarSesionInputPort.Resultado(SESION, GRUPO, EstadoSesion.CANCELADA,
                new CancelacionSesionDTO("DOCENTE", MOTIVO, null, Instant.parse("2026-11-07T14:00:00Z")), 2), resultado);

        final ArgumentCaptor<Notificacion> avisos = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones, times(2)).notificar(avisos.capture());
        final Notificacion aviso = avisos.getAllValues().getFirst();
        assertEquals(Notificacion.TIPO_SESION_CANCELADA, aviso.tipo());
        assertEquals("Clase cancelada: Calculo (10/11/2026)", aviso.asunto());
        assertEquals("Hola Ana Perez. El docente Maria Docente canceló la clase de Calculo del grupo G01 de Calculo programada "
                + "para el 10/11/2026 de 08:00 a 10:00. Motivo: " + MOTIVO + ".", aviso.mensaje());
        assertEquals(List.of(new Notificacion.Destinatario("ESTUDIANTE", aviso.destinatarios().getFirst().idUsuario(),
                "Ana Perez", "ana@uco.edu.co")), aviso.destinatarios());
        assertEquals(Map.of("estudiante", "Ana Perez", "asignatura", "Calculo", "grupo", "G01 de Calculo",
                "fecha", "10/11/2026", "horaInicio", "08:00", "horaFin", "10:00", "motivo", MOTIVO,
                "docente", "Maria Docente", "idSesion", SESION.toString()), aviso.datos());
        // Sin motivo en la fila del destinatario se usa el motivo recortado del comando.
        assertEquals(MOTIVO, avisos.getAllValues().get(1).datos().get("motivo"));

        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals("SESION_CANCELADA", evento.getValue().type());
        assertEquals(Map.of("grupo", GRUPO.toString(), "sesion", SESION.toString(), "tipo", "DOCENTE",
                "fechaHoraInicioUtc", "2026-11-10T13:00:00Z"), evento.getValue().payload());
        assertFalse(evento.getValue().payload().containsKey("motivo"));
    }

    @Test
    void una_sesion_con_horario_indeterminado_publica_el_evento_sin_hora_y_sin_destinatarios_no_avisa() {
        prepararSesion(null);
        when(repositorio.cancelar(any(), any(), any())).thenReturn(new SesionCancelada(SESION, GRUPO, null, 0));
        when(repositorio.consultarDestinatarios(SESION, DOCENTE)).thenReturn(List.of());

        final CancelarSesionInputPort.Resultado resultado =
                useCase.execute(new CancelarSesionInputPort.Comando(SESION, null, DOCENTE));

        assertEquals(0, resultado.estudiantesNotificados());
        assertEquals(null, resultado.cancelacion().fechaCancelacionUtc());
        assertEquals(null, resultado.cancelacion().motivo());
        verify(repositorio).cancelar(SESION, null, DOCENTE);
        verifyNoInteractions(notificaciones);
        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertFalse(evento.getValue().payload().containsKey("fechaHoraInicioUtc"));
    }

    @Test
    void sesion_confirmada_sin_hora_de_inicio_tampoco_publica_la_hora() {
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(new SesionProcedenciaRepositoryProjection(
                SESION, GRUPO, "Sesion 1", 1, "S-1", 1, "1", "G01", null, null, "UTC_V2")));
        when(scope.canDocenteAccessGrupo(DOCENTE, GRUPO)).thenReturn(true);
        when(repositorio.cancelar(any(), any(), any())).thenReturn(new SesionCancelada(SESION, GRUPO, CANCELADA_UTC, 0));
        when(repositorio.consultarDestinatarios(SESION, DOCENTE)).thenReturn(List.of(
                new DestinatarioSesionCancelada(UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, null, null, null, null)));

        assertEquals(1, useCase.execute(new CancelarSesionInputPort.Comando(SESION, MOTIVO, DOCENTE)).estudiantesNotificados());
        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertFalse(evento.getValue().payload().containsKey("fechaHoraInicioUtc"));
        final ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(aviso.capture());
        assertEquals("", aviso.getValue().datos().get("fecha"));
    }

    @Test
    void sesion_inexistente_responde_404_sin_llamar_al_sp() {
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.empty());
        final ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(new CancelarSesionInputPort.Comando(SESION, MOTIVO, DOCENTE)));
        assertEquals(SesionErrorCode.ERR_SESION_NO_EXISTE.code(), error.getCode());
        verifyNoInteractions(repositorio, notificaciones, realtime);
    }

    @Test
    void docente_sin_titularidad_responde_403_sin_llamar_al_sp() {
        prepararSesion("UTC_V2");
        when(scope.canDocenteAccessGrupo(DOCENTE, GRUPO)).thenReturn(false);
        assertThrows(ForbiddenException.class, () -> useCase.execute(new CancelarSesionInputPort.Comando(SESION, MOTIVO, DOCENTE)));
        verifyNoInteractions(repositorio, notificaciones, realtime);
    }

    @Test
    void un_rechazo_de_la_db_no_avisa_ni_publica_el_evento() {
        prepararSesion("UTC_V2");
        when(repositorio.cancelar(SESION, MOTIVO, DOCENTE)).thenThrow(new ConflictException(SesionErrorCode.SES_012));
        assertEquals("SES_012", assertThrows(ConflictException.class,
                () -> useCase.execute(new CancelarSesionInputPort.Comando(SESION, MOTIVO, DOCENTE))).getCode());
        verify(repositorio, never()).consultarDestinatarios(any(), any());
        verifyNoInteractions(notificaciones, realtime);
    }

    @Test
    void los_fallos_del_aviso_y_del_evento_no_cambian_la_respuesta() {
        prepararSesion("UTC_V2");
        when(repositorio.cancelar(SESION, MOTIVO, DOCENTE)).thenReturn(new SesionCancelada(SESION, GRUPO, CANCELADA_UTC, 2));
        when(repositorio.consultarDestinatarios(SESION, DOCENTE)).thenReturn(List.of(
                destinatario("Ana Perez", "ana@uco.edu.co", MOTIVO), destinatario("Luis Gomez", "luis@uco.edu.co", MOTIVO)));
        doThrow(new IllegalStateException("smtp")).doNothing().when(notificaciones).notificar(any());
        doThrow(new IllegalStateException("sse")).when(realtime).publish(any());

        assertEquals(1, useCase.execute(new CancelarSesionInputPort.Comando(SESION, MOTIVO, DOCENTE)).estudiantesNotificados());
    }

    @Test
    void si_no_se_pueden_consultar_los_destinatarios_se_notifica_a_cero_y_se_publica_el_evento() {
        prepararSesion("UTC_V2");
        when(repositorio.cancelar(SESION, MOTIVO, DOCENTE)).thenReturn(new SesionCancelada(SESION, GRUPO, CANCELADA_UTC, 2));
        when(repositorio.consultarDestinatarios(SESION, DOCENTE)).thenThrow(new IllegalStateException("db"));

        assertEquals(0, useCase.execute(new CancelarSesionInputPort.Comando(SESION, MOTIVO, DOCENTE)).estudiantesNotificados());
        verify(realtime).publish(any());
    }

    @Test
    void el_comando_y_las_dependencias_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        assertThrows(NullPointerException.class, () -> new CancelarSesionUseCaseImpl(null, sesiones, repositorio, notificaciones, realtime));
        assertThrows(NullPointerException.class, () -> new CancelarSesionUseCaseImpl(scope, null, repositorio, notificaciones, realtime));
        assertThrows(NullPointerException.class, () -> new CancelarSesionUseCaseImpl(scope, sesiones, null, notificaciones, realtime));
        assertThrows(NullPointerException.class, () -> new CancelarSesionUseCaseImpl(scope, sesiones, repositorio, null, realtime));
        assertThrows(NullPointerException.class, () -> new CancelarSesionUseCaseImpl(scope, sesiones, repositorio, notificaciones, null));
    }

    private void prepararSesion(final String procedencia) {
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(new SesionProcedenciaRepositoryProjection(
                SESION, GRUPO, "Sesion 1", 1, "S-1", 1, "1", "G01", INICIO_UTC, FIN_UTC, procedencia)));
        when(scope.canDocenteAccessGrupo(DOCENTE, GRUPO)).thenReturn(true);
    }

    private static DestinatarioSesionCancelada destinatario(final String nombre, final String correo, final String motivo) {
        return new DestinatarioSesionCancelada(UUID.randomUUID(), UUID.randomUUID(), nombre, correo, "Calculo",
                "G01 de Calculo", "Maria Docente", INICIO_UTC, FIN_UTC, motivo);
    }
}
