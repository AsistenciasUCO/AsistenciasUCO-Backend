package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones;

import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.primaryports.interactor.AdministrarNotificacionesInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.impl.AdministrarNotificacionesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.admin.AdministracionNotificacionPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class AdministrarNotificacionesUseCaseImplTest {

    private final AdministracionNotificacionPort port = mock(AdministracionNotificacionPort.class);
    private final AdministrarNotificacionesUseCaseImpl useCase = new AdministrarNotificacionesUseCaseImpl(port);
    private final AdministrarNotificacionesInteractor interactor = new AdministrarNotificacionesInteractor(useCase);
    private final UUID admin = UUID.randomUUID();

    @Test
    void exigeDependencias() {
        assertThrows(NullPointerException.class, () -> new AdministrarNotificacionesUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> new AdministrarNotificacionesInteractor(null));
    }

    @Test
    void delegaConsultasEnElPuertoSecundario() {
        when(port.consultarPlantillas("es", admin)).thenReturn(List.of());
        assertEquals(List.of(), interactor.consultarPlantillas("es", admin));
        interactor.consultarEstadoCanal();
        interactor.consultarCola("TODOS", "TODOS", 1, 20, admin);
        interactor.crearVersionPlantilla("T", "es", "a", "c", admin);
        interactor.activarVersionPlantilla("T", "es", 2, admin);
        interactor.generarVistaPrevia("T", "es", "a", "c");
        final UUID id = UUID.randomUUID();
        interactor.reintentarNotificacion(id, admin);
        verify(port).consultarEstadoCanal();
        verify(port).consultarCola("TODOS", "TODOS", 1, 20, admin);
        verify(port).crearVersionPlantilla("T", "es", "a", "c", admin);
        verify(port).activarVersionPlantilla("T", "es", 2, admin);
        verify(port).generarVistaPrevia("T", "es", "a", "c");
        verify(port).reintentarNotificacion(id, admin);
    }

    @Test
    void correoDePruebaConTipoUsaLaFirmaConTipo() {
        interactor.enviarCorreoPrueba(admin, "VERIFICACION_CORREO", "a@b.co");
        verify(port).enviarCorreoPrueba(admin, "VERIFICACION_CORREO", "a@b.co");
    }

    @Test
    void correoDePruebaSinTipoUsaLaFirmaSimple() {
        interactor.enviarCorreoPrueba(admin, " ", "a@b.co");
        verify(port).enviarCorreoPrueba(admin, "a@b.co");
        verifyNoMoreInteractions(port);
    }
}
