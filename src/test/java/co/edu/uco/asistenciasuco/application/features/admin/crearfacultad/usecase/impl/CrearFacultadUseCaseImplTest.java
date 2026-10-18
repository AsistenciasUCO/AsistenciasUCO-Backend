package co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.usecase.domain.CrearFacultadDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadCommandPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class CrearFacultadUseCaseImplTest {

    private static final UUID ACTOR_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID INSTITUCION_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID DECANO_ID = UUID.fromString("99999999-8888-7777-6666-555555555555");

    private FacultadCommandPort commandPort;
    private CrearFacultadUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        commandPort = mock(FacultadCommandPort.class);
        useCase = new CrearFacultadUseCaseImpl(commandPort);
    }

    @Test
    void crearFacultad_valida_persiste_exitosamente() {
        final CrearFacultadDomain domain = new CrearFacultadDomain(
                null,
                "Facultad de Ingenieria",
                INSTITUCION_ID,
                DECANO_ID,
                ACTOR_ID
        );

        useCase.execute(domain);

        final ArgumentCaptor<FacultadCommandPort.CrearFacultadCommand> captor =
                ArgumentCaptor.forClass(FacultadCommandPort.CrearFacultadCommand.class);
        verify(commandPort).crearFacultad(captor.capture());

        final FacultadCommandPort.CrearFacultadCommand captured = captor.getValue();
        assertNotNull(captured.id());
        assertEquals("Facultad de Ingenieria", captured.nombre());
        assertEquals(INSTITUCION_ID, captured.idInstitucion());
        assertEquals(DECANO_ID, captured.idDecano());
        assertEquals(ACTOR_ID, captured.actorUsuarioId());
    }

    @Test
    void crearFacultad_domain_nulo_lanza_validationException() {
        final ValidationException ex = assertThrows(ValidationException.class, () -> useCase.execute(null));
        assertEquals("Los datos de la facultad son obligatorios.", ex.getMessage());
        verify(commandPort, never()).crearFacultad(any());
    }

    @Test
    void crearFacultad_nombre_vacio_lanza_validationException() {
        final CrearFacultadDomain domain = new CrearFacultadDomain(
                null,
                "   ",
                INSTITUCION_ID,
                DECANO_ID,
                ACTOR_ID
        );

        final ValidationException ex = assertThrows(ValidationException.class, () -> useCase.execute(domain));
        assertEquals("El nombre de la facultad es obligatorio y debe tener entre 3 y 50 caracteres.", ex.getMessage());
        verify(commandPort, never()).crearFacultad(any());
    }

    @Test
    void crearFacultad_nombre_corto_lanza_validationException() {
        final CrearFacultadDomain domain = new CrearFacultadDomain(
                null,
                "AB",
                INSTITUCION_ID,
                DECANO_ID,
                ACTOR_ID
        );

        final ValidationException ex = assertThrows(ValidationException.class, () -> useCase.execute(domain));
        assertEquals("El nombre de la facultad es obligatorio y debe tener entre 3 y 50 caracteres.", ex.getMessage());
        verify(commandPort, never()).crearFacultad(any());
    }

    @Test
    void crearFacultad_decano_nulo_lanza_validationException() {
        final CrearFacultadDomain domain = new CrearFacultadDomain(
                null,
                "Facultad de Ingenieria",
                INSTITUCION_ID,
                null,
                ACTOR_ID
        );

        final ValidationException ex = assertThrows(ValidationException.class, () -> useCase.execute(domain));
        assertEquals("El decano titular de la facultad es obligatorio.", ex.getMessage());
        verify(commandPort, never()).crearFacultad(any());
    }

    @Test
    void crearFacultad_error_persistencia_se_propaga() {
        final CrearFacultadDomain domain = new CrearFacultadDomain(
                null,
                "Facultad de Ciencias",
                INSTITUCION_ID,
                DECANO_ID,
                ACTOR_ID
        );

        doThrow(new RuntimeException("Error DB")).when(commandPort).crearFacultad(any());

        assertThrows(RuntimeException.class, () -> useCase.execute(domain));
    }
}
