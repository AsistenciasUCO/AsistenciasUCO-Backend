package co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.primaryports.dto.ActualizarFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.primaryports.interactor.ActualizarFacultadInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.primaryports.mapper.ActualizarFacultadMapper;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.usecase.ActualizarFacultadUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.actualizarfacultad.usecase.domain.ActualizarFacultadDomain;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ActualizarFacultadPrimaryPortsTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID DECANO = UUID.fromString("99999999-8888-7777-6666-555555555555");
    private static final UUID ACTOR = UUID.fromString("12345678-1234-1234-1234-123456789012");

    @Test
    void mapper_preserva_el_contrato_de_entrada() {
        final ActualizarFacultadDTO dto = new ActualizarFacultadDTO(ID, "Ingenieria", DECANO, ACTOR);

        assertEquals(new ActualizarFacultadDomain(ID, "Ingenieria", DECANO, ACTOR), ActualizarFacultadMapper.toDomain(dto));
    }

    @Test
    void mapper_devuelve_null_para_entrada_nula() {
        assertNull(ActualizarFacultadMapper.toDomain(null));
    }

    @Test
    void interactor_mapea_y_delega_al_caso_de_uso() {
        final ActualizarFacultadUseCase useCase = mock(ActualizarFacultadUseCase.class);

        new ActualizarFacultadInteractor(useCase).execute(new ActualizarFacultadDTO(ID, null, DECANO, ACTOR));

        verify(useCase).execute(new ActualizarFacultadDomain(ID, null, DECANO, ACTOR));
    }

    @Test
    void interactor_requiere_caso_de_uso() {
        assertThrows(NullPointerException.class, () -> new ActualizarFacultadInteractor(null));
    }
}
