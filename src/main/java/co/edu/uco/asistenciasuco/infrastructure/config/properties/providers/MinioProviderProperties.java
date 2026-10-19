package co.edu.uco.asistenciasuco.infrastructure.config.properties.providers;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * Propiedades de configuracion para la conexion con MinIO. {@code accessKey}/{@code secretKey}
 * se resuelven exclusivamente desde variables de entorno (sin valor por defecto funcional);
 * cero secretos hardcoded.
 *
 * <p>Los timeouts acotan cuanto puede bloquearse una request si MinIO acepta la conexion pero no
 * responde; sin ellos el SDK espera hasta 5 minutos. Valores no positivos usan el default.</p>
 */
@ConfigurationProperties(prefix = "app.providers.minio")
public record MinioProviderProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        int connectTimeoutMillis,
        int writeTimeoutMillis,
        int readTimeoutMillis
) {
    public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5000;
    public static final int DEFAULT_WRITE_TIMEOUT_MILLIS = 30000;
    public static final int DEFAULT_READ_TIMEOUT_MILLIS = 30000;

    @ConstructorBinding
    public MinioProviderProperties {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("app.providers.minio.endpoint es obligatorio.");
        }
        if (accessKey == null || accessKey.isBlank()) {
            throw new IllegalStateException("app.providers.minio.access-key es obligatorio.");
        }
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("app.providers.minio.secret-key es obligatorio.");
        }
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("app.providers.minio.bucket es obligatorio.");
        }
        if (connectTimeoutMillis <= 0) {
            connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS;
        }
        if (writeTimeoutMillis <= 0) {
            writeTimeoutMillis = DEFAULT_WRITE_TIMEOUT_MILLIS;
        }
        if (readTimeoutMillis <= 0) {
            readTimeoutMillis = DEFAULT_READ_TIMEOUT_MILLIS;
        }
    }

    public MinioProviderProperties(
            final String endpoint,
            final String accessKey,
            final String secretKey,
            final String bucket
    ) {
        this(endpoint, accessKey, secretKey, bucket, 0, 0, 0);
    }
}
