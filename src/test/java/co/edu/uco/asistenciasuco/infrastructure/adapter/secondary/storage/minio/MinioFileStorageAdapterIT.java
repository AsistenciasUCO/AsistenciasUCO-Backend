package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.storage.minio;

import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ChecksumCalculator;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.CompressionDecision;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.CompressionPolicy;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.testsupport.files.FilesIntegrationEnvironment;
import co.edu.uco.asistenciasuco.testsupport.files.SoporteFixtures;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RED causal LB-004B.2: STORAGE-004/005/006, INTEGRITY-001
 * (docs/work-items/LB-004-stateless-serverless-readiness/TEST_PLAN.md).
 *
 * <p>Certifica el borde REAL {@code MinioFileStorageAdapter -> MinIO} contra el servicio local
 * provisto por {@code infra/files/compose.yaml} (servicio {@code minio}, bucket
 * {@code asistencias-soportes}). No mockea el SDK: dos clientes MinIO independientes se usan
 * deliberadamente para demostrar STATELESS-001/STORAGE-006 (dos "instancias" leen el mismo
 * objeto). Requiere el stack de {@code infra/files/compose.yaml} corriendo y las variables
 * {@code MINIO_ENDPOINT}/{@code MINIO_ACCESS_KEY}/{@code MINIO_SECRET_KEY} (sin valores por defecto,
 * ver {@link FilesIntegrationEnvironment}); sin ellas esta prueba FALLA, nunca se omite.</p>
 */
@Tag("integration")
class MinioFileStorageAdapterIT {

