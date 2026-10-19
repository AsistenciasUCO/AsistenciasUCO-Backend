package co.edu.uco.asistenciasuco.infrastructure.config.adapters;

import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.storage.minio.MinioStorageAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.MinioProviderProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * LB-004 MINIO-RUNTIME-TIMEOUT: un MinIO que acepta la conexion TCP pero nunca responde no debe
 * bloquear la request durante el timeout por defecto del SDK (5 minutos). La Composition Root
 * aplica los timeouts configurados y el adapter falla cerrado con
 * {@link FileStoragePort.StorageUnavailableException}.
 *
 * <p>Servidor TCP local "agujero negro": no imita S3 ni acredita un MinIO real
 * (ver {@code MinioFileStorageAdapterIT} para el provider real).</p>
 */
class MinioClientTimeoutBoundaryTest {

    private static final int READ_TIMEOUT_MILLIS = 300;

    private ServerSocket blackHole;
    private ExecutorService acceptor;
    private final List<Socket> heldConnections = new CopyOnWriteArrayList<>();

    @BeforeEach
    void startBlackHole() throws IOException {
        blackHole = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        acceptor = Executors.newSingleThreadExecutor();
        acceptor.submit(() -> {
            while (!blackHole.isClosed()) {
                try {
                    // Acepta y retiene la conexion sin leer ni responder nunca.
                    heldConnections.add(blackHole.accept());
                } catch (final IOException closed) {
                    return;
                }
            }
        });
    }

    @AfterEach
    void stopBlackHole() throws IOException {
        blackHole.close();
        for (final Socket socket : heldConnections) {
            socket.close();
        }
        acceptor.shutdownNow();
    }

    @Test
    void minio_que_no_responde_falla_cerrado_dentro_del_timeout_configurado_al_leer() {
        final FileStoragePort port = portAgainstBlackHole();

        assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                assertThrows(FileStoragePort.StorageUnavailableException.class,
                        () -> port.read(UUID.randomUUID().toString())));
    }

    @Test
    void minio_que_no_responde_falla_cerrado_dentro_del_timeout_configurado_al_almacenar() {
        final FileStoragePort port = portAgainstBlackHole();
        final byte[] content = "%PDF-timeout".getBytes();
        final FileStoragePort.StoredObjectMetadata metadata = new FileStoragePort.StoredObjectMetadata(
                UUID.randomUUID().toString(), "x.pdf", "application/pdf", content.length, content.length,
                false, null, "0".repeat(64), java.time.Instant.now());

        assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                assertThrows(FileStoragePort.StorageUnavailableException.class,
                        () -> port.store(new FileStoragePort.StoreObjectCommand(
                                UUID.randomUUID().toString(), content, metadata))));
    }

    private FileStoragePort portAgainstBlackHole() {
        final MinioProviderProperties properties = new MinioProviderProperties(
                "http://127.0.0.1:" + blackHole.getLocalPort(),
                "timeout-test-access",
                "timeout-test-secret-value",
                "asistencias-soportes",
                READ_TIMEOUT_MILLIS,
                READ_TIMEOUT_MILLIS,
                READ_TIMEOUT_MILLIS);
        return new MinioStorageAdapterConfiguration().fileStoragePort(properties);
    }
}
