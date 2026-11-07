package co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.domain.ResolverSolicitudRevisionAsistenciaDomain;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** NOTIF-002B: el aviso al estudiante es best-effort y nunca rompe la resolucion del reclamo. */
class ResolverSolicitudRevisionAsistenciaAvisoTest {

    private static final UUID SOLICITUD = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();

    private final AsistenciaRepositoryPort repository = mock(AsistenciaRepositoryPort.class);
    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final NotificacionPort notificacionPort = mock(NotificacionPort.class);
    private final DestinatariosReclamoPort destinatariosPort = mock(DestinatariosReclamoPort.class);
    private final ResolverSolicitudRevisionAsistenciaUseCaseImpl useCase =
            new ResolverSolicitudRevisionAsistenciaUseCaseImpl(repository, scope, notificacionPort, destinatariosPort);

    private static DestinatarioReclamoProjection destinatario(final String rol, final String grupo, final String fecha) {
        return new DestinatarioReclamoProjection(rol, UUID.randomUUID(), "Nombre " + rol, rol.toLowerCase() + "@uco.edu.co",
                UUID.randomUUID(), grupo, fecha);
    }

    private void docenteAutenticado() {
        when(scope.findDocenteIdByUsuario(USUARIO)).thenReturn(Optional.of(UUID.randomUUID()));
    }

    private static ResolverSolicitudRevisionAsistenciaDomain resolucion(final String respuesta) {
        return new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "RECHAZADA", respuesta, USUARIO);
    }

    @Test
    void avisa_al_estudiante_con_la_decision_y_la_observacion() {
        docenteAutenticado();
        when(destinatariosPort.consultarDestinatariosPorSolicitud(SOLICITUD)).thenReturn(List.of(
                destinatario("ESTUDIANTE", "G01", "2026-08-24"), destinatario("DOCENTE", "G01", "2026-08-24")));

        useCase.execute(resolucion("No procede"));

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        final Notificacion enviada = captor.getValue();
        assertEquals(Notificacion.TIPO_RECLAMO_ASISTENCIA_RESUELTO, enviada.tipo());
        assertEquals("ESTUDIANTE", enviada.destinatarios().getFirst().rol());
        assertEquals("RECHAZADA", enviada.datos().get("estado"));
        assertEquals("No procede", enviada.datos().get("observacion"));
        assertEquals("G01", enviada.datos().get("grupo"));
    }

    @Test
    void sin_observacion_ni_grupo_usa_respaldos_y_omite_la_observacion() {
        docenteAutenticado();
        when(destinatariosPort.consultarDestinatariosPorSolicitud(SOLICITUD)).thenReturn(List.of(
                destinatario("ESTUDIANTE", "", " ")));

        useCase.execute(resolucion(null));

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        assertEquals("el grupo", captor.getValue().datos().get("grupo"));
        assertEquals("la sesión", captor.getValue().datos().get("fecha"));
        assertFalse(captor.getValue().datos().containsKey("observacion"));
    }

    @Test
    void no_avisa_si_no_hay_estudiante_destinatario() {
        docenteAutenticado();
        when(destinatariosPort.consultarDestinatariosPorSolicitud(SOLICITUD)).thenReturn(List.of(
                destinatario("DOCENTE", "G01", "2026-08-24")));

        useCase.execute(resolucion("x"));

        verify(notificacionPort, never()).notificar(any());
    }

    @Test
    void no_avisa_si_la_consulta_viene_vacia_o_nula() {
        docenteAutenticado();
        when(destinatariosPort.consultarDestinatariosPorSolicitud(SOLICITUD)).thenReturn(List.of(), (List<DestinatarioReclamoProjection>) null);

        useCase.execute(resolucion("x"));
        useCase.execute(resolucion("x"));

        verify(notificacionPort, never()).notificar(any());
    }

    @Test
    void sin_puerto_de_destinatarios_resuelve_sin_avisar() {
        docenteAutenticado();
        final var sinPuerto = new ResolverSolicitudRevisionAsistenciaUseCaseImpl(repository, scope, notificacionPort, null);

        sinPuerto.execute(resolucion("x"));

        verify(repository).resolverSolicitudRevisionAsistencia(any());
        verify(notificacionPort, never()).notificar(any());
    }

    @Test
    void un_fallo_del_aviso_no_rompe_la_resolucion() {
        docenteAutenticado();
        when(destinatariosPort.consultarDestinatariosPorSolicitud(SOLICITUD)).thenReturn(List.of(
                destinatario("ESTUDIANTE", "G01", "2026-08-24")));
        doThrow(new IllegalStateException("smtp caido")).when(notificacionPort).notificar(any());

        assertDoesNotThrow(() -> useCase.execute(resolucion("x")));
    }
}
