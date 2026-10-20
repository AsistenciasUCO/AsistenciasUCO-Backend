package co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.ChecksumCalculator;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.DescargarArchivoUseCase;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.domain.DescargarArchivoDomain;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.entity.DescargarArchivoResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.archivo.shared.contentsecurity.CompressionPolicy;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.SoporteArchivoMetadataPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

import java.util.Objects;

/**
 * Descarga autorizada de un soporte. SQL es la autoridad: el acceso se resuelve ANTES de leer un solo
 * byte del storage, con {@link SoporteArchivoMetadataPort#consultarAutorizado}. Solo el estudiante
 * propietario y el docente titular de la sesion de la solicitud (soporte ATTACHED) obtienen una fila;
 * cualquier otro actor, incluido administrador, decano o coordinador, recibe 404 (nunca 403) para no
 * revelar existencia, y el storage ni siquiera se consulta.
 *
 * <p>Tras leer los bytes se infla de forma acotada si estaban comprimidos y se verifica que el SHA-256 y
 * la longitud coincidan con los valores de SQL (autoridad). El {@code owner-subject} del objeto es solo
 * un control tecnico: ya no concede ni deniega acceso.</p>
 */
public final class DescargarArchivoUseCaseImpl implements DescargarArchivoUseCase {

    private static final String NOT_FOUND = "El archivo solicitado no existe.";

    private final FileStoragePort fileStoragePort;
    private final SoporteArchivoMetadataPort soporteArchivoMetadataPort;

    public DescargarArchivoUseCaseImpl(
            final FileStoragePort fileStoragePort,
            final SoporteArchivoMetadataPort soporteArchivoMetadataPort
    ) {
        this.fileStoragePort = Objects.requireNonNull(fileStoragePort, "El puerto de salida FileStoragePort es obligatorio.");
        this.soporteArchivoMetadataPort = Objects.requireNonNull(
                soporteArchivoMetadataPort, "El puerto de salida SoporteArchivoMetadataPort es obligatorio.");
    }

    @Override
    public DescargarArchivoResultadoEntity execute(final DescargarArchivoDomain domain) {
        if (ObjectHelper.isNull(domain)) {
            throw new CrosscuttingException("El dominio para descargar un archivo es obligatorio.");
        }
        if (domain.fileId() == null || domain.requesterSubject() == null) {
            throw new ResourceNotFoundException(NOT_FOUND);
        }

        // DENY_BY_DEFAULT: sin fila autorizada en SQL no hay lectura del storage.
        final SoporteArchivoMetadataPort.SoporteArchivoAutorizado autorizado = consultarAutorizacion(domain);

        final FileStoragePort.StoredObject stored = leerObjeto(domain);

        final byte[] originalContent;
        try {
            originalContent = stored.metadata() != null && stored.metadata().compressed()
                    ? CompressionPolicy.decompress(stored.content(), stored.metadata().compressionAlgorithm())
                    : stored.content();
        } catch (final RuntimeException exception) {
            throw new InternalApplicationException(
                    "La verificacion de integridad del archivo almacenado no fue satisfactoria.", exception);
        }

        if (originalContent.length != autorizado.tamanioBytes()
                || !ChecksumCalculator.sha256(originalContent).equals(autorizado.checksumSha256())) {
            throw new InternalApplicationException(
                    "La verificacion de integridad del archivo almacenado no fue satisfactoria.");
        }

        return new DescargarArchivoResultadoEntity(
                originalContent,
                autorizado.tipoContenido(),
                autorizado.nombreOriginal(),
                originalContent.length
        );
    }

    private SoporteArchivoMetadataPort.SoporteArchivoAutorizado consultarAutorizacion(final DescargarArchivoDomain domain) {
        try {
            return soporteArchivoMetadataPort.consultarAutorizado(domain.fileId(), domain.requesterSubject())
                    .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
        } catch (final SoporteArchivoMetadataPort.SoporteMetadataNoDisponibleException exception) {
            throw new InternalApplicationException("No fue posible verificar la autorizacion del archivo.", exception);
        }
    }

    private FileStoragePort.StoredObject leerObjeto(final DescargarArchivoDomain domain) {
        try {
            return fileStoragePort.read(domain.fileId().toString());
        } catch (final FileStoragePort.ObjectNotFoundException exception) {
            // La fila SQL autoriza el acceso pero el objeto no existe: incidente de integridad, no un 404 del usuario.
            throw new InternalApplicationException(
                    "La verificacion de integridad del archivo almacenado no fue satisfactoria.", exception);
        }
    }
}
