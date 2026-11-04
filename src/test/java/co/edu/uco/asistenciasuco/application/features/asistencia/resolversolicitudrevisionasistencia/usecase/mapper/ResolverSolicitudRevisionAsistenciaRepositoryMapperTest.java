package co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.mapper;

import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.domain.ResolverSolicitudRevisionAsistenciaDomain;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResolverSolicitudRevisionAsistenciaRepositoryMapperTest {

    @Test
    void toRepositoryDTO_rechaza_dominio_nulo() {
        assertThrows(CrosscuttingException.class,
                () -> ResolverSolicitudRevisionAsistenciaRepositoryMapper.toRepositoryDTO(null, UUID.randomUUID()));
    }

    @Test
    void toRepositoryDTO_usa_docenteId_resuelto_y_no_lo_confunde_con_usuarioEjecutor() {
        final UUID usuario = UUID.randomUUID();
        final UUID docenteId = UUID.randomUUID();
        final UUID solicitud = UUID.randomUUID();
        final ResolverSolicitudRevisionAsistenciaDomain domain =
                new ResolverSolicitudRevisionAsistenciaDomain(solicitud, "APROBADA", "Aceptada", usuario);

        final var dto = ResolverSolicitudRevisionAsistenciaRepositoryMapper.toRepositoryDTO(domain, docenteId);

        assertEquals(docenteId, dto.docente());
        assertEquals(usuario, dto.usuarioEjecutor());
        assertNotEquals(dto.docente(), dto.usuarioEjecutor());
    }
}
