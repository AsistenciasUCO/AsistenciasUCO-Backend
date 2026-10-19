package co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.dto.CrearFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.interactor.CrearFacultadInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.mapper.CrearFacultadMapper;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.usecase.CrearFacultadUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.usecase.domain.CrearFacultadDomain;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CrearFacultadPrimaryPortsTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID INSTITUTION_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID DEAN_ID = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final UUID ACTOR_ID = UUID.fromString("12345678-1234-1234-1234-123456789012");

    @Test
    void mapper_preserves_the_input_contract() {
        final CrearFacultadDTO dto = new CrearFacultadDTO(ID, "Ingenieria", INSTITUTION_ID, DEAN_ID, ACTOR_ID);

        final CrearFacultadDomain domain = CrearFacultadMapper.toDomain(dto);

        assertEquals(new CrearFacultadDomain(ID, "Ingenieria", INSTITUTION_ID, DEAN_ID, ACTOR_ID), domain);
    }

    @Test
    void mapper_returns_null_for_null_input() {
        assertNull(CrearFacultadMapper.toDomain(null));
    }

    @Test
    void interactor_maps_and_delegates_to_the_use_case() {
        final CrearFacultadUseCase useCase = mock(CrearFacultadUseCase.class);
        final CrearFacultadInteractor interactor = new CrearFacultadInteractor(useCase);
        final CrearFacultadDTO dto = new CrearFacultadDTO(ID, "Ingenieria", INSTITUTION_ID, DEAN_ID, ACTOR_ID);

        interactor.execute(dto);

        verify(useCase).execute(new CrearFacultadDomain(ID, "Ingenieria", INSTITUTION_ID, DEAN_ID, ACTOR_ID));
    }

    @Test
    void interactor_requires_a_use_case() {
        assertThrows(NullPointerException.class, () -> new CrearFacultadInteractor(null));
    }
}
