package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.archivo;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ChecksumCalculator;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.malwarescan.clamav.ClamAvMalwareScanAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.adapters.storage.minio.MinioStorageAdapterConfiguration;
import co.edu.uco.asistenciasuco.infrastructure.config.wiring.ArchivoWiringConfiguration;
import co.edu.uco.asistenciasuco.testsupport.files.SoporteFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RED causal LB-004B.2 / LB-004 MinIO E2E: flujo backend real, sin frontend
 * (docs/work-items/LB-004-stateless-serverless-readiness/E2E_PLAN.md §FASE 1,
 * spec §19 "Integration tests reales").
 *
 * <pre>
 * ArchivoController.subirArchivo
 *   -&gt; validacion de contenido (ContentSecurityValidator)
 *   -&gt; ClamAV real (ClamAvMalwareScanAdapter)
 *   -&gt; politica de compresion
 *   -&gt; MinIO real (MinioFileStorageAdapter)
 *   -&gt; ArchivoController.descargarArchivo (ownership + checksum + inflate)
 *   -&gt; mismos bytes originales
 * </pre>
 *
 * <p>Contexto Spring minimo: Composition Root de storage/malware-scan + wiring de Application +
 * controller real. La identidad se inyecta con un {@link AuthenticatedUserResolver} de prueba: este
 * IT NO certifica Bearer/JWT (eso lo hace el E2E HTTP con Keycloak, ver
 * {@code scripts/e2e/files/files_storage_e2e.py}). No requiere SQL Server: la metadata durable (SQL) se simula con un
 * {@link SoporteArchivoMetadataPort} en memoria; el contrato real contra SQL lo cubre {@code SoporteArchivoSqlIT}.
 * Requiere
 * {@code infra/files/compose.yaml} y las variables {@code MINIO_ENDPOINT}, {@code MINIO_ACCESS_KEY},
 * {@code MINIO_SECRET_KEY}, {@code CLAMAV_HOST}, {@code CLAMAV_PORT} sin valores por defecto.</p>
 */
@Tag("integration")
@SpringBootTest(
        classes = {
                MinioStorageAdapterConfiguration.class,
                ClamAvMalwareScanAdapterConfiguration.class,
                ArchivoWiringConfiguration.class,
                ArchivoController.class,
                ArchivoUploadDownloadFlowIT.TestSupportConfig.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "app.adapters.storage.provider=minio",
        "app.providers.minio.endpoint=${MINIO_ENDPOINT}",
        "app.providers.minio.access-key=${MINIO_ACCESS_KEY}",
        "app.providers.minio.secret-key=${MINIO_SECRET_KEY}",
        "app.providers.minio.bucket=${MINIO_BUCKET:asistencias-soportes}",
        "app.providers.minio.connect-timeout-millis=2000",
        "app.providers.minio.write-timeout-millis=10000",
        "app.providers.minio.read-timeout-millis=10000",
        "app.providers.clamav.host=${CLAMAV_HOST}",
        "app.providers.clamav.port=${CLAMAV_PORT}",
        "app.providers.clamav.connect-timeout-millis=5000",
        "app.providers.clamav.read-timeout-millis=15000"
})
class ArchivoUploadDownloadFlowIT {

    private static final Set<String> UPLOAD_PUBLIC_FIELDS = Set.of("fileId", "nombre", "url", "tamanio");

    @Autowired
    private ArchivoController archivoController;

    @Autowired
    private FileStoragePort fileStoragePort;

    @Autowired
    private AtomicReference<UUID> currentUser;

    private final List<UUID> createdFileIds = new ArrayList<>();

    @BeforeEach
    void authenticateAsOwner() {
        currentUser.set(UUID.randomUUID());
    }

    /** Limpieza mediante la capability interna {@code delete}; no existe DELETE HTTP publico. */
    @AfterEach
    void cleanUp() {
        for (final UUID fileId : createdFileIds) {
            try {
                fileStoragePort.delete(fileId.toString());
            } catch (final RuntimeException ignored) {
                // best-effort
            }
        }
        createdFileIds.clear();
    }

    @Test
    void flujo_completo_upload_validacion_clamav_minio_download_devuelve_los_mismos_bytes() {
        final byte[] originalContent = withPdfMagic("HU014 soporte de prueba E2E LB-004B.2");
        final UUID fileId = upload("soporte.pdf", "application/pdf", originalContent);

        final ResponseEntity<byte[]> downloadResponse = archivoController.descargarArchivo(fileId.toString());

        assertEquals(HttpStatus.OK, downloadResponse.getStatusCode());
        assertArrayEquals(originalContent, downloadResponse.getBody());
    }

