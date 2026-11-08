package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CULM-001 D.3 (HU168): parametros legibles, edicion con lista blanca y historial. La DB valida RBAC ADMINISTRADOR,
 * lista blanca, tipo, rango, reglas cruzadas, motivo y concurrencia ({@code PRM_*}). Las fechas son {@code DATETIME} en UTC.
 */
public interface ParametroAdminRepositoryPort {

    /** {@code usp_consultar_parametros_paginado}. */
    PaginaParametros consultar(UUID usuarioEjecutor, int pagina, int tamano, String grupo, String texto, boolean soloEditables);

    /** {@code usp_actualizar_parametro}. */
    ParametroActualizado actualizar(UUID idParametro, String valor, LocalDateTime fechaModificacionEsperadaUtc, String motivo,
                                    UUID usuarioEjecutor);

    /** {@code usp_consultar_cambios_parametro_paginado}. */
    PaginaCambios consultarCambios(UUID usuarioEjecutor, UUID idParametro, int pagina, int tamano);

    record ParametroAdmin(UUID id, String grupo, String clave, String valor, String valorDefecto, String tipoDato, boolean esEditable,
                          BigDecimal valorMinimo, BigDecimal valorMaximo, List<String> valoresPermitidos, String etiqueta,
                          String descripcion, String unidad, String categoriaVisual, int orden, LocalDateTime fechaModificacion,
                          LocalDateTime fechaUltimoCambioUtc, String nombreUltimoActor) {
        public ParametroAdmin {
            valoresPermitidos = valoresPermitidos == null ? List.of() : List.copyOf(valoresPermitidos);
        }
    }

    record PaginaParametros(List<ParametroAdmin> elementos, long totalRegistros) {
        public PaginaParametros {
            elementos = elementos == null ? List.of() : List.copyOf(elementos);
        }
    }

    record ParametroActualizado(UUID id, String grupo, String clave, String valorAnterior, String valorNuevo,
                                LocalDateTime fechaModificacion) {
    }

    record CambioParametro(UUID id, String valorAnterior, String valorNuevo, String motivo, UUID idUsuarioEjecutor,
                           String nombreEjecutor, UUID idTraza, LocalDateTime fechaCambioUtc) {
    }

    record PaginaCambios(List<CambioParametro> elementos, long totalRegistros) {
        public PaginaCambios {
            elementos = elementos == null ? List.of() : List.copyOf(elementos);
        }
    }
}
