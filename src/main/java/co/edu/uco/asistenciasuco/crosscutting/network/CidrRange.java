package co.edu.uco.asistenciasuco.crosscutting.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Rango CIDR IPv4/IPv6 inmutable. Solo interpreta direcciones IP literales: jamas resuelve nombres de host por DNS.
 *
 * <p>Las direcciones IPv4 se normalizan a su forma IPv6 mapeada (16 bytes) para comparar IPv4 e IPv6 con la misma
 * logica; asi {@code ::ffff:10.1.2.3} pertenece a {@code 10.0.0.0/8}.</p>
 */
public final class CidrRange {

    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:.]+$");
    private static final int BYTES = 16;
    private static final int PREFIJO_IPV4_MAPEADO = 96;

    private final byte[] red;
    private final int prefijo;

    private CidrRange(final byte[] red, final int prefijo) {
        this.red = red;
        this.prefijo = prefijo;
    }

    /** Interpreta {@code a.b.c.d/n} o {@code x:x::x/n}; devuelve vacio si el texto no es un CIDR valido. */
    public static Optional<CidrRange> parse(final String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        final String limpio = texto.strip();
        final int barra = limpio.indexOf('/');
        if (barra <= 0 || barra == limpio.length() - 1 || barra != limpio.lastIndexOf('/')) {
            return Optional.empty();
        }
        final String direccion = limpio.substring(0, barra);
        final int prefijo;
        try {
            prefijo = Integer.parseInt(limpio.substring(barra + 1));
        } catch (final NumberFormatException _) {
            return Optional.empty();
        }
        final boolean esIpv4 = IPV4.matcher(direccion).matches();
        final int maximo = esIpv4 ? 32 : 128;
        if (prefijo < 0 || prefijo > maximo) {
            return Optional.empty();
        }
        return normalizar(direccion)
                .map(bytes -> new CidrRange(bytes, esIpv4 ? PREFIJO_IPV4_MAPEADO + prefijo : prefijo));
    }

    /** Lista de CIDR separada por comas; ignora los elementos invalidos o vacios. */
    public static List<CidrRange> parseLista(final String texto) {
        final List<CidrRange> rangos = new ArrayList<>();
        if (texto == null) {
            return rangos;
        }
        for (final String elemento : texto.split(",")) {
            parse(elemento).ifPresent(rangos::add);
        }
        return rangos;
    }

    /** {@code true} si el texto es una direccion IP literal valida (IPv4 o IPv6). */
    public static boolean esDireccionIpValida(final String texto) {
        return texto != null && normalizar(texto.strip()).isPresent();
    }

    public boolean contiene(final String direccion) {
        if (direccion == null) {
            return false;
        }
        return normalizar(direccion.strip()).map(this::contiene).orElse(false);
    }

    private boolean contiene(final byte[] otra) {
        final int bytesCompletos = prefijo / 8;
        for (int i = 0; i < bytesCompletos; i++) {
            if (red[i] != otra[i]) {
                return false;
            }
        }
        final int bitsRestantes = prefijo % 8;
        if (bitsRestantes == 0) {
            return true;
        }
        final int mascara = (0xFF << (8 - bitsRestantes)) & 0xFF;
        return (red[bytesCompletos] & mascara) == (otra[bytesCompletos] & mascara);
    }

    private static Optional<byte[]> normalizar(final String direccion) {
        if (direccion == null || direccion.isEmpty()) {
            return Optional.empty();
        }
        final boolean esIpv4 = IPV4.matcher(direccion).matches();
        if (!esIpv4 && (!direccion.contains(":") || !IPV6.matcher(direccion).matches())) {
            return Optional.empty();
        }
        if (esIpv4 && !octetosValidos(direccion)) {
            return Optional.empty();
        }
        try {
            final byte[] crudo = InetAddress.getByName(direccion).getAddress();
            if (crudo.length == BYTES) {
                return Optional.of(crudo);
            }
            final byte[] mapeada = new byte[BYTES];
            mapeada[10] = (byte) 0xFF;
            mapeada[11] = (byte) 0xFF;
            System.arraycopy(crudo, 0, mapeada, 12, 4);
            return Optional.of(mapeada);
        } catch (final UnknownHostException _) {
            return Optional.empty();
        }
    }

    private static boolean octetosValidos(final String ipv4) {
        for (final String octeto : ipv4.split("\\.")) {
            if (Integer.parseInt(octeto) > 255) {
                return false;
            }
        }
        return true;
    }
}
