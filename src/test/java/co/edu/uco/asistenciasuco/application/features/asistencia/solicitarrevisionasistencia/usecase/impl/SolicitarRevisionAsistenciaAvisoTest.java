package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.domain.SolicitarRevisionAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DestinatariosReclamoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioReclamoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** NOTIF-002B: el aviso al docente es best-effort y nunca rompe la radicacion del reclamo. */
class SolicitarRevisionAsistenciaAvisoTest {

    private static final UUID SESION = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    private final AsistenciaRepositoryPort repository = mock(AsistenciaRepositoryPort.class);
    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final NotificacionPort notificacionPort = mock(NotificacionPort.class);
    private final DestinatariosReclamoPort destinatariosPort = mock(DestinatariosReclamoPort.class);
    private final SolicitarRevisionAsistenciaUseCaseImpl useCase =
            new SolicitarRevisionAsistenciaUseCaseImpl(repository, scope, notificacionPort, destinatariosPort);

    private static SolicitarRevisionAsistenciaDomain domain() {
        return new SolicitarRevisionAsistenciaDomain(SESION, "MED", "Incapacidad medica", null, USUARIO);
    }

    private static DestinatarioReclamoProjection destinatario(final String rol, final String grupo, final String fecha) {
        return new DestinatarioReclamoProjection(rol, UUID.randomUUID(), "Nombre " + rol, rol.toLowerCase() + "@uco.edu.co",
                UUID.randomUUID(), grupo, fecha);
    }

    @Test
    void avisa_al_docente_con_los_datos_del_reclamo() {
        when(scope.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(ESTUDIANTE));
        when(destinatariosPort.consultarDestinatariosPorSesion(SESION, ESTUDIANTE)).thenReturn(List.of(
                destinatario("ESTUDIANTE", "G01", "2026-08-24"), destinatario("DOCENTE", "G01", "2026-08-24")));

        useCase.execute(domain());

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        final Notificacion enviada = captor.getValue();
        assertEquals(Notificacion.TIPO_RECLAMO_ASISTENCIA_RADICADO, enviada.tipo());
        assertEquals(1, enviada.destinatarios().size());
        assertEquals("DOCENTE", enviada.destinatarios().getFirst().rol());
        assertEquals("G01", enviada.datos().get("grupo"));
        assertEquals("Nombre ESTUDIANTE", enviada.datos().get("estudiante"));
        assertEquals("2026-08-24", enviada.datos().get("fecha"));
        assertEquals("Incapacidad medica", enviada.datos().get("motivo"));
    }

    @Test
    void usa_textos_de_respaldo_si_faltan_grupo_y_fecha() {
        when(scope.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(ESTUDIANTE));
        when(destinatariosPort.consultarDestinatariosPorSesion(SESION, ESTUDIANTE)).thenReturn(List.of(
                destinatario("DOCENTE", " ", "")));

        useCase.execute(domain());

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        assertEquals("el grupo", captor.getValue().datos().get("grupo"));
        assertEquals("la sesión", captor.getValue().datos().get("fecha"));
        assertEquals("el estudiante", captor.getValue().datos().get("estudiante"));
    }

    @Test
    void no_avisa_si_no_hay_docente_destinatario() {
        when(scope.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(ESTUDIANTE));
        when(destinatariosPort.consultarDestinatariosPorSesion(SESION, ESTUDIANTE)).thenReturn(List.of(
                destinatario("ESTUDIANTE", "G01", "2026-08-24")));

        useCase.execute(domain());

        verify(notificacionPort, never()).notificar(any());
    }

    @Test
    void no_avisa_si_la_consulta_no_devuelve_destinatarios() {
        when(scope.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(ESTUDIANTE));
        when(destinatariosPort.consultarDestinatariosPorSesion(SESION, ESTUDIANTE)).thenReturn(List.of());

        useCase.execute(domain());

        verify(notificacionPort, never()).notificar(any());
    }

    @Test
    void sin_puerto_de_destinatarios_no_avisa_pero_persiste() {
        when(scope.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(ESTUDIANTE));
        final var sinPuerto = new SolicitarRevisionAsistenciaUseCaseImpl(repository, scope, notificacionPort, null);

        sinPuerto.execute(domain());

        verify(repository).solicitarRevisionAsistencia(any());
        verify(notificacionPort, never()).notificar(any());
    }

    @Test
    void un_fallo_del_aviso_no_rompe_la_radicacion() {
        when(scope.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(ESTUDIANTE));
        when(destinatariosPort.consultarDestinatariosPorSesion(SESION, ESTUDIANTE)).thenReturn(List.of(
                destinatario("DOCENTE", "G01", "2026-08-24")));
        doThrow(new IllegalStateException("smtp caido")).when(notificacionPort).notificar(any());

        assertDoesNotThrow(() -> useCase.execute(domain()));

        verify(repository).solicitarRevisionAsistencia(any());
    }

    @Test
    void el_constructor_corto_persiste_sin_avisar() {
        when(scope.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(ESTUDIANTE));
        final var corto = new SolicitarRevisionAsistenciaUseCaseImpl(repository, scope);

        assertDoesNotThrow(() -> corto.execute(domain()));

        verify(repository).solicitarRevisionAsistencia(any());
    }
}
