package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.realtime.sse;

import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * CULM-001 B.1 (CONTRACT 3.8, [SUPUESTO-B-17]): politica de audiencia por tipo de evento del canal SSE. Compara el tipo
 * por <b>igualdad exacta</b> (nunca por prefijo), de modo que un tipo {@code topic.action} emitido por
 * {@code POST /realtime/emit} no puede coincidir con un tipo de negocio. En B la unica audiencia es el docente titular
 * del grupo (el canal sigue abierto solo para el); un tipo no listado tambien se entrega solo al titular. El paquete C
 * extiende este mapa con el estudiante del grupo y los demas roles.
 */
public final class RealtimeAudienciaPolicy {

    /** Audiencias del canal SSE. En B solo existe la del docente titular del grupo suscrito. */
    public enum Audiencia {
        DOCENTE_TITULAR
    }

    private static final Set<Audiencia> SOLO_TITULAR = EnumSet.of(Audiencia.DOCENTE_TITULAR);

    private final Map<String, Set<Audiencia>> audienciasPorTipo;

    public RealtimeAudienciaPolicy(final Map<String, Set<Audiencia>> audienciasPorTipo) {
        this.audienciasPorTipo = Map.copyOf(Objects.requireNonNull(audienciasPorTipo, "El mapa de audiencias es obligatorio."));
    }

    /** Politica de B: los tipos de negocio conocidos se entregan al docente titular del grupo. */
    public static RealtimeAudienciaPolicy porDefecto() {
        return new RealtimeAudienciaPolicy(Map.of(
                "ASISTENCIAS_SESION_ACTUALIZADAS", SOLO_TITULAR,
                "ASISTENCIA_REGISTRADA", SOLO_TITULAR,
                "SESION_CANCELADA", SOLO_TITULAR,
                "MATRICULA_QR_ROTADO", SOLO_TITULAR,
                "ESTUDIANTE_MATRICULADO", SOLO_TITULAR
        ));
    }

    /** Audiencias del tipo por igualdad exacta; un tipo no listado (o nulo) llega solo al titular. */
    public Set<Audiencia> audienciasDe(final String tipo) {
        if (tipo == null) {
            return SOLO_TITULAR;
        }
        final Set<Audiencia> audiencias = audienciasPorTipo.get(tipo);
        return audiencias == null || audiencias.isEmpty() ? SOLO_TITULAR : audiencias;
    }

    public boolean entregaA(final String tipo, final Audiencia audiencia) {
        return audienciasDe(tipo).contains(audiencia);
    }
}
