package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.ConsultarCoordinadoresInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.interactor.ConsultarCoordinadoresInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.ConsultarCoordinadoresUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.impl.ConsultarCoordinadoresUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.CrearCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.interactor.CrearCoordinadorInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.CrearCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.impl.CrearCoordinadorUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DecanoWiringConfiguration {

    @Bean ConsultarCoordinadoresUseCase consultarCoordinadoresUseCase(final InstitutionalScopePort scopePort, final CoordinadorQueryPort queryPort) { return new ConsultarCoordinadoresUseCaseImpl(scopePort, queryPort); }
    @Bean ConsultarCoordinadoresInputPort consultarCoordinadoresInputPort(final ConsultarCoordinadoresUseCase useCase) { return new ConsultarCoordinadoresInteractor(useCase); }
    @Bean CrearCoordinadorUseCase crearCoordinadorUseCase(
            final CoordinadorCommandPort commandPort,
            final InstitutionalScopePort scopePort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final PasswordEncoderPort passwordEncoderPort,
            final IdentityProviderPort identityProviderPort
    ) {
        return new CrearCoordinadorUseCaseImpl(commandPort, scopePort, usuarioRepositoryPort, passwordEncoderPort, identityProviderPort);
    }
    @Bean CrearCoordinadorInputPort crearCoordinadorInputPort(final CrearCoordinadorUseCase useCase) { return new CrearCoordinadorInteractor(useCase); }
}
