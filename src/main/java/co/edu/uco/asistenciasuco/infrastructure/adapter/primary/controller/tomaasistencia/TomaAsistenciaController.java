package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia;

import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartoma.primaryports.CerrarTomaAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarasistenciastoma.primaryports.ConsultarAsistenciasTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarfaltantestoma.primaryports.ConsultarFaltantesTomaInputPort;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.registrarasistenciastoma.primaryports.RegistrarAsistenciasTomaInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia.request.TomaAsistenciaRequests;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia.response.TomaAsistenciaHttpResponses;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia.response.TomaAsistenciaHttpResponses.AsistenciaTomaResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia.response.TomaAsistenciaHttpResponses.CierreTomaResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia.response.TomaAsistenciaHttpResponses.FaltanteTomaResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia.response.TomaAsistenciaHttpResponses.LoteTomaResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 B.2 (contrato 3.2.5): cierre de la toma (con faltantes como ausentes segun el modo), lote de asistencia por toma
 * y consultas paginadas de asistencias y faltantes. Solo el docente titular de la sesion de la toma.
 */
@RestController
@RequestMapping("/api/v1/tomas/{tomaId}")
public final class TomaAsistenciaController {

    private final CerrarTomaAsistenciaInputPort cerrarToma;
    private final RegistrarAsistenciasTomaInputPort registrarAsistencias;
    private final ConsultarAsistenciasTomaInputPort consultarAsistencias;
    private final ConsultarFaltantesTomaInputPort consultarFaltantes;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public TomaAsistenciaController(
            final CerrarTomaAsistenciaInputPort cerrarToma,
            final RegistrarAsistenciasTomaInputPort registrarAsistencias,
            final ConsultarAsistenciasTomaInputPort consultarAsistencias,
            final ConsultarFaltantesTomaInputPort consultarFaltantes,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.cerrarToma = Objects.requireNonNull(cerrarToma, "CerrarTomaAsistenciaInputPort es obligatorio.");
        this.registrarAsistencias = Objects.requireNonNull(registrarAsistencias, "RegistrarAsistenciasTomaInputPort es obligatorio.");
        this.consultarAsistencias = Objects.requireNonNull(consultarAsistencias, "ConsultarAsistenciasTomaInputPort es obligatorio.");
        this.consultarFaltantes = Objects.requireNonNull(consultarFaltantes, "ConsultarFaltantesTomaInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping("/cierre")
    @AuditableOperation(action = "CERRAR_TOMA_ASISTENCIA", resourceType = "TOMA_ASISTENCIA", resourceIdPathVariable = "tomaId")
    public ResponseEntity<ApiDataResponse<CierreTomaResponse>> cerrar(
            @PathVariable final UUID tomaId,
            @RequestBody(required = false) final TomaAsistenciaRequests.CerrarToma request
    ) {
        return ResponseEntity.ok(new ApiDataResponse<>(true, CierreTomaResponse.from(cerrarToma.execute(new CerrarTomaAsistenciaInputPort.Comando(
                tomaId, request == null ? null : request.marcarAusentes(), authenticatedUserResolver.requireAuthenticatedUserId())))));
    }

    @PostMapping("/asistencias")
    @AuditableOperation(action = "REGISTRAR_ASISTENCIAS_TOMA", resourceType = "TOMA_ASISTENCIA", resourceIdPathVariable = "tomaId")
    public ResponseEntity<ApiDataResponse<LoteTomaResponse>> registrar(
            @PathVariable final UUID tomaId,
            @RequestBody(required = false) final TomaAsistenciaRequests.LoteToma request
    ) {
        final List<RegistrarAsistenciasTomaInputPort.Registro> registros = request == null || request.registros() == null ? null
                : request.registros().stream()
                .map(r -> r == null ? null : new RegistrarAsistenciasTomaInputPort.Registro(r.estudianteId(), r.estado()))
                .toList();
        return ResponseEntity.ok(new ApiDataResponse<>(true, LoteTomaResponse.from(registrarAsistencias.execute(
                new RegistrarAsistenciasTomaInputPort.Comando(tomaId, registros, authenticatedUserResolver.requireAuthenticatedUserId())))));
    }

    @GetMapping("/asistencias")
    public ResponseEntity<ApiPageResponse<AsistenciaTomaResponse>> asistencias(
            @PathVariable final UUID tomaId,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        return ResponseEntity.ok(TomaAsistenciaHttpResponses.pagina(consultarAsistencias.execute(new ConsultarAsistenciasTomaInputPort.Consulta(
                tomaId, pagina, tamano, authenticatedUserResolver.requireAuthenticatedUserId())), AsistenciaTomaResponse::from));
    }

    @GetMapping("/faltantes")
    public ResponseEntity<ApiPageResponse<FaltanteTomaResponse>> faltantes(
            @PathVariable final UUID tomaId,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        return ResponseEntity.ok(TomaAsistenciaHttpResponses.pagina(consultarFaltantes.execute(new ConsultarFaltantesTomaInputPort.Consulta(
                tomaId, pagina, tamano, authenticatedUserResolver.requireAuthenticatedUserId())), FaltanteTomaResponse::from));
    }
}