    // Caso negativo: fileId inexistente en MinIO real -> 404, no revela existencia.
    @Test
    void descarga_de_fileId_inexistente_retorna_resource_not_found() {
        assertThrows(ResourceNotFoundException.class,
                () -> archivoController.descargarArchivo(UUID.randomUUID().toString()));
    }

    // MALWARE-001 en el flujo completo (cierra R-LB004-025): el PDF valido con EICAR embebido pasa
    // la validacion de contenido, el ClamAV real lo detecta y la subida se rechaza como 4xx.
    @Test
    void pdf_valido_con_eicar_embebido_es_rechazado_por_el_clamav_real() {
        final MockMultipartFile infectado = new MockMultipartFile(
                "archivo", "soporte.pdf", "application/pdf", SoporteFixtures.pdfWithEmbeddedEicar());

        final ValidationException rejected = assertThrows(ValidationException.class,
                () -> archivoController.subirArchivo(infectado));

        assertTrue(rejected.getMessage().contains("malware"));
    }

    // COMPRESSION-002 real: el objeto queda comprimido en MinIO y la descarga es transparente.
    @Test
    void pdf_compresible_se_almacena_comprimido_en_minio_y_se_descarga_identico() {
        final byte[] original = SoporteFixtures.compressiblePdf(256 * 1024);
        final UUID fileId = upload("compresible.pdf", "application/pdf", original);

        final FileStoragePort.StoredObject stored = fileStoragePort.read(fileId.toString());
        assertTrue(stored.metadata().compressed());
        assertTrue(stored.metadata().storedSize() < original.length);
        assertEquals(original.length, stored.metadata().originalSize());
        assertEquals(ChecksumCalculator.sha256(original), stored.metadata().checksumSha256());

        final ResponseEntity<byte[]> download = archivoController.descargarArchivo(fileId.toString());
        assertArrayEquals(original, download.getBody());
        assertEquals(ChecksumCalculator.sha256(original), ChecksumCalculator.sha256(download.getBody()));
    }

