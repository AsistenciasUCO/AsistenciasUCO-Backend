package co.edu.uco.asistenciasuco.application.features.consultastransversales.usecase;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.ConsultasTransversalesQueryPort.AsignaturaProjection;
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
}
