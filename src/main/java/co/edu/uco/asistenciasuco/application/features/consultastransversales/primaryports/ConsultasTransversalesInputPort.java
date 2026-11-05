package co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports;

import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.AsignaturaInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.CategoriaJustificacionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.ComponenteFormacionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.ConsolidadoGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.DocenteFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.EstadoSistemaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.EventoAuditoriaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FiltroAuditoriaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FiltroConsolidadoDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.EstudianteInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FichaUnificadaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.ProgramaCreditosDTO;

import java.util.List;
import java.util.UUID;

/**
 * HU112 (tabla unificada de programas y creditos), HU128 (docentes de la facultad por programa), HU143 (busqueda de
 * asignaturas), HU157 (buscador de estudiantes del administrador y ficha unificada con historial), HU061 (consolidado
 * de asistencia por grupo), HU172 (rastro de auditoria) y HU147 (componentes de formacion).
 */
public interface ConsultasTransversalesInputPort {

    PaginaDTO<ProgramaCreditosDTO> consultarProgramasCreditos(UUID usuario, String busqueda, Boolean estaActivo,
                                                              Integer pagina, Integer tamano);

    PaginaDTO<DocenteFacultadDTO> consultarDocentes(UUID usuario, String busqueda, UUID idPrograma, Boolean estaActivo,
                                                    Integer pagina, Integer tamano);

    PaginaDTO<AsignaturaInstitucionDTO> buscarAsignaturas(UUID usuario, String busqueda, UUID idFacultad,
                                                          Boolean estaActiva, Integer pagina, Integer tamano);

    PaginaDTO<EstudianteInstitucionDTO> buscarEstudiantes(UUID usuario, String busqueda, UUID idFacultad,
                                                          Boolean estaActivo, Integer pagina, Integer tamano);

    FichaUnificadaDTO consultarHistorial(UUID usuario, UUID idEstudiante);

    PaginaDTO<ConsolidadoGrupoDTO> consultarConsolidadoAsistencia(UUID usuario, FiltroConsolidadoDTO filtro,
                                                                  Integer pagina, Integer tamano);

    PaginaDTO<EventoAuditoriaDTO> consultarAuditoria(UUID usuario, FiltroAuditoriaDTO filtro, Integer pagina,
                                                     Integer tamano);

    List<ComponenteFormacionDTO> consultarComponentes(UUID usuario);

    List<EstadoSistemaDTO> consultarEstadosSistema();

    List<CategoriaJustificacionDTO> consultarCategoriasJustificacion();
}
