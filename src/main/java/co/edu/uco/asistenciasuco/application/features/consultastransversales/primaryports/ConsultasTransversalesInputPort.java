package co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports;

import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.AsignaturaInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.DocenteFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.EstudianteInstitucionDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.FichaUnificadaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.application.features.consultastransversales.primaryports.dto.ConsultasTransversalesDTO.ProgramaCreditosDTO;

import java.util.UUID;

/**
 * HU112 (tabla unificada de programas y creditos), HU128 (docentes de la facultad por programa), HU143 (busqueda de
 * asignaturas) y HU157 (buscador de estudiantes del administrador y ficha unificada con historial).
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
}
