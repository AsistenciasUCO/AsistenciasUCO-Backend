package co.edu.uco.asistenciasuco.application.features.asistencia.consultarresumenasistencia.primaryports;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.ResumenAsistenciaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.UUID;

/**
 * CULM-001 B.2 (F-DOC-004, contrato 3.2.8): sabana del docente titular con los indicadores por horas de cada estudiante
 * del grupo, calculados por la base de datos (el cliente ya no calcula porcentajes).
 */
public interface ConsultarResumenAsistenciaGrupoInputPort
        extends InteractorWithReturn<ConsultarResumenAsistenciaGrupoInputPort.Consulta, PaginaResultado<ResumenAsistenciaEstudianteDTO>> {

    record Consulta(UUID grupoId, Integer pagina, Integer tamano, UUID usuarioEjecutor) {
    }
}
