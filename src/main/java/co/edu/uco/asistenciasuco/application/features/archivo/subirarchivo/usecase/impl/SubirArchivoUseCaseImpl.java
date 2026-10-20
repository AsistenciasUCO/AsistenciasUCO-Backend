package co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ChecksumCalculator;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.CompressionDecision;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.CompressionPolicy;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ContentSecurityValidator;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ValidatedContent;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.SubirArchivoUseCase;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.domain.SubirArchivoDomain;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.entity.SubirArchivoResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.malwarescan.MalwareScanPort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Orquesta: validacion de contenido -> analisis de malware (fail-closed) -> decision de
 * compresion -> checksum -> almacenamiento -> registro durable del borrador (DRAFT) en SQL. Ver
 * docs/work-items/LB-004-stateless-serverless-readiness/CONTENT_SECURITY.md y
 * MINIO_STORAGE_CONTRACT.md para el contrato completo.
 *
 * <p>MinIO no participa en la transaccion SQL. Si SQL rechaza el registro de forma definitiva (no se
 * escribio nada) el objeto recien almacenado se elimina como compensacion best-effort. Si el
 * resultado es desconocido (transporte/timeout) se reintenta una vez con la misma metadata
 * (idempotente) y, si persiste, NO se borran los bytes: puede existir la fila y la reconciliacion
 * posterior decide.</p>
 */
public final class SubirArchivoUseCaseImpl implements SubirArchivoUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubirArchivoUseCaseImpl.class);
    private static final String ARCHIVO_URL_PREFIX = "/api/v1/archivos/";

    private final FileStoragePort fileStoragePort;
    private final MalwareScanPort malwareScanPort;
    private final SoporteArchivoMetadataPort soporteArchivoMetadataPort;

    public SubirArchivoUseCaseImpl(
            final FileStoragePort fileStoragePort,
            final MalwareScanPort malwareScanPort,
            final SoporteArchivoMetadataPort soporteArchivoMetadataPort
    ) {
        this.fileStoragePort = Objects.requireNonNull(fileStoragePort, "El puerto de salida FileStoragePort es obligatorio.");
        this.malwareScanPort = Objects.requireNonNull(malwareScanPort, "El puerto de salida MalwareScanPort es obligatorio.");
        this.soporteArchivoMetadataPort = Objects.requireNonNull(
                soporteArchivoMetadataPort, "El puerto de salida SoporteArchivoMetadataPort es obligatorio.");
    }

    @Override
    public SubirArchivoResultadoEntity execute(final SubirArchivoDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para subir un archivo es obligatorio.");
        }
        if (domain.ownerSubject() == null) {
            throw new CrosscuttingException("El propietario autenticado es obligatorio para subir un archivo.");
        }

        final ValidatedContent validated = ContentSecurityValidator.validate(
                domain.originalFilename(), domain.declaredContentType(), domain.content());

        scanForMalwareFailClosed(domain.content());

        final CompressionDecision compression = CompressionPolicy.evaluate(domain.content());
        final String checksum = ChecksumCalculator.sha256(domain.content());

        final UUID fileId = UUID.randomUUID();
        final FileStoragePort.StoredObjectMetadata metadata = new FileStoragePort.StoredObjectMetadata(
                domain.ownerSubject().toString(),
                validated.sanitizedFilename(),
                validated.verifiedContentType(),
                domain.content().length,
                compression.storedContent().length,
                compression.compressed(),
                compression.algorithm(),
                checksum,
                Instant.now()
        );

        fileStoragePort.store(new FileStoragePort.StoreObjectCommand(fileId.toString(), compression.storedContent(), metadata));

        registrarBorradorDurable(new SoporteArchivoMetadataPort.RegistrarSoporteBorrador(
                fileId,
                domain.ownerSubject(),
                validated.sanitizedFilename(),
                validated.verifiedContentType(),
                domain.content().length,
                checksum
        ));

        return new SubirArchivoResultadoEntity(fileId, validated.sanitizedFilename(), ARCHIVO_URL_PREFIX + fileId, domain.content().length);
    }

    private void registrarBorradorDurable(final SoporteArchivoMetadataPort.RegistrarSoporteBorrador comando) {
        try {
            soporteArchivoMetadataPort.registrarBorrador(comando);
        } catch (final SoporteArchivoMetadataPort.SoporteMetadataRechazadaException rechazo) {
            // Rechazo definitivo: SQL no escribio nada, el objeto es un huerfano seguro de compensar.
            compensarObjeto(comando.fileId());
            throw new InternalApplicationException("No fue posible registrar el soporte adjunto.", rechazo);
        } catch (final SoporteArchivoMetadataPort.SoporteMetadataNoDisponibleException desconocido) {
            reintentarRegistroUnaVez(comando, desconocido);
        }
    }

    private void reintentarRegistroUnaVez(
            final SoporteArchivoMetadataPort.RegistrarSoporteBorrador comando,
            final SoporteArchivoMetadataPort.SoporteMetadataNoDisponibleException primerFallo
    ) {
        try {
            // Mismo fileId, metadata y propietario: el registro es idempotente en SQL.
            soporteArchivoMetadataPort.registrarBorrador(comando);
        } catch (final SoporteArchivoMetadataPort.SoporteMetadataRechazadaException rechazo) {
            compensarObjeto(comando.fileId());
            throw new InternalApplicationException("No fue posible registrar el soporte adjunto.", rechazo);
        } catch (final SoporteArchivoMetadataPort.SoporteMetadataNoDisponibleException persistente) {
            // Resultado desconocido: NO se borran los bytes (puede existir la fila DRAFT).
            if (persistente != primerFallo) {
                persistente.addSuppressed(primerFallo);
            }
            throw new InternalApplicationException("No fue posible confirmar el registro del soporte adjunto.", persistente);
        }
    }

    private void compensarObjeto(final UUID fileId) {
        try {
            fileStoragePort.delete(fileId.toString());
        } catch (final RuntimeException exception) {
            // Best-effort: si falla queda un huerfano tecnico sin fila, atendido por la reconciliacion.
            LOGGER.warn("Compensacion de objeto de soporte no completada. fileId={}", fileId);
        }
    }

    private void scanForMalwareFailClosed(final byte[] content) {
        final MalwareScanPort.ScanResult scanResult;
        try {
            scanResult = malwareScanPort.scan(content);
        } catch (final MalwareScanPort.MalwareScanException exception) {
            throw new InternalApplicationException(
                    "No fue posible completar el control de seguridad del archivo.", exception);
        }
        if (scanResult.isInfected()) {
            throw new ValidationException(
                    "El archivo fue rechazado: se detecto una firma de malware conocida.");
        }
    }
}
