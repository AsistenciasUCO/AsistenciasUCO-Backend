package co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarfaltantestoma.primaryports;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.FaltanteTomaFila;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/** CULM-001 B.2 (contrato 3.2.5): faltantes de la toma (activos sin registro presente), paginadas en SQL. */
public interface ConsultarFaltantesTomaInputPort
        extends InteractorWithReturn<ConsultarFaltantesTomaInputPort.Consulta, PaginaResultado<FaltanteTomaFila>> {

    record Consulta(UUID tomaId, Integer pagina, Integer tamano, UUID usuarioEjecutor) {
    }
}
