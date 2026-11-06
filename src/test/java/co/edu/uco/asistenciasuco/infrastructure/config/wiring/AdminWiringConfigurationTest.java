package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.interactor.ConsultarAreasInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.usecase.impl.ConsultarAreasUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.interactor.ConsultarDecanosInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase.impl.ConsultarDecanosUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.interactor.ConsultarFacultadesInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.usecase.impl.ConsultarFacultadesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.interactor.ConsultarInstitucionesInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.usecase.impl.ConsultarInstitucionesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.interactor.ConsultarParametrosInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.impl.ConsultarParametrosUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.interactor.CrearDecanoInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.impl.CrearDecanoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.interactor.EjecutarCierreMasivoInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.impl.EjecutarCierreMasivoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CierrePeriodoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.InstitucionQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ParametroQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class AdminWiringConfigurationTest {

    private final AdminWiringConfiguration config = new AdminWiringConfiguration();

    @Test
    void todos_los_beans_se_construyen_con_el_adapter_esperado() {
        final var consultarDecanosUseCase = config.consultarDecanosUseCase(mock(DecanoQueryPort.class));
        assertInstanceOf(ConsultarDecanosUseCaseImpl.class, consultarDecanosUseCase);
        assertInstanceOf(ConsultarDecanosInteractor.class, config.consultarDecanosInputPort(consultarDecanosUseCase));

        final var crearDecanoUseCase = config.crearDecanoUseCase(
                mock(FacultadQueryPort.class), mock(DecanoCommandPort.class),
                mock(UsuarioRepositoryPort.class), mock(PasswordEncoderPort.class), mock(IdentityProviderPort.class));
        assertInstanceOf(CrearDecanoUseCaseImpl.class, crearDecanoUseCase);
        assertInstanceOf(CrearDecanoInteractor.class, config.crearDecanoInputPort(crearDecanoUseCase));

        final var consultarParametrosUseCase = config.consultarParametrosUseCase(mock(ParametroQueryPort.class));
        assertInstanceOf(ConsultarParametrosUseCaseImpl.class, consultarParametrosUseCase);
        assertInstanceOf(ConsultarParametrosInteractor.class, config.consultarParametrosInputPort(consultarParametrosUseCase));

        final var consultarInstitucionesUseCase = config.consultarInstitucionesUseCase(mock(InstitucionQueryPort.class));
        assertInstanceOf(ConsultarInstitucionesUseCaseImpl.class, consultarInstitucionesUseCase);
        assertInstanceOf(ConsultarInstitucionesInteractor.class,
                config.consultarInstitucionesInputPort(consultarInstitucionesUseCase));

        final var consultarFacultadesUseCase = config.consultarFacultadesUseCase(mock(FacultadQueryPort.class));
        assertInstanceOf(ConsultarFacultadesUseCaseImpl.class, consultarFacultadesUseCase);
        assertInstanceOf(ConsultarFacultadesInteractor.class, config.consultarFacultadesInputPort(consultarFacultadesUseCase));

        final var consultarAreasUseCase = config.consultarAreasUseCase(mock(AreaQueryPort.class));
        assertInstanceOf(ConsultarAreasUseCaseImpl.class, consultarAreasUseCase);
        assertInstanceOf(ConsultarAreasInteractor.class, config.consultarAreasInputPort(consultarAreasUseCase));

        final var ejecutarCierreMasivoUseCase = config.ejecutarCierreMasivoUseCase(
                mock(PeriodoAcademicoQueryPort.class), mock(CierrePeriodoCommandPort.class), providerVacio());
        assertInstanceOf(EjecutarCierreMasivoUseCaseImpl.class, ejecutarCierreMasivoUseCase);
        assertInstanceOf(EjecutarCierreMasivoInteractor.class,
                config.ejecutarCierreMasivoInputPort(ejecutarCierreMasivoUseCase));
    }

    @SuppressWarnings("unchecked")
    private static <T> org.springframework.beans.factory.ObjectProvider<T> providerVacio() {
        return mock(org.springframework.beans.factory.ObjectProvider.class);
    }
}
