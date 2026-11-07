package co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.domain.EjecutarCierreMasivoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CierrePeriodoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAcademicoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** NOTIF-002B: el aviso del cierre masivo es best-effort y nunca revierte ni rompe el cierre. */
class EjecutarCierreMasivoAvisoTest {

    private static final UUID PERIODO_ID = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();

    private final PeriodoAcademicoQueryPort periodos = mock(PeriodoAcademicoQueryPort.class);
    private final CierrePeriodoCommandPort cierre = mock(CierrePeriodoCommandPort.class);
    private final NotificacionPort notificacionPort = mock(NotificacionPort.class);
    private final EjecutarCierreMasivoUseCaseImpl useCase =
            new EjecutarCierreMasivoUseCaseImpl(periodos, cierre, notificacionPort);

    private void periodoExistente() {
        when(periodos.consultarPeriodoAcademicoPorId(PERIODO_ID)).thenReturn(Optional.of(new PeriodoAcademicoProjection(
                PERIODO_ID, UUID.randomUUID(), "Institucion", "Periodo 2026-1", "2026-1", null, null, 2026)));
    }

    @Test
    void avisa_al_administrador_con_el_periodo_cerrado() {
        periodoExistente();

        useCase.execute(new EjecutarCierreMasivoDomain(PERIODO_ID, ACTOR));

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        assertEquals(Notificacion.TIPO_CIERRE_MASIVO_PERIODO_EJECUTADO, captor.getValue().tipo());
        assertEquals("2026-1", captor.getValue().datos().get("periodo"));
        assertEquals(ACTOR, captor.getValue().destinatarios().getFirst().idUsuario());
    }

    @Test
    void un_fallo_del_aviso_no_rompe_el_cierre() {
        periodoExistente();
        doThrow(new IllegalStateException("smtp caido")).when(notificacionPort).notificar(any());

        assertDoesNotThrow(() -> useCase.execute(new EjecutarCierreMasivoDomain(PERIODO_ID, ACTOR)));

        verify(cierre).ejecutarCierreMasivoPeriodo("2026-1", ACTOR.toString(), ACTOR);
    }

    @Test
    void sin_puerto_de_notificacion_usa_uno_nulo() {
        periodoExistente();
        final var sinPuerto = new EjecutarCierreMasivoUseCaseImpl(periodos, cierre, null);

        assertDoesNotThrow(() -> sinPuerto.execute(new EjecutarCierreMasivoDomain(PERIODO_ID, ACTOR)));

        verify(cierre).ejecutarCierreMasivoPeriodo("2026-1", ACTOR.toString(), ACTOR);
    }
}
