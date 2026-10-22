package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports.dto.CambiarEstadoFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports.interactor.CambiarEstadoFacultadInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.primaryports.mapper.CambiarEstadoFacultadMapper;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.usecase.CambiarEstadoFacultadUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadofacultad.usecase.domain.CambiarEstadoFacultadDomain;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CambiarEstadoFacultadPrimaryPortsTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ACTOR = UUID.fromString("12345678-1234-1234-1234-123456789012");

    @Test
    void mapper_preserva_el_contrato_de_entrada() {
        assertEquals(new CambiarEstadoFacultadDomain(ID, false, ACTOR),
                CambiarEstadoFacultadMapper.toDomain(new CambiarEstadoFacultadDTO(ID, false, ACTOR)));
    }

    @Test
    void mapper_devuelve_null_para_entrada_nula() {
        assertNull(CambiarEstadoFacultadMapper.toDomain(null));
    }

    @Test
    void interactor_mapea_y_delega_al_caso_de_uso() {
        final CambiarEstadoFacultadUseCase useCase = mock(CambiarEstadoFacultadUseCase.class);

        new CambiarEstadoFacultadInteractor(useCase).execute(new CambiarEstadoFacultadDTO(ID, true, ACTOR));

        verify(useCase).execute(new CambiarEstadoFacultadDomain(ID, true, ACTOR));
    }

    @Test
    void interactor_requiere_caso_de_uso() {
        assertThrows(NullPointerException.class, () -> new CambiarEstadoFacultadInteractor(null));
    }
}
