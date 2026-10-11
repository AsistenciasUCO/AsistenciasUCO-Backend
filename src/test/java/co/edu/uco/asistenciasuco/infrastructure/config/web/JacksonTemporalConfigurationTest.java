package co.edu.uco.asistenciasuco.infrastructure.config.web;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JacksonTemporalConfigurationTest {

    @Test
    void serializa_local_date_time_v1_con_el_wire_iso_local_del_contrato() throws Exception {
        final JsonMapper.Builder builder = JsonMapper.builder().findAndAddModules();

        new JacksonTemporalConfiguration().localDateTimeWireFormat().customize(builder);

        assertEquals("\"2026-08-17T13:00:00\"", builder.build()
                .writeValueAsString(LocalDateTime.of(2026, 8, 17, 13, 0)));
    }
}
