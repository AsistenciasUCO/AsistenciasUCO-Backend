package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.localsse;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.RealtimeAudienciaPolicy;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.contract.RealtimeStreamGateway;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse.response.RealtimeEventResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.realtime.localsse.ReactorRealtimeAdapter;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Bridge interno entre el controller SSE y el adapter local seleccionado por el Composition Root.
 *
 * <p>No se autoregistra con Spring. {@code LocalSseRealtimeAdapterConfiguration} crea esta
 * implementacion solamente cuando el provider realtime es {@code local-sse}.</p>
 *
 * <p>Autorizacion y scope por grupo: la titularidad del docente sobre {@code grupoId} se valida
 * UNA vez, sincronicamente, al establecer la suscripcion (antes de construir el {@link Flux}).
 * Despues, el stream se filtra EN MEMORIA para entregar unicamente eventos cuyo
 * {@code payload.grupo} coincide con el grupo solicitado; un evento sin {@code grupo} en su
 * payload nunca se entrega por este canal scopeado.</p>
 */
public final class LocalSseRealtimeStreamGateway implements RealtimeStreamGateway {

    private final ReactorRealtimeAdapter realtimeAdapter;
    private final InstitutionalScopePort institutionalScopePort;
    private final RealtimeAudienciaPolicy audienciaPolicy;

    public LocalSseRealtimeStreamGateway(
            final ReactorRealtimeAdapter realtimeAdapter,
            final InstitutionalScopePort institutionalScopePort
    ) {
        this(realtimeAdapter, institutionalScopePort, RealtimeAudienciaPolicy.porDefecto());
    }

    /** CULM-001 B.1 (3.8): la politica de audiencia decide por tipo exacto que eventos llegan a cada audiencia. */
    public LocalSseRealtimeStreamGateway(
            final ReactorRealtimeAdapter realtimeAdapter,
            final InstitutionalScopePort institutionalScopePort,
            final RealtimeAudienciaPolicy audienciaPolicy
    ) {
        this.realtimeAdapter = Objects.requireNonNull(realtimeAdapter, "ReactorRealtimeAdapter es requerido.");
        this.institutionalScopePort = Objects.requireNonNull(institutionalScopePort, "InstitutionalScopePort es requerido.");
        this.audienciaPolicy = Objects.requireNonNull(audienciaPolicy, "RealtimeAudienciaPolicy es requerida.");
    }

    @Override
    public Flux<RealtimeEventResponse> subscribe(final UUID usuarioId, final UUID grupoId) {
        Objects.requireNonNull(usuarioId, "El usuario autenticado es obligatorio para suscribirse a realtime.");
        Objects.requireNonNull(grupoId, "El grupoId es obligatorio para suscribirse a realtime.");
        if (!institutionalScopePort.canDocenteAccessGrupo(usuarioId, grupoId)) {
            throw new ForbiddenException("El docente autenticado no tiene titularidad sobre el grupo solicitado.");
        }

        final String grupoIdTexto = grupoId.toString();
        return realtimeAdapter.events()
                .filter(event -> grupoIdTexto.equals(event.payload().get("grupo")))
                // En B el suscriptor es siempre el docente titular (validado arriba); C agrega otras audiencias.
                .filter(event -> audienciaPolicy.entregaA(event.type(), RealtimeAudienciaPolicy.Audiencia.DOCENTE_TITULAR))
                .map(RealtimeEventResponse::from);
    }

    @Override
    public int activeSubscribersCount() {
        return realtimeAdapter.activeSubscribersCount();
    }

    @Override
    public void emit(final String type, final Map<String, Object> payload) {
        realtimeAdapter.publish(RealtimeEvent.of(type, payload));
    }
}
