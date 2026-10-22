package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.ConsultarCoordinadoresInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.primaryports.interactor.ConsultarCoordinadoresInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.ConsultarCoordinadoresUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.impl.ConsultarCoordinadoresUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.ConsultarProgramasInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.primaryports.interactor.ConsultarProgramasInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.ConsultarProgramasUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.impl.ConsultarProgramasUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.primaryports.ConsultarTiposProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.primaryports.interactor.ConsultarTiposProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.usecase.ConsultarTiposProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultartiposprograma.usecase.impl.ConsultarTiposProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.CrearCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.interactor.CrearCoordinadorInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.CrearCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.impl.CrearCoordinadorUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.CrearProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.primaryports.interactor.CrearProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.CrearProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.crearprograma.usecase.impl.CrearProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaQueryPort;
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

    @Bean ConsultarProgramasUseCase consultarProgramasUseCase(final InstitutionalScopePort scopePort, final ProgramaQueryPort queryPort) {
        return new ConsultarProgramasUseCaseImpl(scopePort, queryPort);
    }
    @Bean ConsultarProgramasInputPort consultarProgramasInputPort(final ConsultarProgramasUseCase useCase) {
        return new ConsultarProgramasInteractor(useCase);
    }

    @Bean ConsultarTiposProgramaUseCase consultarTiposProgramaUseCase(final TipoProgramaQueryPort queryPort) {
        return new ConsultarTiposProgramaUseCaseImpl(queryPort);
    }
    @Bean ConsultarTiposProgramaInputPort consultarTiposProgramaInputPort(final ConsultarTiposProgramaUseCase useCase) {
        return new ConsultarTiposProgramaInteractor(useCase);
    }

    @Bean CrearProgramaUseCase crearProgramaUseCase(
            final ProgramaCommandPort commandPort,
            final ProgramaQueryPort queryPort,
            final InstitutionalScopePort scopePort
    ) {
        return new CrearProgramaUseCaseImpl(commandPort, queryPort, scopePort);
    }
    @Bean CrearProgramaInputPort crearProgramaInputPort(final CrearProgramaUseCase useCase) {
        return new CrearProgramaInteractor(useCase);
    }
}
