package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.features.festivos.consultarfestivos.primaryports.ConsultarFestivosAnioInputPort;
import co.edu.uco.asistenciasuco.application.features.festivos.consultarsesionesfestivo.primaryports.ConsultarSesionesFestivoConAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.festivos.recargarfestivos.primaryports.RecargarFestivosAnioInputPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.FestivosAnioResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.RecargaFestivosResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.SesionFestivoConAsistenciaResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiPageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.audit.web.AuditableOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * CULM-001 B.1 (§3.2.2): dias festivos nacionales del administrador. Solo consulta y recarga (no se editan a mano). La
 * recarga aplica los festivos a las sesiones futuras sin asistencia de los grupos de su institucion; las que ya tienen
 * asistencia se reportan en {@code sesiones-con-asistencia}.
 */
@RestController
@RequestMapping("/api/v1/admin/festivos")
public class AdminFestivoController {

    private final ConsultarFestivosAnioInputPort consultarFestivos;
    private final RecargarFestivosAnioInputPort recargarFestivos;
    private final ConsultarSesionesFestivoConAsistenciaInputPort consultarSesiones;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminFestivoController(
            final ConsultarFestivosAnioInputPort consultarFestivos,
            final RecargarFestivosAnioInputPort recargarFestivos,
            final ConsultarSesionesFestivoConAsistenciaInputPort consultarSesiones,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.consultarFestivos = Objects.requireNonNull(consultarFestivos, "ConsultarFestivosAnioInputPort es obligatorio.");
        this.recargarFestivos = Objects.requireNonNull(recargarFestivos, "RecargarFestivosAnioInputPort es obligatorio.");
        this.consultarSesiones = Objects.requireNonNull(consultarSesiones,
                "ConsultarSesionesFestivoConAsistenciaInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver,
                "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping
    public ResponseEntity<ApiDataResponse<FestivosAnioResponse>> consultarFestivos(
            @RequestParam("anio") final int anio,
            @RequestParam(name = "incluirInactivos", required = false, defaultValue = "false") final boolean incluirInactivos
    ) {
        final var resultado = consultarFestivos.execute(new ConsultarFestivosAnioInputPort.Comando(
                anio, incluirInactivos, authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, FestivosAnioResponse.from(resultado)));
    }

    @PostMapping("/{anio}/recarga")
    @AuditableOperation(action = "RECARGAR_FESTIVOS_ANIO", resourceType = "FESTIVOS", resourceIdPathVariable = "anio")
    public ResponseEntity<ApiDataResponse<RecargaFestivosResponse>> recargarFestivos(@PathVariable("anio") final int anio) {
        final var resultado = recargarFestivos.execute(new RecargarFestivosAnioInputPort.Comando(
                anio, authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(new ApiDataResponse<>(true, RecargaFestivosResponse.from(resultado)));
    }

    @GetMapping("/{anio}/sesiones-con-asistencia")
    public ResponseEntity<ApiPageResponse<SesionFestivoConAsistenciaResponse>> consultarSesionesConAsistencia(
            @PathVariable("anio") final int anio,
            @RequestParam(name = "pagina", required = false) final Integer pagina,
            @RequestParam(name = "tamano", required = false) final Integer tamano
    ) {
        final var resultado = consultarSesiones.execute(new ConsultarSesionesFestivoConAsistenciaInputPort.Comando(
                anio, pagina, tamano, authenticatedUserResolver.requireAuthenticatedUserId()));
        return ResponseEntity.ok(ApiPageResponse.of(
                resultado.elementos().stream().map(SesionFestivoConAsistenciaResponse::from).toList(),
                resultado.numeroPagina(), resultado.tamanoPagina(), resultado.totalElementos()));
    }
}
