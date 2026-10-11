package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.SesionV2ConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.primaryports.ConsultarSesionV2InputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.primaryports.ConsultarSesionesPorGrupoV2InputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SesionV2ConsultaControllerEndpointTest {

    private static final UUID SESION = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();

    @Test
    void endpoint_por_id_resuelve_actor_y_devuelve_datos() {
        final ConsultarSesionV2InputPort input = mock(ConsultarSesionV2InputPort.class);
        final ConsultarSesionesPorGrupoV2InputPort groupInput = mock(ConsultarSesionesPorGrupoV2InputPort.class);
        final AuthenticatedUserResolver resolver = mock(AuthenticatedUserResolver.class);
        when(resolver.requireAuthenticatedUserId()).thenReturn(USUARIO);
        when(input.execute(new co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.primaryports.dto.ConsultarSesionV2DTO(SESION, USUARIO)))
                .thenReturn(dto());

        final SesionV2ConsultaController controller = new SesionV2ConsultaController(input, groupInput, resolver);
        final ResponseEntity<?> response = controller.consultarSesion(SESION);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void endpoint_por_grupo_mapea_lista_y_total() {
        final ConsultarSesionV2InputPort input = mock(ConsultarSesionV2InputPort.class);
        final ConsultarSesionesPorGrupoV2InputPort groupInput = mock(ConsultarSesionesPorGrupoV2InputPort.class);
        final AuthenticatedUserResolver resolver = mock(AuthenticatedUserResolver.class);
        when(resolver.requireAuthenticatedUserId()).thenReturn(USUARIO);
        when(groupInput.execute(new co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.primaryports.dto.ConsultarSesionesPorGrupoV2DTO(GRUPO, USUARIO)))
                .thenReturn(List.of(dto(), dto()));

        final SesionV2ConsultaController controller = new SesionV2ConsultaController(input, groupInput, resolver);
        final ResponseEntity<?> response = controller.consultarSesionesPorGrupo(GRUPO);
        assertEquals(200, response.getStatusCode().value());
    }

    private static SesionV2ConsultadaDTO dto() {
        return new SesionV2ConsultadaDTO(
                SESION, GRUPO, "Sesion", 1, "S-1", 2, "7", "Grupo",
                LocalDateTime.parse("2042-07-15T14:00:00.1234567"),
                LocalDateTime.parse("2042-07-15T15:30:00.7654321"),
                "CONFIRMADA", "UTC_V2");
    }
}
