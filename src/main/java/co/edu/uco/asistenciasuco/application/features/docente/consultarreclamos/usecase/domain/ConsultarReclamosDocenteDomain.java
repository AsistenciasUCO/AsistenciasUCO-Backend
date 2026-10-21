package co.edu.uco.asistenciasuco.application.features.docente.consultarreclamos.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Criterios normalizados: estado opcional (P/A/R o PENDIENTE/APROBADA/RECHAZADA), pagina minima 1 y tamano
 * entre 1 y 100 (valores fuera de rango se acotan; un estado desconocido es un error de validacion).
 */
public record ConsultarReclamosDocenteDomain(UUID actorUsuarioId, String codigoEstado, int pagina, int tamano) {

    public static final int TAMANO_PREDETERMINADO = 10;
    public static final int TAMANO_MAXIMO = 100;

    private static final Map<String, String> CODIGO_POR_ESTADO = Map.of(
            "P", "P", "PENDIENTE", "P",
            "A", "A", "APROBADA", "A",
            "R", "R", "RECHAZADA", "R"
    );

    public static ConsultarReclamosDocenteDomain of(
            final UUID actorUsuarioId,
            final String estado,
            final Integer pagina,
            final Integer tamano
    ) {
        if (actorUsuarioId == null) {
            throw new ValidationException("El usuario autenticado es obligatorio.");
        }
        final String normalizado = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase(Locale.ROOT);
        final String codigo = normalizado == null ? null : CODIGO_POR_ESTADO.get(normalizado);
        if (normalizado != null && codigo == null) {
            throw new ValidationException("El estado de la solicitud no es valido.");
        }
        final int paginaSegura = pagina == null || pagina < 1 ? 1 : pagina;
        final int tamanoSeguro = tamano == null || tamano < 1 ? TAMANO_PREDETERMINADO : Math.min(tamano, TAMANO_MAXIMO);
        return new ConsultarReclamosDocenteDomain(actorUsuarioId, codigo, paginaSegura, tamanoSeguro);
    }
}
