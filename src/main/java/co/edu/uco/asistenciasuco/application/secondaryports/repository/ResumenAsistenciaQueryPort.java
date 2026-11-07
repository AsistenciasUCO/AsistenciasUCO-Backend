package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.asistencia.common.dto.IndicadoresAsistenciaDTO;

import java.util.List;
import java.util.UUID;

/**
 * CULM-001 B.2 (F-DOC-004, F-EST-002): resumen de asistencia por horas ({@code uv_resumen_asistencia_estudiante_grupo})
 * con el ambito forzado por el SP ({@code usp_consultar_resumen_asistencia_grupo_docente_paginado} para el docente
 * titular y {@code usp_consultar_resumen_asistencia_estudiante} para el estudiante autenticado).
 */
public interface ResumenAsistenciaQueryPort {

    record FilaResumen(UUID idGrupo, UUID idEstudiante, String nombreCompleto, String codigoEstadoEstudiante,
                       IndicadoresAsistenciaDTO indicadores) {
    }

    PaginaResultado<FilaResumen> consultarResumenGrupoDocente(UUID grupoId, int numeroPagina, int tamanoPagina, UUID usuarioEjecutor);

    List<FilaResumen> consultarResumenEstudiante(UUID usuarioEjecutor);
}
