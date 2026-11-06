package co.edu.uco.asistenciasuco.application.features.matriculaqr;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.matriculaqr.exception.MatriculaQrErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.network.RedInstitucionalPort;

import java.util.Objects;

/**
 * HU176B: reglas de acceso comunes a los endpoints de matricula por QR. Interruptor de la funcionalidad (D9, fail-closed
 * si el parametro falta) y pertenencia del cliente a la red del campus (CA-05).
 */
public final class MatriculaQrAccesoPolicy {

    public static final String GRUPO = "MATRICULA_QR";
    public static final String CLAVE_HABILITADA = "HABILITADA";

    private final ParameterCatalogPort parametros;
    private final RedInstitucionalPort redInstitucional;

    public MatriculaQrAccesoPolicy(final ParameterCatalogPort parametros, final RedInstitucionalPort redInstitucional) {
        this.parametros = Objects.requireNonNull(parametros, "ParameterCatalogPort es obligatorio.");
        this.redInstitucional = Objects.requireNonNull(redInstitucional, "RedInstitucionalPort es obligatorio.");
    }

    /** 404 si el administrador apago la funcionalidad o el parametro no existe. */
    public void exigirHabilitada() {
        final boolean habilitada = parametros.getParameter(GRUPO, CLAVE_HABILITADA)
                .map(valor -> Boolean.parseBoolean(valor.strip()))
                .orElse(false);
        if (!habilitada) {
            throw new ResourceNotFoundException("La matrícula por código no está disponible en este momento.");
        }
    }

    /** 403 MQR_002 si la IP no pertenece a los rangos del campus. */
    public void exigirRedCampus(final String direccionIp) {
        if (!redInstitucional.perteneceACampus(direccionIp)) {
            throw new ForbiddenException(MatriculaQrErrorCode.MQR_002);
        }
    }
}
