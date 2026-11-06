package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.identidad.sincronizarcuenta.SincronizarCuentaInstitucionalService;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.primaryports.ActualizarDocenteProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.primaryports.interactor.ActualizarDocenteProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.usecase.ActualizarDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.actualizardocenteprograma.usecase.impl.ActualizarDocenteProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.primaryports.CambiarEstadoDocenteProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.primaryports.interactor.CambiarEstadoDocenteProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.usecase.CambiarEstadoDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.cambiarestadodocenteprograma.usecase.impl.CambiarEstadoDocenteProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.primaryports.ConsultarCandidatosDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.primaryports.interactor.ConsultarCandidatosDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.usecase.ConsultarCandidatosDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.usecase.impl.ConsultarCandidatosDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.ConsultarDocentesProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.interactor.ConsultarDocentesProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.ConsultarDocentesProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.impl.ConsultarDocentesProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.ConsultarFichaDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.interactor.ConsultarFichaDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.ConsultarFichaDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.impl.ConsultarFichaDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.ConsultarResumenDocentesInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.interactor.ConsultarResumenDocentesInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.ConsultarResumenDocentesUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.impl.ConsultarResumenDocentesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.VincularDocenteProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.primaryports.interactor.VincularDocenteProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.VincularDocenteProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.vinculardocenteprograma.usecase.impl.VincularDocenteProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HistorialGruposDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ResumenDocentesProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Casos de uso POJO de docentes del programa (HU062/HU063/HU089/HU091; ficha HU094 y consolidado HU095). Archivo propio para no tocar wirings compartidos. */
@Configuration(proxyBeanMethods = false)
public class DocenteProgramaWiringConfiguration {

    @Bean ConsultarDocentesProgramaUseCase consultarDocentesProgramaUseCase(
            final InstitutionalScopePort scopePort, final DocenteProgramaQueryPort queryPort) {
        return new ConsultarDocentesProgramaUseCaseImpl(scopePort, queryPort);
    }

    @Bean ConsultarDocentesProgramaInputPort consultarDocentesProgramaInputPort(final ConsultarDocentesProgramaUseCase useCase) {
        return new ConsultarDocentesProgramaInteractor(useCase);
    }

    @Bean ConsultarFichaDocenteUseCase consultarFichaDocenteUseCase(
            final InstitutionalScopePort scopePort,
            final DocenteProgramaQueryPort queryPort,
            final HistorialGruposDocenteQueryPort historialQueryPort) {
        return new ConsultarFichaDocenteUseCaseImpl(scopePort, queryPort, historialQueryPort);
    }

    @Bean ConsultarFichaDocenteInputPort consultarFichaDocenteInputPort(final ConsultarFichaDocenteUseCase useCase) {
        return new ConsultarFichaDocenteInteractor(useCase);
    }

    @Bean ConsultarResumenDocentesUseCase consultarResumenDocentesUseCase(
            final InstitutionalScopePort scopePort, final ResumenDocentesProgramaQueryPort queryPort) {
        return new ConsultarResumenDocentesUseCaseImpl(scopePort, queryPort);
    }

    @Bean ConsultarResumenDocentesInputPort consultarResumenDocentesInputPort(final ConsultarResumenDocentesUseCase useCase) {
        return new ConsultarResumenDocentesInteractor(useCase);
    }

    @Bean ConsultarCandidatosDocenteUseCase consultarCandidatosDocenteUseCase(
            final InstitutionalScopePort scopePort, final DocenteProgramaQueryPort queryPort) {
        return new ConsultarCandidatosDocenteUseCaseImpl(scopePort, queryPort);
    }

    @Bean ConsultarCandidatosDocenteInputPort consultarCandidatosDocenteInputPort(final ConsultarCandidatosDocenteUseCase useCase) {
        return new ConsultarCandidatosDocenteInteractor(useCase);
    }

    @Bean VincularDocenteProgramaUseCase vincularDocenteProgramaUseCase(
            final InstitutionalScopePort scopePort,
            final DocenteProgramaCommandPort commandPort,
            final DocenteProgramaQueryPort queryPort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final PasswordEncoderPort passwordEncoderPort,
            final IdentityProviderPort identityProviderPort) {
        return new VincularDocenteProgramaUseCaseImpl(scopePort, commandPort, queryPort, usuarioRepositoryPort,
                passwordEncoderPort, identityProviderPort);
    }

    @Bean VincularDocenteProgramaInputPort vincularDocenteProgramaInputPort(final VincularDocenteProgramaUseCase useCase) {
        return new VincularDocenteProgramaInteractor(useCase);
    }

    @Bean ActualizarDocenteProgramaUseCase actualizarDocenteProgramaUseCase(
            final InstitutionalScopePort scopePort,
            final DocenteProgramaCommandPort commandPort,
            final DocenteProgramaQueryPort queryPort,
            final SincronizarCuentaInstitucionalService sincronizacion) {
        return new ActualizarDocenteProgramaUseCaseImpl(scopePort, commandPort, queryPort, sincronizacion);
    }

    @Bean ActualizarDocenteProgramaInputPort actualizarDocenteProgramaInputPort(final ActualizarDocenteProgramaUseCase useCase) {
        return new ActualizarDocenteProgramaInteractor(useCase);
    }

    @Bean CambiarEstadoDocenteProgramaUseCase cambiarEstadoDocenteProgramaUseCase(
            final InstitutionalScopePort scopePort,
            final DocenteProgramaCommandPort commandPort,
            final DocenteProgramaQueryPort queryPort) {
        return new CambiarEstadoDocenteProgramaUseCaseImpl(scopePort, commandPort, queryPort);
    }

    @Bean CambiarEstadoDocenteProgramaInputPort cambiarEstadoDocenteProgramaInputPort(final CambiarEstadoDocenteProgramaUseCase useCase) {
        return new CambiarEstadoDocenteProgramaInteractor(useCase);
    }
}
