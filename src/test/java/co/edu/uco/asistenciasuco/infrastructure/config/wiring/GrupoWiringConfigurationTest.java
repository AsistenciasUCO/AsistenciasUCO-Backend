package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.festivos.asegurarfestivos.primaryports.AsegurarFestivosPeriodoInputPort;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.primaryports.interactor.ActualizarGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.actualizargrupo.usecase.impl.ActualizarGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.primaryports.interactor.ConsultarGruposInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.impl.ConsultarGruposUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.primaryports.interactor.ConsultarEstudiantesGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.consultarestudiantesgrupo.usecase.impl.ConsultarEstudiantesGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.primaryports.interactor.CrearGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.creargrupo.usecase.impl.CrearGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.primaryports.interactor.RegistrarEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.registrarestudianteengrupo.usecase.impl.RegistrarEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class GrupoWiringConfigurationTest {

    private final GrupoWiringConfiguration config = new GrupoWiringConfiguration();
    private final GrupoRepositoryPort grupoRepositoryPort = mock(GrupoRepositoryPort.class);

    @Test
    void todos_los_beans_se_construyen_con_el_adapter_esperado() {
        final var crearGrupoUseCase = config.crearGrupoUseCase(grupoRepositoryPort, mock(AsegurarFestivosPeriodoInputPort.class));
        assertInstanceOf(CrearGrupoUseCaseImpl.class, crearGrupoUseCase);
        assertInstanceOf(CrearGrupoInteractor.class, config.crearGrupoInputPort(crearGrupoUseCase));

        final var actualizarGrupoUseCase = config.actualizarGrupoUseCase(grupoRepositoryPort, mock(AsegurarFestivosPeriodoInputPort.class));
        assertInstanceOf(ActualizarGrupoUseCaseImpl.class, actualizarGrupoUseCase);
        assertInstanceOf(ActualizarGrupoInteractor.class, config.actualizarGrupoInputPort(actualizarGrupoUseCase));

        final var registrarEstudianteUseCase = config.registrarEstudianteUseCase(
                grupoRepositoryPort, mock(UsuarioRepositoryPort.class), mock(PasswordEncoderPort.class), mock(IdentityProviderPort.class),
                config.correoInstitucionalPolicy(
                        mock(co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort.class),
                        mock(co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort.class)),
                mock(co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort.class));
        assertInstanceOf(RegistrarEstudianteUseCaseImpl.class, registrarEstudianteUseCase);
        assertInstanceOf(RegistrarEstudianteInteractor.class, config.registrarEstudianteInputPort(
                registrarEstudianteUseCase, mock(co.edu.uco.asistenciasuco.application.secondaryports.repository.EstudianteRepositoryPort.class)));

        final var consultarGruposUseCase = config.consultarGruposUseCase(grupoRepositoryPort);
        assertInstanceOf(ConsultarGruposUseCaseImpl.class, consultarGruposUseCase);
        assertInstanceOf(ConsultarGruposInteractor.class, config.consultarGruposInputPort(consultarGruposUseCase));

        final var consultarEstudiantesGrupoUseCase = config.consultarEstudiantesGrupoUseCase(
                grupoRepositoryPort, mock(InstitutionalScopePort.class));
        assertInstanceOf(ConsultarEstudiantesGrupoUseCaseImpl.class, consultarEstudiantesGrupoUseCase);
        assertInstanceOf(ConsultarEstudiantesGrupoInteractor.class,
                config.consultarEstudiantesGrupoInputPort(consultarEstudiantesGrupoUseCase));
    }
}
