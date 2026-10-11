package co.edu.uco.asistenciasuco.infrastructure.config.web;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Configuracion del wire legacy de sesiones: DATETIME2(7) se expone como fecha-hora local ISO,
 * sin interpretar zona ni aplicar el formato localizado por defecto del mapper.
 */
@Configuration
public class JacksonTemporalConfiguration {

    @Bean
    JsonMapperBuilderCustomizer localDateTimeWireFormat() {
        return builder -> builder.addModule(new SimpleModule("uco-local-date-time-wire")
                .addSerializer(LocalDateTime.class, new ValueSerializer<>() {
                    @Override
                    public void serialize(
                            final LocalDateTime value,
                            final JsonGenerator generator,
                            final SerializationContext context
                    ) {
                        generator.writeString(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    }
                }));
    }
}
