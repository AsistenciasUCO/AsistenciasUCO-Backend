package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.interactor.ConsultasTransversalesInteractor;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase.impl.ConsultasTransversalesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

/** HU112/HU128/HU143/HU157: el wiring construye el caso de uso y el interactor con el port de consulta. */
class ConsultasTransversalesWiringConfigurationTest {

    private final ConsultasTransversalesWiringConfiguration config = new ConsultasTransversalesWiringConfiguration();

    @Test
    void los_beans_se_construyen_con_el_port_esperado() {
        final var useCase = config.consultasTransversalesUseCase(mock(ConsultasTransversalesQueryPort.class));
        assertInstanceOf(ConsultasTransversalesUseCaseImpl.class, useCase);
        assertInstanceOf(ConsultasTransversalesInteractor.class, config.consultasTransversalesInputPort(useCase));
    }
}
