package co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.domain.SubirArchivoDomain;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.entity.SubirArchivoResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.malwarescan.MalwareScanPort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RED causal LB-004B.2: STORAGE-002/003, MALWARE-001/002/003 (contrato en Application, con
 * MalwareScanPort/FileStoragePort simulados; el escaneo real contra ClamAV vive en
 * ClamAvMalwareScanAdapterIT).
 */
class SubirArchivoUseCaseImplTest {

    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46, 0x2D};

    private final FileStoragePort fileStoragePort = mock(FileStoragePort.class);
    private final MalwareScanPort malwareScanPort = mock(MalwareScanPort.class);
    private final SoporteArchivoMetadataPort metadataPort = mock(SoporteArchivoMetadataPort.class);
    private final SubirArchivoUseCaseImpl useCase = new SubirArchivoUseCaseImpl(fileStoragePort, malwareScanPort, metadataPort);

    @Test
    void dominio_nulo_es_rechazado() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
        verify(fileStoragePort, never()).store(any());
    }

    // STORAGE-003: fileId UUID generado por backend, independiente del filename.
    @Test
    void subida_valida_genera_fileId_independiente_del_filename_y_almacena() {
        when(malwareScanPort.scan(any())).thenReturn(MalwareScanPort.ScanResult.clean());

        final SubirArchivoResultadoEntity resultado = useCase.execute(dominioValido("soporte medico.pdf"));

        assertNotNull(resultado.fileId());
        assertTrue(resultado.url().endsWith(resultado.fileId().toString()));
        assertEquals("soporte_medico.pdf", resultado.nombre());

        final ArgumentCaptor<FileStoragePort.StoreObjectCommand> captor =
                ArgumentCaptor.forClass(FileStoragePort.StoreObjectCommand.class);
        verify(fileStoragePort).store(captor.capture());
        assertEquals(resultado.fileId().toString(), captor.getValue().fileId());
    }

    // MALWARE-001 (a nivel de orquestacion): infectado -> rechazado, nunca almacenado.
    @Test
    void archivo_infectado_es_rechazado_y_nunca_se_almacena() {
        when(malwareScanPort.scan(any())).thenReturn(MalwareScanPort.ScanResult.infected("Eicar-Test-Signature"));

        assertThrows(ValidationException.class, () -> useCase.execute(dominioValido("soporte.pdf")));

        verify(fileStoragePort, never()).store(any());
    }

    // MALWARE-002
    @Test
    void archivo_limpio_continua_el_flujo_y_se_almacena() {
        when(malwareScanPort.scan(any())).thenReturn(MalwareScanPort.ScanResult.clean());

        useCase.execute(dominioValido("soporte.pdf"));

        verify(fileStoragePort).store(any());
    }

    // MALWARE-003: error del scanner -> FAIL CLOSED, nunca CLEAN por defecto, nunca se almacena.
    @Test
    void error_del_scanner_falla_cerrado_y_nunca_almacena() {
        when(malwareScanPort.scan(any())).thenThrow(new MalwareScanPort.MalwareScanException("ClamAV no disponible"));

        assertThrows(InternalApplicationException.class, () -> useCase.execute(dominioValido("soporte.pdf")));

        verify(fileStoragePort, never()).store(any());
    }

    @Test
    void el_escaneo_ocurre_despues_de_la_validacion_de_contenido_y_antes_del_almacenamiento() {
        when(malwareScanPort.scan(any())).thenReturn(MalwareScanPort.ScanResult.clean());

        useCase.execute(dominioValido("soporte.pdf"));

        final InOrder order = inOrder(malwareScanPort, fileStoragePort);
        order.verify(malwareScanPort).scan(any());
        order.verify(fileStoragePort).store(any());
    }

    @Test
    void contenido_invalido_nunca_llega_al_escaneo_de_malware() {
        assertThrows(ValidationException.class, () -> useCase.execute(dominioValido("script.exe")));

        verify(malwareScanPort, never()).scan(any());
        verify(fileStoragePort, never()).store(any());
    }

    @Test
    void metadata_almacenada_incluye_checksum_owner_y_content_type_verificado() {
        when(malwareScanPort.scan(any())).thenReturn(MalwareScanPort.ScanResult.clean());
        final UUID owner = UUID.randomUUID();

        useCase.execute(new SubirArchivoDomain(
                owner, "soporte.pdf", "application/pdf", withMagic(PDF_MAGIC, "contenido")));

        final ArgumentCaptor<FileStoragePort.StoreObjectCommand> captor =
                ArgumentCaptor.forClass(FileStoragePort.StoreObjectCommand.class);
        verify(fileStoragePort).store(captor.capture());
        final FileStoragePort.StoredObjectMetadata metadata = captor.getValue().metadata();
        assertEquals(owner.toString(), metadata.ownerSubject());
        assertEquals("application/pdf", metadata.contentType());
        assertNotNull(metadata.checksumSha256());
    }


    // LB-004D BE-01: tras almacenar, el borrador DRAFT se registra en SQL con el propietario del JWT y la metadata verificada.
    @Test
    void registra_el_borrador_en_sql_con_propietario_metadata_y_checksum_despues_de_almacenar() {
        when(malwareScanPort.scan(any())).thenReturn(MalwareScanPort.ScanResult.clean());
        final UUID owner = UUID.randomUUID();
        final byte[] content = withMagic(PDF_MAGIC, "contenido registrado");

        final SubirArchivoResultadoEntity resultado = useCase.execute(
                new SubirArchivoDomain(owner, "soporte medico.pdf", "application/pdf", content));

        final ArgumentCaptor<SoporteArchivoMetadataPort.RegistrarSoporteBorrador> captor =
                ArgumentCaptor.forClass(SoporteArchivoMetadataPort.RegistrarSoporteBorrador.class);
        verify(metadataPort).registrarBorrador(captor.capture());
        final SoporteArchivoMetadataPort.RegistrarSoporteBorrador comando = captor.getValue();
        assertEquals(resultado.fileId(), comando.fileId());
        assertEquals(owner, comando.usuarioPropietario());
        assertEquals("soporte_medico.pdf", comando.nombreOriginal());
        assertEquals("application/pdf", comando.tipoContenido());
        assertEquals(content.length, comando.tamanioBytes());
        assertEquals(co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ChecksumCalculator.sha256(content),
                comando.checksumSha256());
        final InOrder order = inOrder(fileStoragePort, metadataPort);
        order.verify(fileStoragePort).store(any());
        order.verify(metadataPort).registrarBorrador(any());
    }

    // BE-02: rechazo definitivo de SQL => compensacion (delete) y error, sin devolver un fileId huerfano.
    @Test
    void rechazo_definitivo_de_sql_elimina_el_objeto_recien_almacenado() {
        when(malwareScanPort.scan(any())).thenReturn(MalwareScanPort.ScanResult.clean());
        doThrow(new SoporteArchivoMetadataPort.SoporteMetadataRechazadaException("rechazo", new RuntimeException()))
                .when(metadataPort).registrarBorrador(any());

        assertThrows(InternalApplicationException.class, () -> useCase.execute(dominioValido("soporte.pdf")));

        final ArgumentCaptor<FileStoragePort.StoreObjectCommand> stored =
                ArgumentCaptor.forClass(FileStoragePort.StoreObjectCommand.class);
        verify(fileStoragePort).store(stored.capture());
        verify(fileStoragePort).delete(stored.getValue().fileId());
        verify(metadataPort, times(1)).registrarBorrador(any());
    }

    @Test
    void si_la_compensacion_falla_el_error_original_se_conserva() {
        when(malwareScanPort.scan(any())).thenReturn(MalwareScanPort.ScanResult.clean());
        doThrow(new SoporteArchivoMetadataPort.SoporteMetadataRechazadaException("rechazo", new RuntimeException()))
                .when(metadataPort).registrarBorrador(any());
        doThrow(new FileStoragePort.StorageUnavailableException("minio down", new RuntimeException()))
                .when(fileStoragePort).delete(any());

        assertThrows(InternalApplicationException.class, () -> useCase.execute(dominioValido("soporte.pdf")));
    }

    // BE-03: resultado desconocido => un reintento idempotente (misma metadata) y SIN delete.
    @Test
    void resultado_desconocido_reintenta_una_vez_con_la_misma_metadata_y_no_borra_bytes() {
        when(malwareScanPort.scan(any())).thenReturn(MalwareScanPort.ScanResult.clean());
        doThrow(new SoporteArchivoMetadataPort.SoporteMetadataNoDisponibleException("timeout", new RuntimeException()))
                .doNothing().when(metadataPort).registrarBorrador(any());

        final SubirArchivoResultadoEntity resultado = useCase.execute(dominioValido("soporte.pdf"));

        assertNotNull(resultado.fileId());
        final ArgumentCaptor<SoporteArchivoMetadataPort.RegistrarSoporteBorrador> captor =
                ArgumentCaptor.forClass(SoporteArchivoMetadataPort.RegistrarSoporteBorrador.class);
        verify(metadataPort, times(2)).registrarBorrador(captor.capture());
        assertEquals(captor.getAllValues().get(0), captor.getAllValues().get(1));
        verify(fileStoragePort, never()).delete(any());
    }

    @Test
    void resultado_desconocido_persistente_falla_sin_borrar_bytes() {
        when(malwareScanPort.scan(any())).thenReturn(MalwareScanPort.ScanResult.clean());
        doThrow(new SoporteArchivoMetadataPort.SoporteMetadataNoDisponibleException("timeout", new RuntimeException()))
                .when(metadataPort).registrarBorrador(any());

        assertThrows(InternalApplicationException.class, () -> useCase.execute(dominioValido("soporte.pdf")));

        verify(metadataPort, times(2)).registrarBorrador(any());
        verify(fileStoragePort, never()).delete(any());
    }

    @Test
    void rechazo_definitivo_en_el_reintento_tambien_compensa() {
        when(malwareScanPort.scan(any())).thenReturn(MalwareScanPort.ScanResult.clean());
        doThrow(new SoporteArchivoMetadataPort.SoporteMetadataNoDisponibleException("timeout", new RuntimeException()))
                .doThrow(new SoporteArchivoMetadataPort.SoporteMetadataRechazadaException("rechazo", new RuntimeException()))
                .when(metadataPort).registrarBorrador(any());

        assertThrows(InternalApplicationException.class, () -> useCase.execute(dominioValido("soporte.pdf")));

        verify(fileStoragePort).delete(any());
    }

    @Test
    void archivo_rechazado_antes_de_almacenar_nunca_registra_metadata() {
        assertThrows(ValidationException.class, () -> useCase.execute(dominioValido("script.exe")));
        when(malwareScanPort.scan(any())).thenReturn(MalwareScanPort.ScanResult.infected("Eicar-Test-Signature"));
        assertThrows(ValidationException.class, () -> useCase.execute(dominioValido("soporte.pdf")));

        verify(metadataPort, never()).registrarBorrador(any());
    }

    // BE-10 / hallazgo H-05: el nombre persistido es NVARCHAR(250); se rechaza (no se trunca en silencio).
    @Test
    void nombre_de_mas_de_250_caracteres_es_rechazado_antes_de_escanear_o_almacenar() {
        final String nombreLargo = "a".repeat(247) + ".pdf";
        assertEquals(251, nombreLargo.length());

        assertThrows(ValidationException.class, () -> useCase.execute(dominioValido(nombreLargo)));

        verify(malwareScanPort, never()).scan(any());
        verify(fileStoragePort, never()).store(any());
        verify(metadataPort, never()).registrarBorrador(any());
    }

    @Test
    void nombre_de_exactamente_250_caracteres_es_aceptado() {
        when(malwareScanPort.scan(any())).thenReturn(MalwareScanPort.ScanResult.clean());
        final String nombre = "a".repeat(246) + ".pdf";
        assertEquals(250, nombre.length());

        assertEquals(nombre, useCase.execute(dominioValido(nombre)).nombre());
    }

    private SubirArchivoDomain dominioValido(final String filename) {
        return new SubirArchivoDomain(
                UUID.randomUUID(), filename, declaredContentTypeFor(filename), withMagic(PDF_MAGIC, "contenido de prueba"));
    }

    private static String declaredContentTypeFor(final String filename) {
        return filename != null && filename.endsWith(".exe") ? "application/octet-stream" : "application/pdf";
    }

    private static byte[] withMagic(final byte[] magic, final String payload) {
        final byte[] payloadBytes = payload.getBytes();
        final byte[] content = new byte[magic.length + payloadBytes.length];
        System.arraycopy(magic, 0, content, 0, magic.length);
        System.arraycopy(payloadBytes, 0, content, magic.length, payloadBytes.length);
        return content;
    }
}


