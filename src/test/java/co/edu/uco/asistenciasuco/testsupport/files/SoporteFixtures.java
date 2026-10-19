package co.edu.uco.asistenciasuco.testsupport.files;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fixtures sinteticos de soportes para pruebas. Solo usa la firma de prueba estandar EICAR
 * (https://www.eicar.org/), nunca malware real.
 */
public final class SoporteFixtures {

    public static final String EICAR_TEST_STRING =
            "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";

    public static final int MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;

    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

    private SoporteFixtures() {
    }

    /** PDF estructurado minimo cuyo unico contenido es un archivo embebido con el payload dado. */
    public static byte[] pdfWithEmbeddedFile(final byte[] embeddedPayload) {
        final List<byte[]> objects = new ArrayList<>();
        objects.add(ascii("<< /Type /Catalog /Pages 2 0 R /Names << /EmbeddedFiles << /Names [(soporte.txt) 4 0 R] >> >> >>"));
        objects.add(ascii("<< /Type /Pages /Kids [] /Count 0 >>"));
        objects.add(concat(
                ascii("<< /Type /EmbeddedFile /Length " + embeddedPayload.length + " >>\nstream\n"),
                embeddedPayload,
                ascii("\nendstream")));
        objects.add(ascii("<< /Type /Filespec /F (soporte.txt) /EF << /F 3 0 R >> >>"));

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(ascii("%PDF-1.4\n"));
        final List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            out.writeBytes(ascii((i + 1) + " 0 obj\n"));
            out.writeBytes(objects.get(i));
            out.writeBytes(ascii("\nendobj\n"));
        }
        final int xref = out.size();
        out.writeBytes(ascii("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n"));
        for (final int offset : offsets) {
            out.writeBytes(ascii(String.format("%010d 00000 n \n", offset)));
        }
        out.writeBytes(ascii("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n"
                + xref + "\n%%EOF\n"));
        return out.toByteArray();
    }

    /**
     * PDF valido (magic {@code %PDF-} en offset 0) que transporta la firma EICAR dentro de un
     * stream embebido. ClamAV extrae el stream y lo detecta; {@code %PDF-} + EICAR plano no.
     */
    public static byte[] pdfWithEmbeddedEicar() {
        return pdfWithEmbeddedFile(ascii(EICAR_TEST_STRING));
    }

    /** PDF altamente compresible (texto repetido): la politica debe almacenarlo comprimido. */
    public static byte[] compressiblePdf(final int approximateSize) {
        final StringBuilder text = new StringBuilder(approximateSize + 64);
        while (text.length() < approximateSize) {
            text.append("Soporte de revision de asistencia LB-004, contenido compresible. ");
        }
        return pdfWithEmbeddedFile(ascii(text.toString()));
    }

    /** PDF con payload pseudoaleatorio deterministico (no compresible) de longitud exacta. */
    public static byte[] incompressiblePdfOfExactSize(final int totalSize, final long seed) {
        return withMagicAndRandomPayload(ascii("%PDF-1.4\n"), totalSize, seed);
    }

    public static byte[] png(final int totalSize, final long seed) {
        return withMagicAndRandomPayload(PNG_MAGIC, totalSize, seed);
    }

    public static byte[] jpeg(final int totalSize, final long seed) {
        return withMagicAndRandomPayload(JPEG_MAGIC, totalSize, seed);
    }

    private static byte[] withMagicAndRandomPayload(final byte[] magic, final int totalSize, final long seed) {
        final byte[] content = new byte[totalSize];
        new Random(seed).nextBytes(content);
        System.arraycopy(magic, 0, content, 0, magic.length);
        return content;
    }

    private static byte[] ascii(final String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] concat(final byte[]... parts) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (final byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }
}
