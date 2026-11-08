package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.AdmitirEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.primaryports.interactor.AdmitirEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.AdmitirEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.impl.AdmitirEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports.ConsultarProgramasInstitucionInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.primaryports.interactor.ConsultarProgramasInstitucionInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.usecase.ConsultarProgramasInstitucionUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarprogramasinstitucion.usecase.impl.ConsultarProgramasInstitucionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.identidad.asegurarcuenta.AsegurarCuentaIdentidadService;
import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AdmisionEstudianteCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ProgramaInstitucionQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.UsuarioRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.PasswordEncoderPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition Root de la admision de estudiantes por el administrador (CULM-001 D.4, HU156). */
@Configuration(proxyBeanMethods = false)
public class AdmisionEstudianteWiringConfiguration {

    @Bean
    AdmitirEstudianteUseCase admitirEstudianteUseCase(
            final AdmisionEstudianteCommandPort comandoPort,
            final UsuarioRepositoryPort usuarioRepositoryPort,
            final PasswordEncoderPort passwordEncoderPort,
            final CorreoInstitucionalPolicy correoInstitucionalPolicy,
            final AsegurarCuentaIdentidadService asegurarCuentaIdentidadService,
            final InstitutionalScopePort scopePort,
            final NotificacionPort notificacionPort
    ) {
        return new AdmitirEstudianteUseCaseImpl(comandoPort, usuarioRepositoryPort, passwordEncoderPort, correoInstitucionalPolicy,
                asegurarCuentaIdentidadService, scopePort, notificacionPort);
    }

    @Bean
    AdmitirEstudianteInputPort admitirEstudianteInputPort(final AdmitirEstudianteUseCase useCase) {
        return new AdmitirEstudianteInteractor(useCase);
    }

    @Bean
    ConsultarProgramasInstitucionUseCase consultarProgramasInstitucionUseCase(final ProgramaInstitucionQueryPort queryPort) {
        return new ConsultarProgramasInstitucionUseCaseImpl(queryPort);
    }

    @Bean
    ConsultarProgramasInstitucionInputPort consultarProgramasInstitucionInputPort(final ConsultarProgramasInstitucionUseCase useCase) {
        return new ConsultarProgramasInstitucionInteractor(useCase);
    }
}
