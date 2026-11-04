package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.ConsultarTiposIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.interactor.ConsultarTiposIdentificacionInteractor;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.ConsultarTiposIdentificacionUseCase;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.impl.ConsultarTiposIdentificacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class TipoIdentificacionWiringConfiguration {

    @Bean
    public ConsultarTiposIdentificacionUseCase consultarTiposIdentificacionUseCase(
            final TipoIdentificacionRepositoryPort tipoIdentificacionRepositoryPort
    ) {
        return new ConsultarTiposIdentificacionUseCaseImpl(tipoIdentificacionRepositoryPort);
    }

    @Bean
    public ConsultarTiposIdentificacionInputPort consultarTiposIdentificacionInputPort(
            final ConsultarTiposIdentificacionUseCase consultarTiposIdentificacionUseCase
    ) {
        return new ConsultarTiposIdentificacionInteractor(consultarTiposIdentificacionUseCase);
    }
}
