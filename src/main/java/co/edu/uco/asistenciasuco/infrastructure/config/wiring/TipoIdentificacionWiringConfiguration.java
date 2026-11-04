package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.ConsultarTiposIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.interactor.ConsultarTiposIdentificacionInteractor;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.ConsultarTiposIdentificacionUseCase;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.impl.ConsultarTiposIdentificacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.primaryports.GuardarTipoIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.primaryports.interactor.GuardarTipoIdentificacionInteractor;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.usecase.GuardarTipoIdentificacionUseCase;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.usecase.impl.GuardarTipoIdentificacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionCommandPort;
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

    /** HU153: alta y edicion del nombre de tipos de documento. */
    @Bean
    public GuardarTipoIdentificacionUseCase guardarTipoIdentificacionUseCase(
            final TipoIdentificacionCommandPort tipoIdentificacionCommandPort
    ) {
        return new GuardarTipoIdentificacionUseCaseImpl(tipoIdentificacionCommandPort);
    }

    @Bean
    public GuardarTipoIdentificacionInputPort guardarTipoIdentificacionInputPort(
            final GuardarTipoIdentificacionUseCase guardarTipoIdentificacionUseCase
    ) {
        return new GuardarTipoIdentificacionInteractor(guardarTipoIdentificacionUseCase);
    }
}
