package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.docente;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarresumenasistencia.primaryports.ConsultarResumenAsistenciaGrupoInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia.response.TomaAsistenciaHttpResponses;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.tomaasistencia.response.TomaAsistenciaHttpResponses.ResumenAsistenciaEstudianteResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 B.2 (F-DOC-004, contrato 3.2.8): sabana del docente titular con los indicadores por horas que calcula la base de
 * datos; el cliente deja de calcular porcentajes.
 */
@RestController
@RequestMapping("/api/v1/docente/grupos/{grupoId}/resumen-asistencia")
public final class DocenteResumenAsistenciaController {

    private final ConsultarResumenAsistenciaGrupoInputPort consultarResumen;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DocenteResumenAsistenciaController(
            final ConsultarResumenAsistenciaGrupoInputPort consultarResumen,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarResumen = Objects.requireNonNull(consultarResumen, "ConsultarResumenAsistenciaGrupoInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<ApiPageResponse<ResumenAsistenciaEstudianteResponse>> consultar(
            @PathVariable final UUID grupoId,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        return ResponseEntity.ok(TomaAsistenciaHttpResponses.pagina(consultarResumen.execute(new ConsultarResumenAsistenciaGrupoInputPort.Consulta(
                grupoId, pagina, tamano, authenticatedUserResolver.requireAuthenticatedUserId())), ResumenAsistenciaEstudianteResponse::from));
    }
}
