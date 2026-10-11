package co.edu.uco.asistenciasuco.infrastructure.config.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifica el converter HTTP que la configuracion de Infrastructure instala para el wire v1. */
class JacksonRuntimeWireCompatibilityIT {

    @Test
    void converter_http_preserva_el_wire_iso_local_v1() throws Exception {
        final JsonMapper baseMapper = JsonMapper.builder().findAndAddModules().build();
        final JacksonJsonHttpMessageConverter converter = new JacksonTemporalConfiguration().wireConverter(baseMapper);

        final MockHttpOutputMessage output = new MockHttpOutputMessage();
        converter.write(LocalDateTime.of(2026, 8, 17, 13, 0), MediaType.APPLICATION_JSON, output);

        assertTrue(output.getBodyAsString().contains("\"2026-08-17T13:00:00\""),
                output.getBodyAsString());
    }
}
