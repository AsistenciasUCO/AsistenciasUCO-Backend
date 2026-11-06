package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.primaryports.interactor.ActualizarDocenteProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.usecase.impl.ActualizarDocenteProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.primaryports.interactor.CambiarEstadoDocenteProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.usecase.impl.CambiarEstadoDocenteProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.primaryports.interactor.ConsultarCandidatosDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.usecase.impl.ConsultarCandidatosDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.interactor.ConsultarDocentesProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.impl.ConsultarDocentesProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.interactor.VincularDocenteProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.impl.VincularDocenteProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.primaryports.interactor.ConsultarTiposVinculacionInteractor;
import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.usecase.impl.ConsultarTiposVinculacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoVinculacionQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

/** T-WIR-01: los casos de uso se cablean como POJO en configuracion (sin @Service en application). */
class DocenteProgramaWiringConfigurationTest {

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final DocenteProgramaQueryPort query = mock(DocenteProgramaQueryPort.class);

    @Test
    void docentes_programa_beans_se_construyen_con_los_adapters_esperados() {
        final DocenteProgramaWiringConfiguration config = new DocenteProgramaWiringConfiguration();

        final var consultar = config.consultarDocentesProgramaUseCase(scope, query);
        assertInstanceOf(ConsultarDocentesProgramaUseCaseImpl.class, consultar);
        assertInstanceOf(ConsultarDocentesProgramaInteractor.class, config.consultarDocentesProgramaInputPort(consultar));

        final var candidatos = config.consultarCandidatosDocenteUseCase(scope, query);
        assertInstanceOf(ConsultarCandidatosDocenteUseCaseImpl.class, candidatos);
        assertInstanceOf(ConsultarCandidatosDocenteInteractor.class, config.consultarCandidatosDocenteInputPort(candidatos));

        final var vincular = config.vincularDocenteProgramaUseCase(scope, mock(DocenteProgramaCommandPort.class), query,
                mock(UsuarioRepositoryPort.class), mock(PasswordEncoderPort.class), mock(IdentityProviderPort.class));
        assertInstanceOf(VincularDocenteProgramaUseCaseImpl.class, vincular);
        assertInstanceOf(VincularDocenteProgramaInteractor.class, config.vincularDocenteProgramaInputPort(vincular));
    }

    @Test
    void actualizar_docente_programa_beans_se_construyen_con_los_adapters_esperados() {
        final DocenteProgramaWiringConfiguration config = new DocenteProgramaWiringConfiguration();

        final var useCase = config.actualizarDocenteProgramaUseCase(scope, mock(DocenteProgramaCommandPort.class), query,
                mock(co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.SincronizarCuentaInstitucionalService.class));
        assertInstanceOf(ActualizarDocenteProgramaUseCaseImpl.class, useCase);
        assertInstanceOf(ActualizarDocenteProgramaInteractor.class, config.actualizarDocenteProgramaInputPort(useCase));
    }

    @Test
    void cambiar_estado_docente_programa_beans_se_construyen_con_los_adapters_esperados() {
        final DocenteProgramaWiringConfiguration config = new DocenteProgramaWiringConfiguration();

        final var useCase = config.cambiarEstadoDocenteProgramaUseCase(scope, mock(DocenteProgramaCommandPort.class), query);
        assertInstanceOf(CambiarEstadoDocenteProgramaUseCaseImpl.class, useCase);
        assertInstanceOf(CambiarEstadoDocenteProgramaInteractor.class, config.cambiarEstadoDocenteProgramaInputPort(useCase));
    }

    @Test
    void tipo_vinculacion_beans_se_construyen_con_los_adapters_esperados() {
        final TipoVinculacionWiringConfiguration config = new TipoVinculacionWiringConfiguration();

        final var useCase = config.consultarTiposVinculacionUseCase(mock(TipoVinculacionQueryPort.class));
        assertInstanceOf(ConsultarTiposVinculacionUseCaseImpl.class, useCase);
        assertInstanceOf(ConsultarTiposVinculacionInteractor.class, config.consultarTiposVinculacionInputPort(useCase));
    }
}
