package co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.domain.DescargarArchivoDomain;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.entity.DescargarArchivoResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ChecksumCalculator;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.CompressionDecision;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.CompressionPolicy;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort.RelacionAcceso;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort.SoporteArchivoAutorizado;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * LB-004D: SQL es la autoridad de acceso. La autorizacion ({@link SoporteArchivoMetadataPort#consultarAutorizado})
 * precede a toda lectura del storage; sin fila autorizada hay 404 (nunca 403) y el storage no se consulta. El
 * checksum y la longitud de SQL son la autoridad de integridad.
 */
class DescargarArchivoUseCaseImplTest {

    private static final byte[] PDF = "%PDF- contenido pdf".getBytes();

    private final FileStoragePort storage = mock(FileStoragePort.class);
    private final SoporteArchivoMetadataPort metadata = mock(SoporteArchivoMetadataPort.class);
    private final DescargarArchivoUseCaseImpl useCase = new DescargarArchivoUseCaseImpl(storage, metadata);

    @Test
    void dominio_nulo_es_rechazado() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void propietario_recupera_bytes_originales_con_el_tipo_y_nombre_de_sql() {
        final UUID fileId = UUID.randomUUID();
        final UUID owner = UUID.randomUUID();
        autorizar(fileId, owner, RelacionAcceso.PROPIETARIO, PDF);
        when(storage.read(fileId.toString())).thenReturn(stored(fileId, PDF, false, null));

        final DescargarArchivoResultadoEntity resultado = useCase.execute(new DescargarArchivoDomain(fileId, owner));

        assertArrayEquals(PDF, resultado.content());
        assertEquals("application/pdf", resultado.contentType());
        assertEquals("soporte.pdf", resultado.filename());
    }

    // El docente titular no es el propietario del objeto: el acceso lo concede SQL, no el owner-subject del storage.
    @Test
    void docente_titular_autorizado_por_sql_recupera_los_bytes() {
        final UUID fileId = UUID.randomUUID();
        final UUID teacher = UUID.randomUUID();
        autorizar(fileId, teacher, RelacionAcceso.DOCENTE_TITULAR, PDF);
        when(storage.read(fileId.toString())).thenReturn(stored(fileId, PDF, false, null));

        final DescargarArchivoResultadoEntity resultado = useCase.execute(new DescargarArchivoDomain(fileId, teacher));

        assertArrayEquals(PDF, resultado.content());
    }

    @Test
    void la_autorizacion_en_sql_ocurre_antes_de_leer_el_storage() {
        final UUID fileId = UUID.randomUUID();
        final UUID owner = UUID.randomUUID();
        autorizar(fileId, owner, RelacionAcceso.PROPIETARIO, PDF);
        when(storage.read(fileId.toString())).thenReturn(stored(fileId, PDF, false, null));

        useCase.execute(new DescargarArchivoDomain(fileId, owner));

        final InOrder order = inOrder(metadata, storage);
        order.verify(metadata).consultarAutorizado(fileId, owner);
        order.verify(storage).read(fileId.toString());
    }

    // SEC: otro estudiante, docente ajeno, administrador, coordinador, decano o archivo inexistente => 404 y sin storage.
    @Test
    void sin_fila_autorizada_responde_404_y_no_consulta_el_storage() {
        final UUID fileId = UUID.randomUUID();
        final UUID actor = UUID.randomUUID();
        when(metadata.consultarAutorizado(fileId, actor)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(new DescargarArchivoDomain(fileId, actor)));

        verifyNoInteractions(storage);
    }

    @Test
    void identificadores_ausentes_no_consultan_ni_sql_ni_storage() {
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(new DescargarArchivoDomain(null, UUID.randomUUID())));
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(new DescargarArchivoDomain(UUID.randomUUID(), null)));
        verifyNoInteractions(storage, metadata);
    }

    @Test
    void sql_no_disponible_falla_cerrado_y_no_lee_el_storage() {
        final UUID fileId = UUID.randomUUID();
        final UUID actor = UUID.randomUUID();
        when(metadata.consultarAutorizado(fileId, actor)).thenThrow(
                new SoporteArchivoMetadataPort.SoporteMetadataNoDisponibleException("sql down", new RuntimeException()));

        assertThrows(InternalApplicationException.class, () -> useCase.execute(new DescargarArchivoDomain(fileId, actor)));

        verifyNoInteractions(storage);
    }

    @Test
    void fila_autorizada_sin_objeto_en_storage_es_un_incidente_de_integridad_no_un_404() {
        final UUID fileId = UUID.randomUUID();
        final UUID owner = UUID.randomUUID();
        autorizar(fileId, owner, RelacionAcceso.PROPIETARIO, PDF);
        when(storage.read(anyString())).thenThrow(new FileStoragePort.ObjectNotFoundException(fileId.toString()));

        assertThrows(InternalApplicationException.class, () -> useCase.execute(new DescargarArchivoDomain(fileId, owner)));
    }

    @Test
    void storage_no_disponible_no_se_disfraza_de_archivo_inexistente() {
        final UUID fileId = UUID.randomUUID();
        final UUID owner = UUID.randomUUID();
        autorizar(fileId, owner, RelacionAcceso.PROPIETARIO, PDF);
        final FileStoragePort.StorageUnavailableException unavailable =
                new FileStoragePort.StorageUnavailableException("minio down", new RuntimeException());
        when(storage.read(anyString())).thenThrow(unavailable);

        final FileStoragePort.StorageUnavailableException thrown = assertThrows(
                FileStoragePort.StorageUnavailableException.class,
                () -> useCase.execute(new DescargarArchivoDomain(fileId, owner)));
        assertEquals(unavailable, thrown);
    }

    @Test
    void contenido_comprimido_se_descomprime_de_forma_transparente() {
        final UUID fileId = UUID.randomUUID();
        final UUID owner = UUID.randomUUID();
        final byte[] original = "%PDF- contenido original repetible repetible repetible repetible repetible".getBytes();
        final CompressionDecision compressed = CompressionPolicy.evaluate(original);
        autorizar(fileId, owner, RelacionAcceso.PROPIETARIO, original);
        when(storage.read(fileId.toString()))
                .thenReturn(stored(fileId, compressed.storedContent(), compressed.compressed(), compressed.algorithm()));

        final DescargarArchivoResultadoEntity resultado = useCase.execute(new DescargarArchivoDomain(fileId, owner));

        assertArrayEquals(original, resultado.content());
    }

    // INTEGRITY: el SHA-256 de SQL manda; los bytes alterados en el storage nunca se sirven.
    @Test
    void checksum_distinto_al_de_sql_falla_cerrado_sin_servir_bytes() {
        final UUID fileId = UUID.randomUUID();
        final UUID owner = UUID.randomUUID();
        autorizar(fileId, owner, RelacionAcceso.PROPIETARIO, PDF);
        final byte[] alterado = "%PDF- contenido alterad".getBytes();
        when(storage.read(fileId.toString())).thenReturn(stored(fileId, alterado, false, null));

        assertThrows(InternalApplicationException.class, () -> useCase.execute(new DescargarArchivoDomain(fileId, owner)));
    }

    @Test
    void longitud_distinta_a_la_de_sql_falla_cerrado() {
        final UUID fileId = UUID.randomUUID();
        final UUID owner = UUID.randomUUID();
        when(metadata.consultarAutorizado(fileId, owner)).thenReturn(Optional.of(new SoporteArchivoAutorizado(
                fileId, SoporteArchivoMetadataPort.EstadoSoporte.ATTACHED, "soporte.pdf", "application/pdf",
                PDF.length + 1L, ChecksumCalculator.sha256(PDF), Instant.now(), Instant.now(), UUID.randomUUID(),
                RelacionAcceso.PROPIETARIO)));
        when(storage.read(fileId.toString())).thenReturn(stored(fileId, PDF, false, null));

        assertThrows(InternalApplicationException.class, () -> useCase.execute(new DescargarArchivoDomain(fileId, owner)));
    }

    @Test
    void contenido_comprimido_corrupto_falla_cerrado() {
        final UUID fileId = UUID.randomUUID();
        final UUID owner = UUID.randomUUID();
        autorizar(fileId, owner, RelacionAcceso.PROPIETARIO, PDF);
        when(storage.read(fileId.toString())).thenReturn(stored(fileId, new byte[]{1, 2, 3, 4}, true, "DEFLATE"));

        assertThrows(InternalApplicationException.class, () -> useCase.execute(new DescargarArchivoDomain(fileId, owner)));
    }

    @Test
    void el_owner_subject_del_objeto_ya_no_concede_ni_deniega_el_acceso() {
        final UUID fileId = UUID.randomUUID();
        final UUID teacher = UUID.randomUUID();
        autorizar(fileId, teacher, RelacionAcceso.DOCENTE_TITULAR, PDF);
        // owner-subject de MinIO = otro usuario (el estudiante): SQL autoriza al docente igualmente.
        when(storage.read(fileId.toString())).thenReturn(stored(fileId, PDF, false, null));

        assertArrayEquals(PDF, useCase.execute(new DescargarArchivoDomain(fileId, teacher)).content());
        verify(metadata).consultarAutorizado(fileId, teacher);
    }

    private void autorizar(final UUID fileId, final UUID actor, final RelacionAcceso relacion, final byte[] original) {
        when(metadata.consultarAutorizado(fileId, actor)).thenReturn(Optional.of(new SoporteArchivoAutorizado(
                fileId, SoporteArchivoMetadataPort.EstadoSoporte.ATTACHED, "soporte.pdf", "application/pdf",
                original.length, ChecksumCalculator.sha256(original), Instant.parse("2026-10-19T15:00:00Z"),
                Instant.parse("2026-10-19T15:30:45.1234567Z"), UUID.randomUUID(), relacion)));
    }

    private static FileStoragePort.StoredObject stored(
            final UUID fileId, final byte[] content, final boolean compressed, final String algorithm) {
        return new FileStoragePort.StoredObject(fileId.toString(), content, new FileStoragePort.StoredObjectMetadata(
                UUID.randomUUID().toString(), "otro-nombre.pdf", "application/octet-stream", content.length,
                content.length, compressed, algorithm, "0".repeat(64), Instant.parse("2026-10-08T12:00:00Z")));
    }
}
