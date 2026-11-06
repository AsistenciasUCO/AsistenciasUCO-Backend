package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.qr;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * HU176B (CA-01, CA-09, T-01): el backend genera el PNG del QR (D4); el contenido leido debe ser exactamente la URL.
 */
class ZxingCodigoQrGeneratorAdapterTest {

    private final ZxingCodigoQrGeneratorAdapter generator = new ZxingCodigoQrGeneratorAdapter();

    @Test
    void el_png_decodifica_exactamente_la_url_de_matricula() throws Exception {
        final String url = "https://asistencia.uco.edu.co/matricula/qr?t=AbC-123_xyZ-AbC-123_xyZ-AbC-123_xyZ-AbC12";

        final byte[] png = generator.generarPng(url, 320);

        assertEquals(url, leer(png));
    }

    @Test
    void el_png_tiene_el_lado_solicitado_y_firma_png() throws Exception {
        final byte[] png = generator.generarPng("https://x.test/matricula/qr?t=abc", 320);

        assertArrayEquals(new byte[]{(byte) 0x89, 'P', 'N', 'G'}, new byte[]{png[0], png[1], png[2], png[3]});
        final BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(png));
        assertEquals(320, imagen.getWidth());
        assertEquals(320, imagen.getHeight());
    }

    @Test
    void rechaza_contenido_nulo_o_vacio_y_lados_fuera_de_rango() {
        assertThrows(IllegalArgumentException.class, () -> generator.generarPng(null, 320));
        assertThrows(IllegalArgumentException.class, () -> generator.generarPng("  ", 320));
        assertThrows(IllegalArgumentException.class, () -> generator.generarPng("https://x.test", 0));
        assertThrows(IllegalArgumentException.class, () -> generator.generarPng("https://x.test", 63));
        assertThrows(IllegalArgumentException.class, () -> generator.generarPng("https://x.test", 2049));
    }

    private static String leer(final byte[] png) throws Exception {
        final BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(png));
        final BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(imagen)));
        return new MultiFormatReader().decode(bitmap).getText();
    }
}
