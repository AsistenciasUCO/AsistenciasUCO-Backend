package co.edu.uco.asistenciasuco.application.features.matriculaqr.usecase;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.qr.CodigoQrGeneratorPort;

import java.util.Base64;
import java.util.Objects;

/**
 * HU176B: arma la URL de matricula con el token y la imagen QR (PNG en base64). La URL base es el parametro
 * {@code MATRICULA_QR/URL_BASE_FRONTEND}. El token nunca se loguea.
 */
public final class MatriculaQrPresentador {

    static final int LADO_QR = 320;
    private static final String RUTA = "/matricula/qr?t=";

    public record Presentacion(String urlMatricula, String qrPngBase64) {
    }

    private final ParameterCatalogPort parametros;
    private final CodigoQrGeneratorPort qrGenerator;

    public MatriculaQrPresentador(final ParameterCatalogPort parametros, final CodigoQrGeneratorPort qrGenerator) {
        this.parametros = Objects.requireNonNull(parametros, "ParameterCatalogPort es obligatorio.");
        this.qrGenerator = Objects.requireNonNull(qrGenerator, "CodigoQrGeneratorPort es obligatorio.");
    }

    public Presentacion presentar(final String token) {
        final String base = parametros.getRequiredParameter("MATRICULA_QR", "URL_BASE_FRONTEND").strip();
        final String baseSinBarra = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        final String url = baseSinBarra + RUTA + token;
        final byte[] png = qrGenerator.generarPng(url, LADO_QR);
        return new Presentacion(url, Base64.getEncoder().encodeToString(png));
    }
}
