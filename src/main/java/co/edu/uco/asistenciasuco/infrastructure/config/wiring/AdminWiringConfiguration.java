package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.ActualizarAreaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.interactor.ActualizarAreaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.usecase.ActualizarAreaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.usecase.impl.ActualizarAreaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.CrearAreaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.interactor.CrearAreaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.usecase.CrearAreaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.usecase.impl.CrearAreaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.ConsultarAreasInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.primaryports.interactor.ConsultarAreasInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.usecase.ConsultarAreasUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarareas.usecase.impl.ConsultarAreasUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.ConsultarDecanosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.primaryports.interactor.ConsultarDecanosInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase.ConsultarDecanosUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultardecanos.usecase.impl.ConsultarDecanosUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.ConsultarFacultadesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.primaryports.interactor.ConsultarFacultadesInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.usecase.ConsultarFacultadesUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarfacultades.usecase.impl.ConsultarFacultadesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.ConsultarInstitucionesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.interactor.ConsultarInstitucionesInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.usecase.ConsultarInstitucionesUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.usecase.impl.ConsultarInstitucionesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.ConsultarParametrosInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.interactor.ConsultarParametrosInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.ConsultarParametrosUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.usecase.impl.ConsultarParametrosUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.CrearDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.interactor.CrearDecanoInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.CrearDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.impl.CrearDecanoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.CrearFacultadInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.primaryports.interactor.CrearFacultadInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.usecase.CrearFacultadUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.crearfacultad.usecase.impl.CrearFacultadUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.EjecutarCierreMasivoInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.primaryports.interactor.EjecutarCierreMasivoInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.EjecutarCierreMasivoUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.ejecutarcierremasivo.usecase.impl.EjecutarCierreMasivoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CierrePeriodoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.FacultadQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.InstitucionQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ParametroQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AdminWiringConfiguration {

    @Bean ConsultarDecanosUseCase consultarDecanosUseCase(final DecanoQueryPort port) { return new ConsultarDecanosUseCaseImpl(port); }
    @Bean ConsultarDecanosInputPort consultarDecanosInputPort(final ConsultarDecanosUseCase useCase) { return new ConsultarDecanosInteractor(useCase); }
    @Bean CrearDecanoUseCase crearDecanoUseCase(
            final FacultadQueryPort facultadQueryPort,
            final DecanoCommandPort decanoCommandPort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final PasswordEncoderPort passwordEncoderPort,
            final IdentityProviderPort identityProviderPort
    ) {
        return new CrearDecanoUseCaseImpl(facultadQueryPort, decanoCommandPort,
                usuarioRepositoryPort, passwordEncoderPort, identityProviderPort);
    }
    @Bean CrearDecanoInputPort crearDecanoInputPort(final CrearDecanoUseCase useCase) { return new CrearDecanoInteractor(useCase); }
    @Bean ConsultarParametrosUseCase consultarParametrosUseCase(final ParametroQueryPort port) { return new ConsultarParametrosUseCaseImpl(port); }
    @Bean ConsultarParametrosInputPort consultarParametrosInputPort(final ConsultarParametrosUseCase useCase) { return new ConsultarParametrosInteractor(useCase); }
    @Bean ConsultarInstitucionesUseCase consultarInstitucionesUseCase(final InstitucionQueryPort port) { return new ConsultarInstitucionesUseCaseImpl(port); }
    @Bean ConsultarInstitucionesInputPort consultarInstitucionesInputPort(final ConsultarInstitucionesUseCase useCase) { return new ConsultarInstitucionesInteractor(useCase); }
    @Bean ConsultarFacultadesUseCase consultarFacultadesUseCase(final FacultadQueryPort port) { return new ConsultarFacultadesUseCaseImpl(port); }
    @Bean ConsultarFacultadesInputPort consultarFacultadesInputPort(final ConsultarFacultadesUseCase useCase) { return new ConsultarFacultadesInteractor(useCase); }
    @Bean CrearFacultadUseCase crearFacultadUseCase(final FacultadCommandPort port) { return new CrearFacultadUseCaseImpl(port); }
    @Bean CrearFacultadInputPort crearFacultadInputPort(final CrearFacultadUseCase useCase) { return new CrearFacultadInteractor(useCase); }
    @Bean ConsultarAreasUseCase consultarAreasUseCase(final AreaQueryPort port) { return new ConsultarAreasUseCaseImpl(port); }
    @Bean ConsultarAreasInputPort consultarAreasInputPort(final ConsultarAreasUseCase useCase) { return new ConsultarAreasInteractor(useCase); }
    @Bean CrearAreaUseCase crearAreaUseCase(final AreaCommandPort port) { return new CrearAreaUseCaseImpl(port); }
    @Bean CrearAreaInputPort crearAreaInputPort(final CrearAreaUseCase useCase) { return new CrearAreaInteractor(useCase); }
    @Bean ActualizarAreaUseCase actualizarAreaUseCase(final AreaCommandPort port) { return new ActualizarAreaUseCaseImpl(port); }
    @Bean ActualizarAreaInputPort actualizarAreaInputPort(final ActualizarAreaUseCase useCase) { return new ActualizarAreaInteractor(useCase); }
    @Bean EjecutarCierreMasivoUseCase ejecutarCierreMasivoUseCase(final PeriodoAcademicoQueryPort periodoPort, final CierrePeriodoCommandPort cierrePort) { return new EjecutarCierreMasivoUseCaseImpl(periodoPort, cierrePort); }
    @Bean EjecutarCierreMasivoInputPort ejecutarCierreMasivoInputPort(final EjecutarCierreMasivoUseCase useCase) { return new EjecutarCierreMasivoInteractor(useCase); }
}
