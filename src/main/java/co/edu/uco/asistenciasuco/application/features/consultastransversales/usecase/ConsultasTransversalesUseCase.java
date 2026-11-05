package co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.AsignaturaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.CategoriaJustificacionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.ComponenteFormacionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.ConsolidadoGrupoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.EstadoSistemaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.EventoAuditoriaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroAuditoria;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroConsolidado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.DocenteFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.EstudianteInstitucionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.FiltroConsulta;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.HistorialEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.ProgramaCreditosProjection;

import java.util.List;
import java.util.UUID;

public interface ConsultasTransversalesUseCase {

    Pagina<ProgramaCreditosProjection> consultarProgramasCreditos(UUID usuario, String busqueda, Boolean estaActivo,
                                                                  Integer pagina, Integer tamano);

    Pagina<DocenteFacultadProjection> consultarDocentes(UUID usuario, FiltroConsulta filtro, Integer pagina,
                                                        Integer tamano);

    Pagina<AsignaturaProjection> buscarAsignaturas(UUID usuario, FiltroConsulta filtro, Integer pagina, Integer tamano);

    Pagina<EstudianteInstitucionProjection> buscarEstudiantes(UUID usuario, FiltroConsulta filtro, Integer pagina,
                                                              Integer tamano);

    List<HistorialEstudianteProjection> consultarHistorial(UUID usuario, UUID idEstudiante);

    /** HU061: el ambito (programa, facultad o institucion) lo resuelve el provider segun el perfil. */
    Pagina<ConsolidadoGrupoProjection> consultarConsolidadoAsistencia(UUID usuario, FiltroConsolidado filtro,
                                                                      Integer pagina, Integer tamano);

    /** HU172: sin rango se consultan los ultimos 7 dias; el rango maximo es de 90 dias. */
    Pagina<EventoAuditoriaProjection> consultarAuditoria(UUID usuario, FiltroAuditoria filtro, Integer pagina,
                                                         Integer tamano);

    List<ComponenteFormacionProjection> consultarComponentes(UUID usuario);

    /** HU155: estados del sistema (solo consulta; el rol ADMINISTRADOR lo exige SecurityConfig). */
    List<EstadoSistemaProjection> consultarEstadosSistema();

    /** HU005/HU016: categorias de justificacion (solo consulta; cualquier usuario autenticado). */
    List<CategoriaJustificacionProjection> consultarCategoriasJustificacion();
}
