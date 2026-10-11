package co.edu.uco.asistenciasuco.infrastructure.config.web;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Configuracion del wire legacy de sesiones: DATETIME2(7) se expone como fecha-hora local ISO,
 * sin interpretar zona ni aplicar el formato localizado por defecto del mapper.
 */
@Configuration
public class JacksonTemporalConfiguration {

    private JsonMapper springJsonMapper;

    @Autowired
    void setSpringJsonMapper(final JsonMapper springJsonMapper) {
        this.springJsonMapper = springJsonMapper;
    }

    @Bean
    ServerHttpMessageConvertersCustomizer localDateTimeWireConverter() {
        return builder -> {
            if (springJsonMapper != null) {
                builder.withJsonConverter(wireConverter(springJsonMapper));
            }
        };
    }

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

    JacksonJsonHttpMessageConverter wireConverter(final JsonMapper jsonMapper) {
        final JsonMapper.Builder builder = jsonMapper.rebuild();
        localDateTimeWireFormat().customize(builder);
        return new JacksonJsonHttpMessageConverter(builder.build());
    }
}
