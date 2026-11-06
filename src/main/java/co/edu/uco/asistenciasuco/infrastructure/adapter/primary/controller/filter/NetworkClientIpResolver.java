package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.crosscutting.network.CidrRange;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Resuelve la IP del cliente que se compara con los rangos del campus (D5). A diferencia de {@link ClientIpResolver},
 * que solo sanea la IP para los logs, esta devuelve la IP sin sanear y puede leer {@code X-Forwarded-For}.
 *
 * <p>Solo se confia en {@code X-Forwarded-For} si {@code remoteAddr} pertenece a {@code RED/PROXIES_CONFIABLES}. Entonces
 * se recorre el encabezado de derecha a izquierda saltando los proxies confiables; el primer salto no confiable es el
 * cliente. Cualquier valor invalido o ausente hace caer a {@code remoteAddr}: un encabezado falsificado nunca puede
 * hacerse pasar por una IP del campus.</p>
 */
public final class NetworkClientIpResolver {

    private static final String HEADER = "X-Forwarded-For";

    private final Supplier<Optional<String>> proxiesConfiables;

    public NetworkClientIpResolver(final Supplier<Optional<String>> proxiesConfiables) {
        this.proxiesConfiables = Objects.requireNonNull(proxiesConfiables, "El proveedor de proxies es obligatorio.");
    }

    public String resolve(final HttpServletRequest request) {
        final String remoto = request.getRemoteAddr() == null ? "" : request.getRemoteAddr().strip();
        final List<CidrRange> proxies = CidrRange.parseLista(proxiesConfiables.get().orElse(""));
        if (proxies.isEmpty() || !esConfiable(remoto, proxies)) {
            return remoto;
        }
        final String encabezado = request.getHeader(HEADER);
        if (encabezado == null || encabezado.isBlank()) {
            return remoto;
        }
        final String[] saltos = encabezado.split(",");
        for (int i = saltos.length - 1; i >= 0; i--) {
            final String salto = saltos[i].strip();
            if (!CidrRange.esDireccionIpValida(salto)) {
                return remoto;
            }
            if (!esConfiable(salto, proxies)) {
                return salto;
            }
        }
        return remoto;
    }

    private static boolean esConfiable(final String direccion, final List<CidrRange> proxies) {
        return proxies.stream().anyMatch(rango -> rango.contiene(direccion));
    }
}
