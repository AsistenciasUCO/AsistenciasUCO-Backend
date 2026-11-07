package co.edu.uco.asistenciasuco.application.features.asistenciaqr.common;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.qr.CodigoQrGeneratorPort;

import java.util.Base64;
import java.util.Objects;

/**
 * CULM-001 B.2 (HU175): arma la URL de asistencia con el token ({@code ASISTENCIA_QR/URL_BASE_FRONTEND} +
 * {@code /app/asistencia/qr?t=}) y el QR PNG de 320 px en base64. El token nunca se loguea.
 */
public final class AsistenciaQrPresentador {

    public static final int LADO_QR = 320;
    public static final String RUTA = "/app/asistencia/qr?t=";
    public static final String URL_BASE_POR_DEFECTO = "http://localhost:4200";

    public record Presentacion(String urlAsistencia, String qrPngBase64) {
    }

    private final ParameterCatalogPort parametros;
    private final CodigoQrGeneratorPort qrGenerator;

    public AsistenciaQrPresentador(final ParameterCatalogPort parametros, final CodigoQrGeneratorPort qrGenerator) {
        this.parametros = Objects.requireNonNull(parametros, "ParameterCatalogPort es obligatorio.");
        this.qrGenerator = Objects.requireNonNull(qrGenerator, "CodigoQrGeneratorPort es obligatorio.");
    }

    public Presentacion presentar(final String token) {
        String base;
        try {
            base = parametros.getParameter(AsistenciaQrAccesoPolicy.GRUPO, "URL_BASE_FRONTEND")
                    .map(String::strip).filter(valor -> !valor.isEmpty()).orElse(URL_BASE_POR_DEFECTO);
        } catch (final ParameterCatalogPort.ParameterCatalogException _) {
            base = URL_BASE_POR_DEFECTO;
        }
        final String baseSinBarra = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        final String url = baseSinBarra + RUTA + token;
        return new Presentacion(url, Base64.getEncoder().encodeToString(qrGenerator.generarPng(url, LADO_QR)));
    }
}
