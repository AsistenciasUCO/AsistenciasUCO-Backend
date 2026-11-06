package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO;

import java.util.Map;

/**
 * HU159: parametros comunes de la correccion de documento ({@code @idTipoIdIdentificacion}, {@code @numeroIdentificacion}
 * y {@code @motivo}) para los SP que la aceptan. Nulos = el documento no cambia.
 */
final class CorreccionDocumentoParametros {

    private CorreccionDocumentoParametros() {
    }

    static void agregar(final Map<String, Object> parametros, final CorreccionDocumentoDTO documento) {
        final CorreccionDocumentoDTO valor = documento == null ? CorreccionDocumentoDTO.ninguna() : documento;
        parametros.put("idTipoIdIdentificacion", valor.idTipoIdentificacion());
        parametros.put("numeroIdentificacion", valor.numeroIdentificacion());
        parametros.put("motivo", valor.motivo());
    }
}