    // COMPRESSION-001 real + tipos permitidos: PNG/JPEG no compresibles quedan sin comprimir.
    @Test
    void png_y_jpeg_se_almacenan_sin_comprimir_y_se_descargan_con_su_tipo_verificado() {
        final byte[] png = SoporteFixtures.png(48 * 1024, 21L);
        final byte[] jpeg = SoporteFixtures.jpeg(48 * 1024, 22L);
        final UUID pngId = upload("foto.png", "image/png", png);
        final UUID jpegId = upload("foto.jpeg", "image/jpeg", jpeg);

        assertFalse(fileStoragePort.read(pngId.toString()).metadata().compressed());
        assertFalse(fileStoragePort.read(jpegId.toString()).metadata().compressed());

        final ResponseEntity<byte[]> pngDownload = archivoController.descargarArchivo(pngId.toString());
        final ResponseEntity<byte[]> jpegDownload = archivoController.descargarArchivo(jpegId.toString());
        assertArrayEquals(png, pngDownload.getBody());
        assertEquals(MediaType.IMAGE_PNG, pngDownload.getHeaders().getContentType());
        assertArrayEquals(jpeg, jpegDownload.getBody());
        assertEquals(MediaType.IMAGE_JPEG, jpegDownload.getHeaders().getContentType());
        assertEquals("inline; filename=\"foto.jpeg\"",
                jpegDownload.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
    }

    // CONTENT-005 limite exacto: 5 MiB atraviesan ClamAV, MinIO y el presupuesto de lectura.
    @Test
    void soporte_de_5_mib_exactos_completa_el_flujo_real() {
        final byte[] max = SoporteFixtures.incompressiblePdfOfExactSize(SoporteFixtures.MAX_FILE_SIZE_BYTES, 31L);
        final UUID fileId = upload("maximo.pdf", "application/pdf", max);

        final ResponseEntity<byte[]> download = archivoController.descargarArchivo(fileId.toString());

        assertArrayEquals(max, download.getBody());
    }

    @Test
    void soporte_de_5_mib_mas_un_byte_se_rechaza_antes_de_contactar_proveedores() {
        final byte[] oversized = SoporteFixtures.incompressiblePdfOfExactSize(SoporteFixtures.MAX_FILE_SIZE_BYTES + 1, 32L);

        assertThrows(ValidationException.class, () -> archivoController.subirArchivo(
                new MockMultipartFile("archivo", "grande.pdf", "application/pdf", oversized)));
    }

    // FOREIGN_FILE_HTTP 404: otro usuario autenticado no descarga ni distingue el archivo ajeno
    // de uno inexistente; el propietario conserva el acceso.
    @Test
    void otro_usuario_autenticado_recibe_not_found_indistinguible_de_inexistente() {
        final byte[] content = withPdfMagic("soporte privado del propietario");
        final UUID fileId = upload("privado.pdf", "application/pdf", content);
        final UUID owner = currentUser.get();

        currentUser.set(UUID.randomUUID());
        final ResourceNotFoundException foreign = assertThrows(ResourceNotFoundException.class,
                () -> archivoController.descargarArchivo(fileId.toString()));
        final ResourceNotFoundException missing = assertThrows(ResourceNotFoundException.class,
                () -> archivoController.descargarArchivo(UUID.randomUUID().toString()));
        assertEquals(missing.getMessage(), foreign.getMessage());

        currentUser.set(owner);
        assertArrayEquals(content, archivoController.descargarArchivo(fileId.toString()).getBody());
    }

    // HTTP_CONTRACT_TARGET: la respuesta publica solo expone fileId, nombre, url relativa y tamanio.
    @Test
    void respuesta_de_subida_no_expone_bucket_object_key_ni_endpoint_del_provider() {
        final byte[] content = withPdfMagic("contrato publico de subida");
        final ResponseEntity<ApiDataResponse<Map<String, Object>>> response = archivoController.subirArchivo(
                new MockMultipartFile("archivo", "mi soporte.pdf", "application/pdf", content));
        final Map<String, Object> data = response.getBody().datos();
        final UUID fileId = (UUID) data.get("fileId");
        createdFileIds.add(fileId);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(UPLOAD_PUBLIC_FIELDS, data.keySet());
        assertEquals("/api/v1/archivos/" + fileId, data.get("url"));
        assertEquals("mi_soporte.pdf", data.get("nombre"));
        assertEquals((long) content.length, ((Number) data.get("tamanio")).longValue());
        final String serialized = data.toString();
        assertFalse(serialized.contains("soportes/"));
        assertFalse(serialized.contains("asistencias-soportes"));
        assertFalse(serialized.contains(System.getenv("MINIO_ENDPOINT")));
    }

    private UUID upload(final String filename, final String contentType, final byte[] content) {
        final ResponseEntity<ApiDataResponse<Map<String, Object>>> response = archivoController.subirArchivo(
                new MockMultipartFile("archivo", filename, contentType, content));
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        final UUID fileId = (UUID) response.getBody().datos().get("fileId");
        createdFileIds.add(fileId);
        return fileId;
    }

    private static byte[] withPdfMagic(final String payload) {
        final byte[] magic = {0x25, 0x50, 0x44, 0x46, 0x2D};
        final byte[] payloadBytes = payload.getBytes();
        final byte[] content = new byte[magic.length + payloadBytes.length];
        System.arraycopy(magic, 0, content, 0, magic.length);
        System.arraycopy(payloadBytes, 0, content, magic.length, payloadBytes.length);
        return content;
    }

    @TestConfiguration
    static class TestSupportConfig {

        @Bean
        AtomicReference<UUID> currentUser() {
            return new AtomicReference<>(UUID.randomUUID());
        }

        @Bean
        AuthenticatedUserResolver authenticatedUserResolver(final AtomicReference<UUID> currentUser) {
            return currentUser::get;
        }

        /** Sustituto de SQL: registra el borrador y autoriza solo al propietario (el docente titular lo cubre SoporteArchivoSqlIT). */
        @Bean
        SoporteArchivoMetadataPort soporteArchivoMetadataPort() {
            final Map<UUID, SoporteArchivoMetadataPort.RegistrarSoporteBorrador> registros = new ConcurrentHashMap<>();
            return new SoporteArchivoMetadataPort() {
                @Override
                public void registrarBorrador(final RegistrarSoporteBorrador comando) {
                    registros.putIfAbsent(comando.fileId(), comando);
                }

                @Override
                public Optional<SoporteArchivoAutorizado> consultarAutorizado(final UUID fileId, final UUID usuarioEjecutor) {
                    final RegistrarSoporteBorrador registro = registros.get(fileId);
                    if (registro == null || !registro.usuarioPropietario().equals(usuarioEjecutor)) {
                        return Optional.empty();
                    }
                    return Optional.of(new SoporteArchivoAutorizado(registro.fileId(), EstadoSoporte.DRAFT,
                            registro.nombreOriginal(), registro.tipoContenido(), registro.tamanioBytes(),
                            registro.checksumSha256(), Instant.now(), null, null, RelacionAcceso.PROPIETARIO));
                }
            };
        }
    }
}
