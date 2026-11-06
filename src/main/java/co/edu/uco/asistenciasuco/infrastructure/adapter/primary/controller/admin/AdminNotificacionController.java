package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin;

import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.primaryports.AdministrarNotificacionesInputPort;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.EstadoCanalNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.MarcadorPlantillaProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.NotificacionSalienteAdminProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.PaginaColaNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.ResumenColaNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.TipoPlantillaAdminProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VersionPlantillaProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VistaPreviaPlantillaProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request.CrearVersionPlantillaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request.EnviarCorreoPruebaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.request.VistaPreviaPlantillaRequest;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.ConsultaColaNotificacionesResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.EstadoCanalNotificacionResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.MarcadorPlantillaResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.NotificacionSalienteResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.ResumenColaNotificacionesResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.TipoPlantillaAdminResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.VersionPlantillaResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response.VistaPreviaPlantillaResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiListResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiMessageResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
 * Endpoints administrativos para la gestion de plantillas de correo, visualizacion del canal
 * y monitoreo/reintento de la cola outbox (NOTIF-002B + TD-069).
 */
@RestController
@RequestMapping("/api/v1/admin/notificaciones")
public class AdminNotificacionController {

    private final AdministrarNotificacionesInputPort administracionPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public AdminNotificacionController(
            final AdministrarNotificacionesInputPort administracionPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.administracionPort = Objects.requireNonNull(administracionPort, "AdministrarNotificacionesInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @GetMapping("/estado-canal")
    public ResponseEntity<ApiDataResponse<EstadoCanalNotificacionResponse>> consultarEstadoCanal() {
        final EstadoCanalNotificacionProjection p = administracionPort.consultarEstadoCanal();
        final EstadoCanalNotificacionResponse resp = new EstadoCanalNotificacionResponse(
                p.provider(),
                p.sender(),
                p.dispatcherEnabled(),
                p.disponible(),
                p.host(),
                p.port(),
                p.username(),
                p.tienePasswordConfigurado(),
                p.fromAddress(),
                p.fromName(),
                p.starttls(),
                p.ssl(),
                p.maxBcc(),
                p.ratePerMinute(),
                p.ratePerDay(),
                p.requireConfirmedEmail(),
                p.platformUrl(),
                p.testModeEnabled(),
                p.testModeRedirectCount()
        );
        return ResponseEntity.ok(new ApiDataResponse<>(true, resp));
    }

    @GetMapping("/plantillas")
    public ResponseEntity<ApiListResponse<TipoPlantillaAdminResponse>> consultarPlantillas(
            @RequestParam(defaultValue = "es") final String idioma
    ) {
        final UUID adminId = authenticatedUserResolver.requireAuthenticatedUserId();
        final List<TipoPlantillaAdminProjection> list = administracionPort.consultarPlantillas(idioma, adminId);
        final List<TipoPlantillaAdminResponse> responses = list.stream()
                .map(AdminNotificacionController::toTipoPlantillaResponse)
                .toList();
        return ResponseEntity.ok(new ApiListResponse<>(true, responses, responses.size()));
    }

    @PostMapping("/plantillas/{tipo}/versiones")
    public ResponseEntity<ApiDataResponse<VersionPlantillaResponse>> crearVersion(
            @PathVariable final String tipo,
            @RequestBody final CrearVersionPlantillaRequest request
    ) {
        final UUID adminId = authenticatedUserResolver.requireAuthenticatedUserId();
        final VersionPlantillaProjection version = administracionPort.crearVersionPlantilla(
                tipo,
                request != null ? request.idioma() : "es",
                request != null ? request.asunto() : "",
                request != null ? request.cuerpo() : "",
                adminId
        );
        return ResponseEntity.ok(new ApiDataResponse<>(true, toVersionResponse(version)));
    }

    @PatchMapping("/plantillas/{tipo}/versiones/{version}/activar")
    public ResponseEntity<ApiMessageResponse> activarVersion(
            @PathVariable final String tipo,
            @PathVariable final int version,
            @RequestParam(defaultValue = "es") final String idioma
    ) {
        final UUID adminId = authenticatedUserResolver.requireAuthenticatedUserId();
        administracionPort.activarVersionPlantilla(tipo, idioma, version, adminId);
        return ResponseEntity.ok(new ApiMessageResponse(true, "Versión " + version + " de la plantilla " + tipo + " activada exitosamente."));
    }

    @PostMapping("/plantillas/{tipo}/vista-previa")
    public ResponseEntity<ApiDataResponse<VistaPreviaPlantillaResponse>> generarVistaPrevia(
            @PathVariable final String tipo,
            @RequestBody final VistaPreviaPlantillaRequest request
    ) {
        final VistaPreviaPlantillaProjection prev = administracionPort.generarVistaPrevia(
                tipo,
                request != null ? request.idioma() : "es",
                request != null ? request.asunto() : "",
                request != null ? request.cuerpo() : ""
        );
        return ResponseEntity.ok(new ApiDataResponse<>(true, new VistaPreviaPlantillaResponse(
                prev.asunto(),
                prev.textoPlano(),
                prev.html(),
                prev.origen()
        )));
    }

    @GetMapping("/cola")
    public ResponseEntity<ApiDataResponse<ConsultaColaNotificacionesResponse>> consultarCola(
            @RequestParam(required = false) final String estado,
            @RequestParam(required = false) final String tipo,
            @RequestParam(defaultValue = "1") final int pagina,
            @RequestParam(defaultValue = "20") final int tamano
    ) {
        final UUID adminId = authenticatedUserResolver.requireAuthenticatedUserId();
        final PaginaColaNotificacionProjection pag = administracionPort.consultarCola(estado, tipo, pagina, tamano, adminId);
        final List<NotificacionSalienteResponse> items = pag.elementos().stream()
                .map(AdminNotificacionController::toSalienteResponse)
                .toList();

        final ResumenColaNotificacionProjection r = pag.resumen();
        final ResumenColaNotificacionesResponse resumen = new ResumenColaNotificacionesResponse(
                r != null ? r.pendientes() : 0,
                r != null ? r.reservadas() : 0,
                r != null ? r.entregadas() : 0,
                r != null ? r.fallidasReintentables() : 0,
                r != null ? r.fallidasDefinitivas() : 0
        );

        final ConsultaColaNotificacionesResponse response = new ConsultaColaNotificacionesResponse(
                items,
                pag.numeroPagina(),
                pag.tamanoPagina(),
                pag.totalElementos(),
                pag.totalPaginas(),
                resumen
        );
        return ResponseEntity.ok(new ApiDataResponse<>(true, response));
    }

    @PostMapping("/cola/{id}/reintentar")
    public ResponseEntity<ApiMessageResponse> reintentar(
            @PathVariable final UUID id
    ) {
        final UUID adminId = authenticatedUserResolver.requireAuthenticatedUserId();
        administracionPort.reintentarNotificacion(id, adminId);
        return ResponseEntity.ok(new ApiMessageResponse(true, "Notificación encolada para reintento inmediato."));
    }

    @PostMapping("/prueba")
    public ResponseEntity<ApiMessageResponse> enviarPrueba(
            @RequestBody(required = false) final EnviarCorreoPruebaRequest request
    ) {
        final UUID adminId = authenticatedUserResolver.requireAuthenticatedUserId();
        final String tipo = request != null ? request.tipo() : null;
        final String correo = request != null ? request.correoDestino() : null;
        administracionPort.enviarCorreoPrueba(adminId, tipo, correo);
        return ResponseEntity.ok(new ApiMessageResponse(true, "Mensaje de prueba emitido exitosamente."));
    }

    private static TipoPlantillaAdminResponse toTipoPlantillaResponse(final TipoPlantillaAdminProjection p) {
        final List<MarcadorPlantillaResponse> marcadores = p.marcadores().stream()
                .map(m -> new MarcadorPlantillaResponse(m.marcador(), m.descripcion(), m.ejemplo(), m.obligatorio()))
                .toList();
        final List<VersionPlantillaResponse> versiones = p.versiones().stream()
                .map(AdminNotificacionController::toVersionResponse)
                .toList();
        return new TipoPlantillaAdminResponse(
                p.tipo(),
                p.idioma(),
                p.versionActiva(),
                p.asuntoActivo(),
                p.cuerpoActivo(),
                p.actualizadoEn(),
                marcadores,
                versiones
        );
    }

    private static VersionPlantillaResponse toVersionResponse(final VersionPlantillaProjection v) {
        return new VersionPlantillaResponse(
                v.id(),
                v.tipo(),
                v.idioma(),
                v.version(),
                v.asunto(),
                v.cuerpo(),
                v.activa(),
                v.creadoEn()
        );
    }

    private static NotificacionSalienteResponse toSalienteResponse(final NotificacionSalienteAdminProjection n) {
        return new NotificacionSalienteResponse(
                n.id(),
                n.tipo(),
                n.asunto(),
                n.codigoEstado(),
                n.nombreEstado(),
                n.esTerminal(),
                n.cantidadDestinatarios(),
                n.intentos(),
                n.creadoEn(),
                n.proximoIntentoEn(),
                n.entregadoEn(),
                n.codigoError(),
                n.detalleError(),
                n.referenciaTraza()
        );
    }
}
