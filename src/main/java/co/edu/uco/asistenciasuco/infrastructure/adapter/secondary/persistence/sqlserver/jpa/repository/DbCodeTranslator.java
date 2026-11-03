package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import jakarta.persistence.PersistenceException;

/**
 * Traduce los codigos DBCODE de los SP de consulta (indicadores, verificacion de correo y consultas transversales
 * HU112/HU128/HU143/HU157) a errores de negocio con su HTTP: la DB decide el caso,
 * el backend solo lo clasifica. Los codigos no listados siguen siendo fallos tecnicos (500).
 */
final class DbCodeTranslator {

    private static final String MARCA = "DBCODE=";

    private DbCodeTranslator() {
    }

    static RuntimeException traducir(final PersistenceException exception) {
        return traducir(exception, "estudiante");
    }

    /** {@code entidadNoEncontrada} nombra lo que significa GEN_001 en la consulta que falla (estudiante, programa...). */
    static RuntimeException traducir(final PersistenceException exception, final String entidadNoEncontrada) {
        final String codigo = codigoDe(exception);
        if (codigo == null) {
            return exception;
        }
        return switch (codigo) {
            case "KPI_001" -> new ForbiddenException("KPI_001", "No tiene un programa academico a su cargo para consultar estos indicadores.");
            case "KPI_003" -> new ForbiddenException("KPI_003", "No tiene una facultad a su cargo para consultar estos indicadores.");
            case "KPI_004" -> new ForbiddenException("KPI_004", "No tiene una institucion a su cargo para consultar estos indicadores.");
            case "GEN_001" -> new ResourceNotFoundException("El " + entidadNoEncontrada + " especificado no existe en el sistema.");
            case "PER_001" -> new ResourceNotFoundException("El periodo academico especificado no existe en el sistema.");
            // IDENT-001: reenvio del enlace de verificacion. VCE_004 (fuera de ambito) es indistinguible de inexistente.
            case "VCE_003" -> new ConflictException("VCE_003", "Debe esperar unos minutos antes de volver a enviar el enlace de verificacion.");
            case "VCE_004" -> new ResourceNotFoundException("VCE_004", "El estudiante no pertenece a un grupo a su cargo.");
            case "VCE_005" -> new ConflictException("VCE_005", "El correo de este estudiante ya fue verificado.");
            case "PAG_001" -> new ValidationException("Los parametros de paginacion no son validos.");
            case "GEN_002", "VAL_001" -> new ValidationException("Los datos de la consulta no son validos.");
            // HU112/HU128/HU143/HU157: el SP de lectura exige el perfil del ejecutor.
            case "SEC_001" -> new ForbiddenException("SEC_001", "No tiene el perfil requerido para esta consulta.");
            default -> exception;
        };
    }

    private static String codigoDe(final Throwable exception) {
        for (Throwable actual = exception; actual != null; actual = actual.getCause()) {
            final String mensaje = actual.getMessage();
            if (mensaje != null) {
                final int inicio = mensaje.indexOf(MARCA);
                final int fin = inicio < 0 ? -1 : mensaje.indexOf('|', inicio);
                if (fin > inicio) {
                    return mensaje.substring(inicio + MARCA.length(), fin);
                }
            }
        }
        return null;
    }
}
