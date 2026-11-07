package co.edu.uco.asistenciasuco.application.features.tomaasistencia.registrarasistenciastoma.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.List;
import java.util.UUID;

/**
 * CULM-001 B.2 (contrato 3.2.5): lote atomico de asistencia de una toma ({@code AN|SJC|EX}, 1..200 registros sin
 * duplicados). Una toma cerrada admite correccion mientras su ventana siga abierta ([SUPUESTO-B-08]).
 */
public interface RegistrarAsistenciasTomaInputPort
        extends InteractorWithReturn<RegistrarAsistenciasTomaInputPort.Comando, RegistrarAsistenciasTomaInputPort.Resultado> {

    int MAXIMO_REGISTROS = 200;

    record Registro(UUID estudianteId, String estado) {
    }

    record Comando(UUID tomaId, List<Registro> registros, UUID usuarioEjecutor) {
    }

    record Resultado(UUID idToma, UUID idSesion, int totalRegistros) {
    }
}
