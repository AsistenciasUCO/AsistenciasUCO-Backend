package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.sesioncancelacion.cancelarsesion.primaryports.CancelarSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesioncancelacion.cancelarsesion.usecase.impl.CancelarSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition Root de la cancelacion de sesiones por el docente titular (CULM-001 B.1, HU047). */
@Configuration(proxyBeanMethods = false)
public class SesionCancelacionWiringConfiguration {

    @Bean
    public CancelarSesionInputPort cancelarSesionInputPort(
            final InstitutionalScopePort institutionalScopePort,
            final SesionProcedenciaQueryPort sesionProcedenciaQueryPort,
            final SesionCancelacionRepositoryPort sesionCancelacionRepositoryPort,
            final NotificacionPort notificacionPort,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        return new CancelarSesionUseCaseImpl(institutionalScopePort, sesionProcedenciaQueryPort,
                sesionCancelacionRepositoryPort, notificacionPort, realtimePublisherPort);
    }
}
