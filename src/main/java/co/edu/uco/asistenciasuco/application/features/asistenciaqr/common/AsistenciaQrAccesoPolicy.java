package co.edu.uco.asistenciasuco.application.features.asistenciaqr.common;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.asistenciaqr.exception.AsistenciaQrErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.network.RedInstitucionalPort;

import java.util.Objects;
import java.util.Optional;

/**
 * * CULM-001 B.2 (DEC-015, DEC-018, DEC-045: limite por estudiante 5): reglas de acceso del QR de asistencia. Interruptor {@code ASISTENCIA_QR/HABILITADA}
 * (fail-closed: si falta o el catalogo falla, la funcionalidad no esta disponible), red del campus por
 * {@link RedInstitucionalPort} (fail-closed con CIDR vacio) y parametros numericos con valor por defecto igual a la semilla.
 */
public final class AsistenciaQrAccesoPolicy {

    public static final String GRUPO = "ASISTENCIA_QR";
    public static final int ROTACION_SEGUNDOS_POR_DEFECTO = 60;
    public static final int LIMITE_ESTUDIANTE_POR_DEFECTO = 5;
    public static final int LIMITE_TOKEN_POR_DEFECTO = 120;

    private final ParameterCatalogPort parametros;
    private final RedInstitucionalPort redInstitucional;

    public AsistenciaQrAccesoPolicy(final ParameterCatalogPort parametros, final RedInstitucionalPort redInstitucional) {
        this.parametros = Objects.requireNonNull(parametros, "ParameterCatalogPort es obligatorio.");
        this.redInstitucional = Objects.requireNonNull(redInstitucional, "RedInstitucionalPort es obligatorio.");
    }

    /** 404 {@code AQR_010} si el administrador apago la funcionalidad, el parametro no existe o el catalogo falla. */
    public void exigirHabilitada() {
        if (!estaHabilitada()) {
            throw new ResourceNotFoundException(AsistenciaQrErrorCode.AQR_010);
        }
    }

    public boolean estaHabilitada() {
        return leer("HABILITADA").map(valor -> Boolean.parseBoolean(valor.strip())).orElse(false);
    }

    /** 403 {@code AQR_002} si la IP no pertenece a {@code RED/CIDRS_CAMPUS}. */
    public void exigirRedCampus(final String direccionIp) {
        if (direccionIp == null || !redInstitucional.perteneceACampus(direccionIp)) {
            throw new ForbiddenException(AsistenciaQrErrorCode.AQR_002);
        }
    }

    public int rotacionSegundos() {
        return entero("ROTACION_SEGUNDOS", ROTACION_SEGUNDOS_POR_DEFECTO);
    }

    public int limiteEstudiantePorMinuto() {
        return entero("LIMITE_ESTUDIANTE_POR_MINUTO", LIMITE_ESTUDIANTE_POR_DEFECTO);
    }

    public int limiteTokenPorMinuto() {
        return entero("LIMITE_TOKEN_POR_MINUTO", LIMITE_TOKEN_POR_DEFECTO);
    }

    private int entero(final String clave, final int porDefecto) {
        return leer(clave).map(valor -> {
            try {
                final int numero = Integer.parseInt(valor.strip());
                return numero > 0 ? numero : porDefecto;
            } catch (final NumberFormatException _) {
                return porDefecto;
            }
        }).orElse(porDefecto);
    }

    private Optional<String> leer(final String clave) {
        try {
            return parametros.getParameter(GRUPO, clave);
        } catch (final ParameterCatalogPort.ParameterCatalogException _) {
            return Optional.empty();
        }
    }
}
