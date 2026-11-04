package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.primaryports.dto.GuardarTipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.primaryports.interactor.GuardarTipoIdentificacionInteractor;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.usecase.domain.GuardarTipoIdentificacionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionCommandPort.GuardarTipoIdentificacionCommand;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** HU153: alta (codigo de 1 a 5 letras + nombre) y edicion del nombre de tipos de documento. */
class GuardarTipoIdentificacionUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ID = UUID.randomUUID();

    private final TipoIdentificacionCommandPort port = mock(TipoIdentificacionCommandPort.class);
    private final GuardarTipoIdentificacionUseCaseImpl useCase = new GuardarTipoIdentificacionUseCaseImpl(port);

    @Test
    void constructor_rechaza_port_nulo() {
        assertThrows(NullPointerException.class, () -> new GuardarTipoIdentificacionUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> new GuardarTipoIdentificacionInteractor(null));
    }

    @Test
    void T_153_UC_01_alta_normaliza_codigo_y_nombre() {
        useCase.execute(new GuardarTipoIdentificacionDomain(null, " pep ", "  Permiso especial  ", ACTOR));

        verify(port).guardarTipoIdentificacion(new GuardarTipoIdentificacionCommand(null, "PEP", "Permiso especial", ACTOR));
    }

    @Test
    void T_153_UC_02_edicion_ignora_el_codigo() {
        new GuardarTipoIdentificacionInteractor(useCase)
                .execute(new GuardarTipoIdentificacionDTO(ID, "XX", "Cedula de extranjeria", ACTOR));

        verify(port).guardarTipoIdentificacion(new GuardarTipoIdentificacionCommand(ID, null, "Cedula de extranjeria", ACTOR));
    }

    @Test
    void T_153_UC_03_rechaza_datos_invalidos_sin_llamar_al_provider() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        assertThrows(ValidationException.class, () -> new GuardarTipoIdentificacionInteractor(useCase).execute(null));
        assertThrows(ValidationException.class,
                () -> useCase.execute(new GuardarTipoIdentificacionDomain(null, null, "Pasaporte", ACTOR)));
        assertThrows(ValidationException.class,
                () -> useCase.execute(new GuardarTipoIdentificacionDomain(null, "CC1", "Pasaporte", ACTOR)));
        assertThrows(ValidationException.class,
                () -> useCase.execute(new GuardarTipoIdentificacionDomain(null, "PASAPO", "Pasaporte", ACTOR)));
        assertThrows(ValidationException.class,
                () -> useCase.execute(new GuardarTipoIdentificacionDomain(ID, null, " ab ", ACTOR)));
        assertThrows(ValidationException.class,
                () -> useCase.execute(new GuardarTipoIdentificacionDomain(ID, null, "x".repeat(51), ACTOR)));
        assertThrows(ValidationException.class,
                () -> useCase.execute(new GuardarTipoIdentificacionDomain(ID, null, null, ACTOR)));
        verifyNoInteractions(port);
    }
}
