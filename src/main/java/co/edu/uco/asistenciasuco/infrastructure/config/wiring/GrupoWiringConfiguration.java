package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.ActualizarGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.interactor.ActualizarGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.ActualizarGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.impl.ActualizarGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.ConsultarGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.interactor.ConsultarGruposInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.ConsultarGruposUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.impl.ConsultarGruposUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.ConsultarEstudiantesGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.interactor.ConsultarEstudiantesGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.ConsultarEstudiantesGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.impl.ConsultarEstudiantesGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.CrearGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.interactor.CrearGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.CrearGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.impl.CrearGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.RegistrarEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.interactor.RegistrarEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.RegistrarEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.impl.RegistrarEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GrupoWiringConfiguration {

    @Bean
    public CrearGrupoUseCase crearGrupoUseCase(final GrupoRepositoryPort grupoRepositoryPort) {
        return new CrearGrupoUseCaseImpl(grupoRepositoryPort);
    }

    @Bean
    public CrearGrupoInputPort crearGrupoInputPort(final CrearGrupoUseCase crearGrupoUseCase) {
        return new CrearGrupoInteractor(crearGrupoUseCase);
    }

    @Bean
    public ActualizarGrupoUseCase actualizarGrupoUseCase(final GrupoRepositoryPort grupoRepositoryPort) {
        return new ActualizarGrupoUseCaseImpl(grupoRepositoryPort);
    }

    @Bean
    public ActualizarGrupoInputPort actualizarGrupoInputPort(final ActualizarGrupoUseCase actualizarGrupoUseCase) {
        return new ActualizarGrupoInteractor(actualizarGrupoUseCase);
    }

    @Bean
    public RegistrarEstudianteUseCase registrarEstudianteUseCase(
            final GrupoRepositoryPort grupoRepositoryPort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final PasswordEncoderPort passwordEncoderPort,
            final IdentityProviderPort identityProviderPort
    ) {
        return new RegistrarEstudianteUseCaseImpl(
                grupoRepositoryPort, usuarioRepositoryPort, passwordEncoderPort, identityProviderPort
        );
    }

    @Bean
    public RegistrarEstudianteInputPort registrarEstudianteInputPort(
            final RegistrarEstudianteUseCase registrarEstudianteUseCase,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final EstudianteRepositoryPort estudianteRepositoryPort
    ) {
        return new RegistrarEstudianteInteractor(
                registrarEstudianteUseCase, usuarioRepositoryPort, estudianteRepositoryPort
        );
    }

    @Bean
    public ConsultarGruposUseCase consultarGruposUseCase(final GrupoRepositoryPort grupoRepositoryPort) {
        return new ConsultarGruposUseCaseImpl(grupoRepositoryPort);
    }

    @Bean
    public ConsultarGruposInputPort consultarGruposInputPort(final ConsultarGruposUseCase consultarGruposUseCase) {
        return new ConsultarGruposInteractor(consultarGruposUseCase);
    }

    @Bean
    public ConsultarEstudiantesGrupoUseCase consultarEstudiantesGrupoUseCase(
            final GrupoRepositoryPort grupoRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        return new ConsultarEstudiantesGrupoUseCaseImpl(grupoRepositoryPort, institutionalScopePort);
    }

    @Bean
    public ConsultarEstudiantesGrupoInputPort consultarEstudiantesGrupoInputPort(
            final ConsultarEstudiantesGrupoUseCase consultarEstudiantesGrupoUseCase
    ) {
        return new ConsultarEstudiantesGrupoInteractor(consultarEstudiantesGrupoUseCase);
    }
}
