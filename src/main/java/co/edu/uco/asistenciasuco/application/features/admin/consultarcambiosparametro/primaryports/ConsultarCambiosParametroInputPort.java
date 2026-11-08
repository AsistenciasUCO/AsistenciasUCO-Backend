package co.edu.uco.asistenciasuco.application.features.admin.consultarcambiosparametro.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** CULM-001 D.3 (HU168, parte diferible): historial paginado de cambios de un parametro, del mas reciente al mas antiguo. */
public interface ConsultarCambiosParametroInputPort
        extends InteractorWithReturn<ConsultarCambiosParametroInputPort.Comando, ConsultarCambiosParametroInputPort.Resultado> {

    int PAGINA_POR_DEFECTO = 1;
    int TAMANO_POR_DEFECTO = 20;

    record Comando(UUID idParametro, Integer pagina, Integer tamano, UUID usuarioEjecutor) {
    }

    record Resultado(List<Cambio> elementos, int numeroPagina, int tamanoPagina, long totalElementos) {
        public Resultado {
            elementos = elementos == null ? List.of() : List.copyOf(elementos);
        }
    }

    record Cambio(UUID id, String valorAnterior, String valorNuevo, String motivo, UUID idUsuarioEjecutor, String nombreEjecutor,
                  UUID idTraza, Instant fechaCambioUtc) {
    }
}
