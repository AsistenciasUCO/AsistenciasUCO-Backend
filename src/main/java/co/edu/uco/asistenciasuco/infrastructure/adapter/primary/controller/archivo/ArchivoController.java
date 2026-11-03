package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.archivo;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.DescargarArchivoInputPort;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.dto.DescargarArchivoDTO;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.dto.DescargarArchivoResultado;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.SubirArchivoInputPort;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.dto.SubirArchivoDTO;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.dto.SubirArchivoResultado;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.contract.AuthenticatedUserResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador primario REST para subida/descarga de soportes de revision (HU014).
 *
 * <p>Depende exclusivamente de InputPorts de Application: no conoce MinIO, ClamAV, filesystem ni
 * ningun detalle de Infrastructure. Ver
 * docs/work-items/LB-004-stateless-serverless-readiness/MINIO_STORAGE_CONTRACT.md.</p>
 */
@RestController
@RequestMapping("/api/v1/archivos")
public final class ArchivoController {

    private final SubirArchivoInputPort subirArchivoInputPort;
    private final DescargarArchivoInputPort descargarArchivoInputPort;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ArchivoController(
            final SubirArchivoInputPort subirArchivoInputPort,
            final DescargarArchivoInputPort descargarArchivoInputPort,
            final AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.subirArchivoInputPort = Objects.requireNonNull(subirArchivoInputPort, "SubirArchivoInputPort es obligatorio.");
        this.descargarArchivoInputPort = Objects.requireNonNull(descargarArchivoInputPort, "DescargarArchivoInputPort es obligatorio.");
        this.authenticatedUserResolver = Objects.requireNonNull(authenticatedUserResolver, "AuthenticatedUserResolver es obligatorio.");
    }

    @PostMapping(value = "/subir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> subirArchivo(
            @RequestParam("archivo") final MultipartFile archivo
    ) {
        final UUID ownerSubject = authenticatedUserResolver.requireAuthenticatedUserId();
        final byte[] content = readBytes(archivo);
        final String originalFilename = archivo == null ? null : archivo.getOriginalFilename();
        final String declaredContentType = archivo == null ? null : archivo.getContentType();

        final SubirArchivoResultado resultado = subirArchivoInputPort.execute(
                new SubirArchivoDTO(ownerSubject, originalFilename, declaredContentType, content));

        final Map<String, Object> data = new HashMap<>();
        data.put("fileId", resultado.fileId());
        data.put("nombre", resultado.nombre());
        data.put("url", resultado.url());
        data.put("tamanio", resultado.tamanio());

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, data));
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<byte[]> descargarArchivo(@PathVariable final String fileId) {
        final UUID requesterSubject = authenticatedUserResolver.requireAuthenticatedUserId();
        final UUID parsedFileId = parseFileId(fileId);

        final DescargarArchivoResultado resultado = descargarArchivoInputPort.execute(
                new DescargarArchivoDTO(parsedFileId, requesterSubject));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(resultado.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + sanitizeForHeader(resultado.filename()) + "\"")
                .body(resultado.content());
    }

    private byte[] readBytes(final MultipartFile archivo) {
        if (archivo == null) {
            return new byte[0];
        }
        try {
            return archivo.getBytes();
        } catch (final IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al leer el archivo adjunto.");
        }
    }

    private UUID parseFileId(final String fileId) {
        try {
            return UUID.fromString(fileId);
        } catch (final IllegalArgumentException exception) {
            throw new ResourceNotFoundException("El archivo solicitado no existe.");
        }
    }

    private String sanitizeForHeader(final String filename) {
        return filename == null ? "archivo" : filename.replaceAll("[\"\\r\\n]", "_");
    }
}
