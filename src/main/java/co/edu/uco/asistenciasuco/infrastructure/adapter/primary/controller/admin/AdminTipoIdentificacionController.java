package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.ConsultarTiposIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.primaryports.GuardarTipoIdentificacionInputPort;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.guardartipoidentificacion.primaryports.dto.GuardarTipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * HU153: el administrador crea tipos de documento y edita su nombre (sin inactivar). El listado sigue en
 * {@code GET /api/v1/tipos-identificacion}. Rol ADMINISTRADOR por {@code /api/v1/admin/**} de SecurityConfig.
 */
@RestController
@RequestMapping("/api/v1/admin/tipos-identificacion")
public final class AdminTipoIdentificacionController {

    private static final String NO_EXISTE = "El tipo de documento especificado no existe en el sistema.";

    private final GuardarTipoIdentificacionInputPort guardarInputPort;
    private final ConsultarTiposIdentificacionInputPort consultarInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminTipoIdentificacionController(final GuardarTipoIdentificacionInputPort guardarInputPort,
                                             final ConsultarTiposIdentificacionInputPort consultarInputPort,
                                             final AuthenticatedUserResolver authenticatedUserResolver) {
        this.guardarInputPort = Objects.requireNonNull(guardarInputPort);
        this.consultarInputPort = Objects.requireNonNull(consultarInputPort);
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver);
    }

    @PostMapping
    public ResponseEntity<ApiDataResponse<TipoIdentificacionDTO>> crear(
            @RequestBody final CrearTipoIdentificacionRequest request) {
        final String codigo = request != null ? request.codigo() : null;
        guardarInputPort.execute(new GuardarTipoIdentificacionDTO(null, codigo,
                request != null ? request.nombre() : null, authenticatedUserResolver.requireAuthenticatedUserId()));
        final String codigoNormalizado = codigo.trim().toUpperCase(Locale.ROOT);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true,
                releer(t -> codigoNormalizado.equals(t.getTipoIdentificacion()))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiDataResponse<TipoIdentificacionDTO>> actualizar(
            @PathVariable final UUID id,
            @RequestBody final ActualizarTipoIdentificacionRequest request) {
        guardarInputPort.execute(new GuardarTipoIdentificacionDTO(id, null,
                request != null ? request.nombre() : null, authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, releer(t -> id.equals(t.getId()))));
    }

    private TipoIdentificacionDTO releer(final Predicate<TipoIdentificacionDTO> criterio) {
        return consultarInputPort.execute().stream()
                .filter(criterio)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(NO_EXISTE));
    }

    public record CrearTipoIdentificacionRequest(String codigo, String nombre) {
    }

    public record ActualizarTipoIdentificacionRequest(String nombre) {
    }
}
