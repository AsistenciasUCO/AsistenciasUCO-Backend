package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.sesion.v2;

import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.SesionV2ConsultadaDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.primaryports.ConsultarSesionesPorGrupoV2InputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionesporgrupov2.primaryports.dto.ConsultarSesionesPorGrupoV2DTO;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.primaryports.ConsultarSesionV2InputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesionv2.primaryports.dto.ConsultarSesionV2DTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.HttpUtcInstantCodec;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lecturas v2 de sesiones (UTC-D07): {@code GET /api/v2/sesiones/{sesionId}} y
 * {@code GET /api/v2/sesiones/grupo/{grupoId}}.
 *
 * <p>Separado de {@link SesionV2Controller} (comandos) porque depende de puertos de consulta propios
 * de v2; ambos comparten la misma puerta de activacion. Una sesion CONFIRMADA serializa sus horas con
 * {@code Z} desde el UTC almacenado; una INDETERMINADA las serializa en null y nunca se oculta.</p>
 */
@RestController
@RequestMapping("/api/v2")
public final class SesionV2ConsultaController {

    private final ConsultarSesionV2InputPort consultarSesionV2InputPort;
    private final ConsultarSesionesPorGrupoV2InputPort consultarSesionesPorGrupoV2InputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public SesionV2ConsultaController(
            final ConsultarSesionV2InputPort consultarSesionV2InputPort,
            final ConsultarSesionesPorGrupoV2InputPort consultarSesionesPorGrupoV2InputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarSesionV2InputPort = Objects.requireNonNull(consultarSesionV2InputPort, "ConsultarSesionV2InputPort es obligatorio.");
        this.consultarSesionesPorGrupoV2InputPort = Objects.requireNonNull(
                consultarSesionesPorGrupoV2InputPort, "ConsultarSesionesPorGrupoV2InputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping("/sesiones/{sesionId}")
    public ResponseEntity<ApiDataResponse<SesionV2Response>> consultarSesion(@PathVariable final UUID sesionId) {
        final SesionV2ConsultadaDTO sesion = consultarSesionV2InputPort.execute(
                new ConsultarSesionV2DTO(sesionId, authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, toResponse(sesion)));
    }

    @GetMapping("/sesiones/grupo/{grupoId}")
    public ResponseEntity<ApiListResponse<SesionV2Response>> consultarSesionesPorGrupo(@PathVariable final UUID grupoId) {
        final List<SesionV2Response> sesiones = consultarSesionesPorGrupoV2InputPort.execute(
                        new ConsultarSesionesPorGrupoV2DTO(grupoId, authenticatedUserResolver.requireAuthenticatedUserId()))
                .stream()
                .map(SesionV2ConsultaController::toResponse)
                .toList();
        return ResponseEntity.ok(new ApiListResponse<>(true, sesiones, sesiones.size()));
    }

    static SesionV2Response toResponse(final SesionV2ConsultadaDTO dto) {
        return new SesionV2Response(
                dto.sesion(), dto.grupo(), dto.nombre(), dto.numero(), dto.codigo(), dto.numeroSemana(),
                dto.codigoGrupo(), dto.nombreGrupo(),
                instante(dto.fechaHoraInicioUtc()), instante(dto.fechaHoraFinUtc()),
                dto.estadoTemporal(), dto.procedenciaTemporal()
        );
    }

    private static String instante(final LocalDateTime utc) {
        return utc == null ? null : HttpUtcInstantCodec.fromUtcDatabaseDateTime(utc);
    }
}
