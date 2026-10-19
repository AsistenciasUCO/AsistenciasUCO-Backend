package co.edu.uco.asistenciasuco.testsupport.files;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Ambiente de los IT reales de soportes (MinIO + ClamAV de {@code infra/files/compose.yaml}).
 *
 * <p>No hay credenciales ni endpoints por defecto: apuntar en silencio a {@code localhost:9000}
 * podria certificar otro MinIO (p. ej. un contenedor heredado) con credenciales desconocidas. Si
 * falta una variable la prueba FALLA de forma explicita; nunca se omite ni pasa en falso. Nunca se
 * imprimen valores, solo nombres de variables.</p>
 */
public final class FilesIntegrationEnvironment {

    /** Bucket congelado por MINIO_STORAGE_CONTRACT; no es un secreto. */
    public static final String DEFAULT_BUCKET = "asistencias-soportes";

    private FilesIntegrationEnvironment() {
    }

    public static String minioEndpoint() {
        return required("MINIO_ENDPOINT");
    }

    public static String minioAccessKey() {
        return required("MINIO_ACCESS_KEY");
    }

    public static String minioSecretKey() {
        return required("MINIO_SECRET_KEY");
    }

    public static String minioBucket() {
        final String value = System.getenv("MINIO_BUCKET");
        return value == null || value.isBlank() ? DEFAULT_BUCKET : value.trim();
    }

    public static String clamAvHost() {
        return required("CLAMAV_HOST");
    }

    public static int clamAvPort() {
        return Integer.parseInt(required("CLAMAV_PORT"));
    }

    public static String minioHost() {
        return URI.create(minioEndpoint()).getHost();
    }

    public static int minioPort() {
        return URI.create(minioEndpoint()).getPort();
    }

    private static String required(final String name) {
        final String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fail("Ambiente de archivos no disponible: defina " + name
                    + " (ver infra/files/README.md). La prueba no usa valores por defecto.");
        }
        return value.trim();
    }
}
