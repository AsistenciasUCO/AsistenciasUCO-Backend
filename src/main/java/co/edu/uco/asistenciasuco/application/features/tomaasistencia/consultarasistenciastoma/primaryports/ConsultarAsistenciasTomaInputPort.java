package co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarasistenciastoma.primaryports;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.AsistenciaTomaFila;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/** CULM-001 B.2 (contrato 3.2.5): matriculas activas del grupo con su detalle en la toma, paginadas en SQL. */
public interface ConsultarAsistenciasTomaInputPort
        extends InteractorWithReturn<ConsultarAsistenciasTomaInputPort.Consulta, PaginaResultado<AsistenciaTomaFila>> {

    record Consulta(UUID tomaId, Integer pagina, Integer tamano, UUID usuarioEjecutor) {
    }
}
