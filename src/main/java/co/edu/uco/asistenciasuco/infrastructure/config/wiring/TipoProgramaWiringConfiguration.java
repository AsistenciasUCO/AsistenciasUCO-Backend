package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.ConsultarTiposProgramaInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.interactor.ConsultarTiposProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.usecase.ConsultarTiposProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.usecase.impl.ConsultarTiposProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition Root de los casos de uso del catalogo de tipos de programa (HU023 / HU154). */
@Configuration(proxyBeanMethods = false)
public class TipoProgramaWiringConfiguration {

    @Bean ConsultarTiposProgramaUseCase consultarTiposProgramaUseCase(final TipoProgramaQueryPort port) { return new ConsultarTiposProgramaUseCaseImpl(port); }
    @Bean ConsultarTiposProgramaInputPort consultarTiposProgramaInputPort(final ConsultarTiposProgramaUseCase useCase) { return new ConsultarTiposProgramaInteractor(useCase); }
}
