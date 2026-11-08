package co.edu.uco.asistenciasuco.application.features.admin.consultarparametros.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * CULM-001 D.3 (HU168, F-ADM-012): parametros de negocio legibles, agrupados por {@code categoriaVisual}, filtrables y
 * paginados. {@code soloEditables} nulo equivale a {@code true}; los tecnicos se listan de solo lectura con {@code false}.
 */
public interface ConsultarParametrosInputPort
        extends InteractorWithReturn<ConsultarParametrosInputPort.Comando, ConsultarParametrosInputPort.Resultado> {

    int PAGINA_POR_DEFECTO = 1;
    int TAMANO_POR_DEFECTO = 20;

    record Comando(Integer pagina, Integer tamano, String grupo, String texto, Boolean soloEditables, UUID usuarioEjecutor) {
    }

    record Resultado(List<Parametro> elementos, int numeroPagina, int tamanoPagina, long totalElementos) {
        public Resultado {
            elementos = elementos == null ? List.of() : List.copyOf(elementos);
        }
    }

    record Parametro(UUID id, String grupo, String clave, String valor, String valorDefecto, String tipoDato, boolean esEditable,
                     BigDecimal valorMinimo, BigDecimal valorMaximo, List<String> valoresPermitidos, String etiqueta,
                     String descripcion, String unidad, String categoriaVisual, int orden, Instant fechaModificacion,
                     Instant fechaUltimoCambioUtc, String nombreUltimoActor) {
    }
}
