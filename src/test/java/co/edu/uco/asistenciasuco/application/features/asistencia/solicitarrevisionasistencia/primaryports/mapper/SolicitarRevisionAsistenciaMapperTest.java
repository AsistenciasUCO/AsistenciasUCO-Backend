package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.dto.SolicitarRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.domain.SolicitarRevisionAsistenciaDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolicitarRevisionAsistenciaMapperTest {

    @Test
    void toDomain_con_dto_nulo_lanza_excepcion() {
        assertThrows(CrosscuttingException.class, () -> SolicitarRevisionAsistenciaMapper.toDomain(null));
    }

    @Test
    void toDomain_con_dto_valido_mapea_campos() {
        final UUID sesion = UUID.randomUUID();
        final UUID usuario = UUID.randomUUID();
        final UUID soporte = UUID.randomUUID();
        final SolicitarRevisionAsistenciaDTO dto = new SolicitarRevisionAsistenciaDTO(
                sesion, "SALUD", "Justificacion valida", soporte, usuario);

        final SolicitarRevisionAsistenciaDomain domain = SolicitarRevisionAsistenciaMapper.toDomain(dto);

        assertEquals(sesion, domain.getSesion());
        assertEquals("SALUD", domain.getCategoria());
        assertEquals(usuario, domain.getUsuario());
        assertEquals(soporte, domain.getSoporteArchivoId());
    }
}
