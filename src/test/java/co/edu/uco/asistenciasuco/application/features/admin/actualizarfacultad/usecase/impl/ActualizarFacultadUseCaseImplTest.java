package co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.usecase.domain.ActualizarFacultadDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadCommandPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ActualizarFacultadUseCaseImplTest {

    private static final UUID FACULTAD_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID DECANO_ID = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final UUID ACTOR_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private FacultadCommandPort commandPort;
    private ActualizarFacultadUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        commandPort = mock(FacultadCommandPort.class);
        useCase = new ActualizarFacultadUseCaseImpl(commandPort);
    }

    @Test
    void renombrar_delega_al_puerto_con_nombre_recortado_y_sin_decano() {
        useCase.execute(new ActualizarFacultadDomain(FACULTAD_ID, "  Facultad de Ingenieria  ", null, ACTOR_ID));

        final ArgumentCaptor<FacultadCommandPort.ActualizarFacultadCommand> captor =
                ArgumentCaptor.forClass(FacultadCommandPort.ActualizarFacultadCommand.class);
        verify(commandPort).actualizarFacultad(captor.capture());
        assertEquals(FACULTAD_ID, captor.getValue().id());
        assertEquals("Facultad de Ingenieria", captor.getValue().nombre());
        assertNull(captor.getValue().idDecano());
        assertEquals(ACTOR_ID, captor.getValue().actorUsuarioId());
    }

    @Test
    void reasignar_decano_delega_al_puerto_sin_nombre() {
        useCase.execute(new ActualizarFacultadDomain(FACULTAD_ID, null, DECANO_ID, ACTOR_ID));

        final ArgumentCaptor<FacultadCommandPort.ActualizarFacultadCommand> captor =
                ArgumentCaptor.forClass(FacultadCommandPort.ActualizarFacultadCommand.class);
        verify(commandPort).actualizarFacultad(captor.capture());
        assertNull(captor.getValue().nombre());
        assertEquals(DECANO_ID, captor.getValue().idDecano());
    }

    @Test
    void renombrar_y_reasignar_en_una_llamada() {
        useCase.execute(new ActualizarFacultadDomain(FACULTAD_ID, "Ciencias", DECANO_ID, ACTOR_ID));

        final ArgumentCaptor<FacultadCommandPort.ActualizarFacultadCommand> captor =
                ArgumentCaptor.forClass(FacultadCommandPort.ActualizarFacultadCommand.class);
        verify(commandPort).actualizarFacultad(captor.capture());
        assertEquals("Ciencias", captor.getValue().nombre());
        assertEquals(DECANO_ID, captor.getValue().idDecano());
    }

    @Test
    void domain_nulo_lanza_validationException() {
        final ValidationException ex = assertThrows(ValidationException.class, () -> useCase.execute(null));
        assertEquals("Los datos de la facultad son obligatorios.", ex.getMessage());
        verify(commandPort, never()).actualizarFacultad(any());
    }

    @Test
    void id_nulo_lanza_validationException() {
        final ValidationException ex = assertThrows(ValidationException.class,
                () -> useCase.execute(new ActualizarFacultadDomain(null, "Ciencias", null, ACTOR_ID)));
        assertEquals("El identificador de la facultad es obligatorio.", ex.getMessage());
        verify(commandPort, never()).actualizarFacultad(any());
    }

    @Test
    void sin_nombre_ni_decano_lanza_validationException() {
        final ValidationException ex = assertThrows(ValidationException.class,
                () -> useCase.execute(new ActualizarFacultadDomain(FACULTAD_ID, null, null, ACTOR_ID)));
        assertEquals("Debe indicar al menos el nombre o el decano de la facultad.", ex.getMessage());
        verify(commandPort, never()).actualizarFacultad(any());
    }

    @Test
    void nombre_en_blanco_lanza_validationException() {
        final ValidationException ex = assertThrows(ValidationException.class,
                () -> useCase.execute(new ActualizarFacultadDomain(FACULTAD_ID, "   ", null, ACTOR_ID)));
        assertEquals("El nombre de la facultad debe tener entre 3 y 50 caracteres.", ex.getMessage());
        verify(commandPort, never()).actualizarFacultad(any());
    }

    @Test
    void nombre_corto_lanza_validationException() {
        assertThrows(ValidationException.class,
                () -> useCase.execute(new ActualizarFacultadDomain(FACULTAD_ID, "AB", null, ACTOR_ID)));
        verify(commandPort, never()).actualizarFacultad(any());
    }

    @Test
    void nombre_largo_lanza_validationException() {
        final String largo = "x".repeat(51);
        assertThrows(ValidationException.class,
                () -> useCase.execute(new ActualizarFacultadDomain(FACULTAD_ID, largo, null, ACTOR_ID)));
        verify(commandPort, never()).actualizarFacultad(any());
    }

    @Test
    void nombre_en_los_limites_3_y_50_es_valido() {
        useCase.execute(new ActualizarFacultadDomain(FACULTAD_ID, "abc", null, ACTOR_ID));
        useCase.execute(new ActualizarFacultadDomain(FACULTAD_ID, "x".repeat(50), null, ACTOR_ID));
        verify(commandPort, times(2)).actualizarFacultad(any());
    }

    @Test
    void conflicto_del_puerto_se_propaga() {
        doThrow(new ConflictException("Conflicto")).when(commandPort).actualizarFacultad(any());
        final ActualizarFacultadDomain domain = new ActualizarFacultadDomain(FACULTAD_ID, "Ciencias", null, ACTOR_ID);
        assertThrows(ConflictException.class, () -> useCase.execute(domain));
    }

    @Test
    void puerto_obligatorio() {
        assertThrows(NullPointerException.class, () -> new ActualizarFacultadUseCaseImpl(null));
    }
}