    private final List<String> createdFileIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        final FileStoragePort adapter = newAdapter();
        for (final String fileId : createdFileIds) {
            try {
                adapter.delete(fileId);
            } catch (final RuntimeException ignored) {
                // best-effort: el objeto pudo no haberse llegado a crear en un test fallido
            }
        }
        createdFileIds.clear();
    }

    // STORAGE-004
    @Test
    void store_y_read_recuperan_los_mismos_bytes_y_metadata() {
        final FileStoragePort adapter = newAdapter();
        final String fileId = newTrackedFileId();
        final byte[] content = "%PDF-1.4 contenido de prueba MinioFileStorageAdapterIT".getBytes();
        final FileStoragePort.StoredObjectMetadata metadata = metadataFor(content, "soporte.pdf");

        adapter.store(new FileStoragePort.StoreObjectCommand(fileId, content, metadata));
        final FileStoragePort.StoredObject read = adapter.read(fileId);

        assertArrayEquals(content, read.content());
        assertEquals(metadata.ownerSubject(), read.metadata().ownerSubject());
        assertEquals(metadata.originalFilename(), read.metadata().originalFilename());
        assertEquals(metadata.contentType(), read.metadata().contentType());
        // INTEGRITY-001: checksum sobrevive el round-trip de metadata tecnica del objeto.
        assertEquals(metadata.checksumSha256(), read.metadata().checksumSha256());
    }

    @Test
    void exists_refleja_presencia_real_del_objeto() {
        final FileStoragePort adapter = newAdapter();
        final String fileId = newTrackedFileId();
        assertFalse(adapter.exists(fileId));

        adapter.store(new FileStoragePort.StoreObjectCommand(fileId, "contenido".getBytes(), metadataFor("contenido".getBytes(), "x.pdf")));

        assertTrue(adapter.exists(fileId));
    }

    @Test
    void read_de_fileId_inexistente_lanza_object_not_found() {
        final FileStoragePort adapter = newAdapter();

        assertThrows(FileStoragePort.ObjectNotFoundException.class, () -> adapter.read(UUID.randomUUID().toString()));
    }

    @Test
    void delete_elimina_el_objeto_y_exists_pasa_a_false() {
        final FileStoragePort adapter = newAdapter();
        final String fileId = newTrackedFileId();
        adapter.store(new FileStoragePort.StoreObjectCommand(fileId, "contenido".getBytes(), metadataFor("contenido".getBytes(), "x.pdf")));

        adapter.delete(fileId);

        assertFalse(adapter.exists(fileId));
    }

    // STORAGE-005: restart simulado — un adapter/cliente NUEVO (nueva "instancia") lee el objeto
    // creado por el adapter original.
    @Test
    void un_nuevo_adapter_recupera_el_objeto_tras_un_restart_simulado() {
        final String fileId = newTrackedFileId();
        final byte[] content = "contenido persistente".getBytes();
        newAdapter().store(new FileStoragePort.StoreObjectCommand(fileId, content, metadataFor(content, "x.pdf")));

        final FileStoragePort adapterTrasRestart = newAdapter();
        final FileStoragePort.StoredObject read = adapterTrasRestart.read(fileId);

        assertArrayEquals(content, read.content());
    }

    // STORAGE-006 / STATELESS-001: dos adapters/clientes independientes contra el mismo MinIO;
    // A escribe, B lee el mismo objeto.
    @Test
    void dos_adapters_independientes_contra_el_mismo_minio_leen_el_mismo_objeto() {
        final FileStoragePort adapterA = newAdapter();
        final FileStoragePort adapterB = newAdapter();
        final String fileId = newTrackedFileId();
        final byte[] content = "bytes escritos por instancia A".getBytes();

        adapterA.store(new FileStoragePort.StoreObjectCommand(fileId, content, metadataFor(content, "x.pdf")));
        final FileStoragePort.StoredObject readByB = adapterB.read(fileId);

        assertArrayEquals(content, readByB.content());
    }

    // INTEGRITY-001 / metadata tecnica completa: tamanos, compresion, checksum y uploadedAt
    // sobreviven el round-trip real, incluido un objeto almacenado comprimido.
    @Test
    void metadata_tecnica_de_objeto_comprimido_sobrevive_el_round_trip_real() {
        final FileStoragePort adapter = newAdapter();
        final String fileId = newTrackedFileId();
        final byte[] original = SoporteFixtures.compressiblePdf(64 * 1024);
        final CompressionDecision decision = CompressionPolicy.evaluate(original);
        assertTrue(decision.compressed(), "el fixture compresible debe superar el umbral de ahorro");
        final Instant uploadedAt = Instant.parse("2026-10-18T15:30:45.123Z");
        final FileStoragePort.StoredObjectMetadata metadata = new FileStoragePort.StoredObjectMetadata(
                UUID.randomUUID().toString(), "soporte.pdf", "application/pdf",
                original.length, decision.storedContent().length, true, decision.algorithm(),
                ChecksumCalculator.sha256(original), uploadedAt);

        adapter.store(new FileStoragePort.StoreObjectCommand(fileId, decision.storedContent(), metadata));
        final FileStoragePort.StoredObject read = adapter.read(fileId);
        final byte[] restored = CompressionPolicy.decompress(read.content(), read.metadata().compressionAlgorithm());

        assertArrayEquals(decision.storedContent(), read.content());
        assertEquals(original.length, read.metadata().originalSize());
        assertEquals(decision.storedContent().length, read.metadata().storedSize());
        assertTrue(read.metadata().compressed());
        assertEquals(CompressionPolicy.ALGORITHM_DEFLATE, read.metadata().compressionAlgorithm());
        assertEquals(uploadedAt, read.metadata().uploadedAt());
        assertArrayEquals(original, restored);
        assertEquals(metadata.checksumSha256(), ChecksumCalculator.sha256(restored));
    }

    @Test
    void objeto_sin_comprimir_conserva_tamano_y_marca_de_no_compresion() {
        final FileStoragePort adapter = newAdapter();
        final String fileId = newTrackedFileId();
        final byte[] content = SoporteFixtures.png(32 * 1024, 7L);

        adapter.store(new FileStoragePort.StoreObjectCommand(fileId, content, metadataFor(content, "foto.png")));
        final FileStoragePort.StoredObject read = adapter.read(fileId);

        assertArrayEquals(content, read.content());
        assertEquals(content.length, read.metadata().originalSize());
        assertEquals(content.length, read.metadata().storedSize());
        assertFalse(read.metadata().compressed());
        assertNull(read.metadata().compressionAlgorithm());
    }

    // MINIO-AUTH: credenciales invalidas son un fallo tecnico, nunca "no existe" ni fuga del secreto.
    @Test
    void credenciales_invalidas_fallan_como_storage_no_disponible_sin_exponer_el_secreto() {
        final String wrongSecret = "wrong-secret-" + UUID.randomUUID();
        final FileStoragePort adapter = adapterWith(FilesIntegrationEnvironment.minioEndpoint(),
                FilesIntegrationEnvironment.minioAccessKey(), wrongSecret);

        final FileStoragePort.StorageUnavailableException exception = assertThrows(
                FileStoragePort.StorageUnavailableException.class,
                () -> adapter.read(UUID.randomUUID().toString()));

        assertFalse(exception.getMessage().contains(wrongSecret));
        assertFalse(exception.getMessage().contains(FilesIntegrationEnvironment.minioAccessKey()));
    }

    // MINIO-DOWN: endpoint sin servicio -> fallo tecnico controlado, nunca ObjectNotFound.
    @Test
    void endpoint_sin_servicio_falla_como_storage_no_disponible() {
        final FileStoragePort adapter = adapterWith("http://127.0.0.1:1",
                FilesIntegrationEnvironment.minioAccessKey(), FilesIntegrationEnvironment.minioSecretKey());

        assertThrows(FileStoragePort.StorageUnavailableException.class, () -> adapter.read(UUID.randomUUID().toString()));
        assertThrows(FileStoragePort.StorageUnavailableException.class, () -> adapter.exists(UUID.randomUUID().toString()));
    }

    private String newTrackedFileId() {
        final String fileId = UUID.randomUUID().toString();
        createdFileIds.add(fileId);
        return fileId;
    }

    private FileStoragePort.StoredObjectMetadata metadataFor(final byte[] content, final String filename) {
        return new FileStoragePort.StoredObjectMetadata(
                UUID.randomUUID().toString(),
                filename,
                "application/pdf",
                content.length,
                content.length,
                false,
                null,
                ChecksumCalculator.sha256(content),
                Instant.now()
        );
    }

    private static FileStoragePort newAdapter() {
        return adapterWith(FilesIntegrationEnvironment.minioEndpoint(),
                FilesIntegrationEnvironment.minioAccessKey(), FilesIntegrationEnvironment.minioSecretKey());
    }

    private static FileStoragePort adapterWith(final String endpoint, final String accessKey, final String secretKey) {
        final MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        client.setTimeout(2000, 5000, 5000);
        return new MinioFileStorageAdapter(client, FilesIntegrationEnvironment.minioBucket());
    }
}


