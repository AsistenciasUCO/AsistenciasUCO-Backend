package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.qr;

import co.edu.uco.asistenciasuco.application.secondaryports.qr.CodigoQrGeneratorPort;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Generador de QR en PNG con ZXing (D4). Corrección de errores M y margen de 2 módulos.
 */
public final class ZxingCodigoQrGeneratorAdapter implements CodigoQrGeneratorPort {

    private static final int LADO_MINIMO = 64;
    private static final int LADO_MAXIMO = 2048;

    @Override
    public byte[] generarPng(final String contenido, final int lado) {
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("El contenido del QR es obligatorio.");
        }
        if (lado < LADO_MINIMO || lado > LADO_MAXIMO) {
            throw new IllegalArgumentException("El lado del QR debe estar entre " + LADO_MINIMO + " y " + LADO_MAXIMO + " pixeles.");
        }
        try {
            final BitMatrix matriz = new QRCodeWriter().encode(
                    contenido,
                    BarcodeFormat.QR_CODE,
                    lado,
                    lado,
                    Map.of(
                            EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M,
                            EncodeHintType.MARGIN, 2,
                            EncodeHintType.CHARACTER_SET, "UTF-8"
                    )
            );
            final ByteArrayOutputStream salida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", salida);
            return salida.toByteArray();
        } catch (final WriterException | IOException exception) {
            throw new IllegalStateException("No fue posible generar el codigo QR.", exception);
        }
    }
}
