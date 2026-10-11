package co.edu.uco.asistenciasuco.infrastructure.config.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.mock.http.MockHttpOutputMessage;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = {JacksonAutoConfiguration.class, JacksonTemporalConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class JacksonTemporalConfigurationTest {

    @Autowired
    private JsonMapper springJsonMapper;

    @Test
    void serializa_local_date_time_v1_con_el_wire_iso_local_del_contrato() throws Exception {
        final JsonMapper.Builder builder = JsonMapper.builder().findAndAddModules();

        new JacksonTemporalConfiguration().localDateTimeWireFormat().customize(builder);

        assertEquals("\"2026-08-17T13:00:00\"", builder.build()
                .writeValueAsString(LocalDateTime.of(2026, 8, 17, 13, 0)));
    }

    @Test
    void mapper_real_de_spring_aplica_el_wire_iso_local_v1() throws Exception {
        assertEquals("\"2026-08-17T13:00:00\"",
                springJsonMapper.writeValueAsString(LocalDateTime.of(2026, 8, 17, 13, 0)));
    }

    @Test
    void customizer_oficial_de_boot_reemplaza_el_converter_json_del_servidor() throws Exception {
        final JacksonTemporalConfiguration configuration = new JacksonTemporalConfiguration();
        configuration.setSpringJsonMapper(springJsonMapper);
        final HttpMessageConverters.ServerBuilder builder = HttpMessageConverters.forServer().registerDefaults();
        configuration.localDateTimeWireConverter().customize(builder);

        final JacksonJsonHttpMessageConverter converter = StreamSupport.stream(builder.build().spliterator(), false)
                .filter(JacksonJsonHttpMessageConverter.class::isInstance)
                .map(JacksonJsonHttpMessageConverter.class::cast)
                .findFirst()
                .orElseThrow();
        final MockHttpOutputMessage output = new MockHttpOutputMessage();
        converter.write(LocalDateTime.of(2026, 8, 17, 13, 0), MediaType.APPLICATION_JSON, output);

        assertTrue(output.getBodyAsString().contains("\"2026-08-17T13:00:00\""), output.getBodyAsString());
    }
}
