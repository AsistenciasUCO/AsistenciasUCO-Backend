package co.edu.uco.asistenciasuco.infrastructure.config.adapters.catalog.composite;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.composite.ParametroConValorPorDefectoCatalogAdapter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * CULM-001 A.2-6 (DEC-018): el {@link ParameterCatalogPort} que reciben los consumidores es el decorador con valores por
 * defecto. El proveedor real (Azure App Configuration o {@code sqlserver}, segun
 * {@code app.adapters.parameter-catalog.provider}) se registra con el calificador {@value #DELEGADO}: el decorador lo recibe
 * por ese calificador, sin inyectarse a si mismo ni crear un ciclo (N9).
 */
@Configuration(proxyBeanMethods = false)
public class ParametroConValorPorDefectoConfiguration {

    public static final String DELEGADO = "parameterCatalogDelegado";

    @Bean
    @Primary
    public ParameterCatalogPort parameterCatalogPortConValoresPorDefecto(
            @Qualifier(DELEGADO) final ParameterCatalogPort delegado
    ) {
        return new ParametroConValorPorDefectoCatalogAdapter(delegado);
    }
}
