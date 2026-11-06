package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.network;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.network.RedInstitucionalPort;
import co.edu.uco.asistenciasuco.crosscutting.network.CidrRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Adaptador de {@link RedInstitucionalPort} basado en rangos CIDR configurados en el catalogo de parametros
 * ({@code RED/CIDRS_CAMPUS}, lista separada por comas).
 *
 * <p>Los rangos se leen en cada consulta (el catalogo ya cachea), de modo que un cambio del administrador aplica sin
 * reiniciar. Es fail-closed: sin rangos validos niega siempre. Nunca resuelve nombres de host.</p>
 */
public final class CidrRedInstitucionalAdapter implements RedInstitucionalPort {

    public static final String GRUPO = "RED";
    public static final String CLAVE_CIDRS_CAMPUS = "CIDRS_CAMPUS";

    private static final Logger LOGGER = LoggerFactory.getLogger(CidrRedInstitucionalAdapter.class);

    private final Supplier<Optional<String>> cidrs;

    public CidrRedInstitucionalAdapter(final Supplier<Optional<String>> cidrs) {
        this.cidrs = Objects.requireNonNull(cidrs, "El proveedor de rangos CIDR es obligatorio.");
    }

    public static CidrRedInstitucionalAdapter desdeCatalogo(final ParameterCatalogPort catalogo) {
        Objects.requireNonNull(catalogo, "ParameterCatalogPort es obligatorio.");
        return new CidrRedInstitucionalAdapter(() -> catalogo.getParameter(GRUPO, CLAVE_CIDRS_CAMPUS));
    }

    @Override
    public boolean perteneceACampus(final String direccionIp) {
        if (direccionIp == null || direccionIp.isBlank()) {
            return false;
        }
        final List<CidrRange> rangos = CidrRange.parseLista(cidrs.get().orElse(""));
        if (rangos.isEmpty()) {
            LOGGER.error("RED/CIDRS_CAMPUS no tiene rangos validos: se niega el acceso (fail-closed).");
            return false;
        }
        return rangos.stream().anyMatch(rango -> rango.contiene(direccionIp));
    }
}
