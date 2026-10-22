package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.usecase.domain.CambiarEstadoFacultadDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadCommandPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CambiarEstadoFacultadUseCaseImplTest {

    private static final UUID FACULTAD_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID ACTOR_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private FacultadCommandPort commandPort;
    private CambiarEstadoFacultadUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        commandPort = mock(FacultadCommandPort.class);
        useCase = new CambiarEstadoFacultadUseCaseImpl(commandPort);
    }

    @Test
    void inactivar_delega_al_puerto() {
        useCase.execute(new CambiarEstadoFacultadDomain(FACULTAD_ID, false, ACTOR_ID));

        final ArgumentCaptor<FacultadCommandPort.CambiarEstadoFacultadCommand> captor =
                ArgumentCaptor.forClass(FacultadCommandPort.CambiarEstadoFacultadCommand.class);
        verify(commandPort).cambiarEstadoFacultad(captor.capture());
        assertEquals(FACULTAD_ID, captor.getValue().id());
        assertFalse(captor.getValue().activa());
        assertEquals(ACTOR_ID, captor.getValue().actorUsuarioId());
    }

    @Test
    void activar_delega_al_puerto() {
        useCase.execute(new CambiarEstadoFacultadDomain(FACULTAD_ID, true, ACTOR_ID));

        final ArgumentCaptor<FacultadCommandPort.CambiarEstadoFacultadCommand> captor =
                ArgumentCaptor.forClass(FacultadCommandPort.CambiarEstadoFacultadCommand.class);
        verify(commandPort).cambiarEstadoFacultad(captor.capture());
        assertTrue(captor.getValue().activa());
    }

    @Test
    void domain_nulo_lanza_validationException() {
        final ValidationException ex = assertThrows(ValidationException.class, () -> useCase.execute(null));
        assertEquals("Los datos de la facultad son obligatorios.", ex.getMessage());
        verify(commandPort, never()).cambiarEstadoFacultad(any());
    }

    @Test
    void id_nulo_lanza_validationException() {
        final ValidationException ex = assertThrows(ValidationException.class,
                () -> useCase.execute(new CambiarEstadoFacultadDomain(null, true, ACTOR_ID)));
        assertEquals("El identificador de la facultad es obligatorio.", ex.getMessage());
        verify(commandPort, never()).cambiarEstadoFacultad(any());
    }

    @Test
    void estado_deseado_nulo_lanza_validationException() {
        final ValidationException ex = assertThrows(ValidationException.class,
                () -> useCase.execute(new CambiarEstadoFacultadDomain(FACULTAD_ID, null, ACTOR_ID)));
        assertEquals("El estado deseado de la facultad es obligatorio.", ex.getMessage());
        verify(commandPort, never()).cambiarEstadoFacultad(any());
    }

    @Test
    void conflicto_del_puerto_se_propaga() {
        doThrow(new ConflictException("Conflicto")).when(commandPort).cambiarEstadoFacultad(any());
        final CambiarEstadoFacultadDomain domain = new CambiarEstadoFacultadDomain(FACULTAD_ID, true, ACTOR_ID);
        assertThrows(ConflictException.class, () -> useCase.execute(domain));
    }

    @Test
    void puerto_obligatorio() {
        assertThrows(NullPointerException.class, () -> new CambiarEstadoFacultadUseCaseImpl(null));
    }
}
