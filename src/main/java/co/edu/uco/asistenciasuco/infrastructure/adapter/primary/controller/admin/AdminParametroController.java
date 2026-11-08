package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.features.admin.actualizarparametro.primaryports.ActualizarParametroInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcambiosparametro.primaryports.ConsultarCambiosParametroInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports.ConsultarParametrosInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.CambioParametroResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.ParametroActualizadoResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.ParametroAdminResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.validation.ActualizarParametroRequestValidator;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.validation.RequestValidationGuard;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * CULM-001 D.3 (HU168): parametros de negocio del administrador. GET paginado y legible, PUT con lista blanca, motivo y
 * concurrencia optimista ({@code fechaModificacionEsperada} UTC con milisegundos, reenviada tal como llego) y el historial
 * de cambios. Rol ADMINISTRADOR por {@code /api/v1/admin/**}.
 */
@RestController
@RequestMapping("/api/v1/admin/parametros")
public class AdminParametroController {

    private static final ActualizarParametroRequestValidator VALIDATOR = new ActualizarParametroRequestValidator();

    private final ConsultarParametrosInputPort consultarParametros;
    private final ActualizarParametroInputPort actualizarParametro;
    private final ConsultarCambiosParametroInputPort consultarCambios;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminParametroController(
            final ConsultarParametrosInputPort consultarParametros,
            final ActualizarParametroInputPort actualizarParametro,
            final ConsultarCambiosParametroInputPort consultarCambios,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarParametros = Objects.requireNonNull(consultarParametros, "ConsultarParametrosInputPort es obligatorio.");
        this.actualizarParametro = Objects.requireNonNull(actualizarParametro, "ActualizarParametroInputPort es obligatorio.");
        this.consultarCambios = Objects.requireNonNull(consultarCambios, "ConsultarCambiosParametroInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<ApiPageResponse<ParametroAdminResponse>> consultarParametros(
            @RequestParam(name = "grupo", required = false) final String grupo,
            @RequestParam(name = "texto", required = false) final String texto,
            @RequestParam(name = "soloEditables", required = false) final Boolean soloEditables,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final var resultado = consultarParametros.execute(new ConsultarParametrosInputPort.Comando(
                pagina, tamano, grupo, texto, soloEditables, authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(ApiPageResponse.of(
                resultado.elementos().stream().map(ParametroAdminResponse::from).toList(),
                resultado.numeroPagina(), resultado.tamanoPagina(), resultado.totalElementos()));
    }

    @PutMapping("/{id}")
    @AuditableOperation(action = "ACTUALIZAR_PARAMETRO", resourceType = "PARAMETRO", resourceIdPathVariable = "id",
            metadataResponseFields = {"grupo", "clave", "valorAnterior", "valorNuevo"}, metadataRequestFields = {"motivo"})
    public ResponseEntity<ApiDataResponse<ParametroActualizadoResponse>> actualizarParametro(
            @PathVariable("id") final UUID id,
            @RequestBody final ActualizarParametroRequest request
    ) {
        RequestValidationGuard.validate(VALIDATOR.validate(request));
        final var resultado = actualizarParametro.execute(new ActualizarParametroInputPort.Comando(
                id, request.valor(), Instant.parse(request.fechaModificacionEsperada().strip()), request.motivo(),
                authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, ParametroActualizadoResponse.from(resultado)));
    }

    @GetMapping("/{id}/cambios")
    public ResponseEntity<ApiPageResponse<CambioParametroResponse>> consultarCambios(
            @PathVariable("id") final UUID id,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final var resultado = consultarCambios.execute(new ConsultarCambiosParametroInputPort.Comando(
                id, pagina, tamano, authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(ApiPageResponse.of(
                resultado.elementos().stream().map(CambioParametroResponse::from).toList(),
                resultado.numeroPagina(), resultado.tamanoPagina(), resultado.totalElementos()));
    }

    /** {@code fechaModificacionEsperada}: ISO-8601 UTC con milisegundos, exactamente como la entrego el GET. */
    public record ActualizarParametroRequest(String valor, String fechaModificacionEsperada, String motivo) {
    }
}
